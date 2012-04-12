package org.basex.query.func;

import static org.basex.query.util.Err.*;
import static org.basex.query.QueryText.*;
import java.io.IOException;
import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import org.basex.io.serial.Serializer;
import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.item.AtomType;
import org.basex.query.item.Item;
import org.basex.query.item.QNm;
import org.basex.query.item.SeqType;
import org.basex.query.item.Value;
import org.basex.query.iter.*;
import org.basex.query.util.*;
import org.basex.query.var.*;
import org.basex.util.Atts;
import org.basex.util.InputInfo;
import org.basex.util.Token;
import org.basex.util.TokenBuilder;

/**
 * User-defined function.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public class UserFunc extends Single implements Scope {
  /** Function name. */
  public final QNm name;
  /** Arguments. */
  public final Var[] args;
  /** Declaration flag. */
  public final boolean declared;
  /** Return type. */
  public final SeqType ret;
  /** Annotations. */
  public final Ann ann;
  /** Updating flag. */
  public final boolean updating;

  /** Local variables in the scope of this function. */
  protected final VarScope scope;

  /** Cast flag. */
  private boolean cast;
  /** Compilation flag. */
  private boolean compiling;
  /** Compilation flag. */
  private boolean compiled;

  /**
   * Function constructor.
   * @param ii input info
   * @param n function name
   * @param v arguments
   * @param r return type
   * @param a annotations
   * @param scp scope
   */
  public UserFunc(final InputInfo ii, final QNm n, final Var[] v, final SeqType r,
      final Ann a, final VarScope scp) {
    this(ii, n, v, r, a, scp, true);
  }

  /**
   * Function constructor.
   * @param ii input info
   * @param n function name
   * @param v arguments
   * @param r return type
   * @param a annotations
   * @param scp scope
   * @param d declaration flag
   */
  public UserFunc(final InputInfo ii, final QNm n, final Var[] v, final SeqType r,
      final Ann a, final VarScope scp, final boolean d) {

    super(ii, null);
    name = n;
    args = v;
    ret = r;
    cast = r != null;
    ann = a == null ? new Ann() : a;
    declared = d;
    updating = ann.contains(Ann.UPDATING);
    scope = scp;
  }

  /**
   * Checks the function for updating behavior.
   * @throws QueryException query exception
   */
  final void checkUp() throws QueryException {
    final boolean u = expr.uses(Use.UPD);
    if(updating) {
      // updating function
      if(ret != null) UPFUNCTYPE.thrw(info);
      if(!u && !expr.isVacuous()) UPEXPECTF.thrw(info);
    } else if(u) {
      // uses updates, but is not declared as such
      UPNOT.thrw(info, description());
    }
  }

  @Override
  public Expr comp(final QueryContext ctx, final VarScope scp) throws QueryException {
    cmp(ctx, scp);
    return this;
  }

  /**
   * Compiles the expression.
   * @param ctx query context
   * @param outer enclosing scope
   * @throws QueryException query exception
   */
  void cmp(final QueryContext ctx, final VarScope outer) throws QueryException {
    if(compiled) return;

    if(compiling) throw CIRCVAR.thrw(info, description());
    compiling = true;

    // compile closure
    ArrayList<Entry<Var, Value>> propagate = null;
    final Iterator<Entry<Var, LocalVarRef>> cls = scope.closure().entrySet().iterator();
    while(cls.hasNext()) {
      final Entry<Var, LocalVarRef> e = cls.next();
      final Expr c = e.getValue().comp(ctx, outer);
      if(c.isValue()) {
        if(propagate == null) propagate = new ArrayList<Entry<Var, Value>>();
        propagate.add(new SimpleImmutableEntry<Var, Value>(e.getKey(), (Value) c));
        cls.remove();
      }
    }

    final Value[] sf = scope.enter(ctx);
    try {
      // constant propagation
      if(propagate != null)
        for(final Entry<Var, Value> e : propagate)
          ctx.set(e.getKey(), e.getValue(), info);

      expr = expr.comp(ctx, scope);
      scope.cleanUp(ctx, this);
    } finally {
      scope.exit(ctx, sf);
    }

    // convert all function calls in tail position to proper tail calls
    if(tco()) expr = expr.markTailCalls();

    if(ret != null) {
      // adopt expected return type
      type = ret;
      // remove redundant casts
      if((ret.type == AtomType.BLN || ret.type == AtomType.FLT ||
          ret.type == AtomType.DBL || ret.type == AtomType.QNM ||
          ret.type == AtomType.URI) && ret.eq(expr.type())) {
        ctx.compInfo(OPTCAST, ret);
        cast = false;
      }
    }

    compiling = false;
    compiled = true;
  }

  @Override
  public Item item(final QueryContext ctx, final InputInfo ii)
      throws QueryException {

    // reset context and evaluate function
    final Value cv = ctx.value;
    final Atts ns = ctx.sc.ns.reset();
    ctx.value = null;
    try {
      final Item it = expr.item(ctx, ii);
      // optionally promote return value to target type
      return cast ? ret.cast(it, false, ctx, info, this) : it;
    } finally {
      ctx.value = cv;
      ctx.sc.ns.stack(ns);
    }
  }

  @Override
  public Value value(final QueryContext ctx) throws QueryException {
    // reset context and evaluate function
    final Value cv = ctx.value;
    final Atts ns = ctx.sc.ns.reset();
    ctx.value = null;
    try {
      final Value v = ctx.value(expr);
      // optionally promote return value to target type
      return cast ? ret.promote(v, ctx, info) : v;
    } finally {
      ctx.value = cv;
      ctx.sc.ns.stack(ns);
    }
  }

  @Override
  public ValueIter iter(final QueryContext ctx) throws QueryException {
    return value(ctx).iter();
  }

  @Override
  public void plan(final Serializer ser) throws IOException {
    ser.openElement(this);
    ser.attribute(NAM, name.string());
    for(int i = 0; i < args.length; ++i) {
      ser.attribute(Token.token(ARG + i), args[i].name.string());
    }
    expr.plan(ser);
    ser.closeElement();
  }

  @Override
  public String toString() {
    final TokenBuilder tb = new TokenBuilder().add(DECLARE).add(' ').addExt(ann);
    if(updating) tb.add(UPDATING).add(' ');
    tb.add(FUNCTION).add(' ').add(name.string());
    tb.add(PAR1).addSep(args, SEP).add(PAR2);
    if(ret != null) tb.add(' ' + AS + ' ' + ret);
    if(expr != null) tb.add(" { " + expr + " }; ");
    return tb.toString();
  }

  /**
   * Checks if this function is tail-call optimizable.
   * @return {@code true} if it is optimizable, {@code false} otherwise
   */
  boolean tco() {
    return true;
  }

  @Override
  public final boolean visitVars(final VarVisitor visitor) {
    for(final LocalVarRef ref : scope.closure().values())
      if(!visitor.used(ref)) return false;
    return visitor.subScope(this);
  }

  @Override
  public boolean visit(final VarVisitor visitor) {
    return visitor.withVars(args, expr);
  }
}
