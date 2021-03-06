package org.basex.query.expr;

import static org.basex.query.QueryText.*;

import org.basex.query.*;
import org.basex.query.func.*;
import org.basex.query.iter.*;
import org.basex.query.util.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.value.type.*;
import org.basex.util.*;
import org.basex.util.list.*;

/**
 * If expression.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public final class If extends Arr {
  /** If expression. */
  private Expr cond;

  /**
   * Constructor.
   * @param ii input info
   * @param c condition
   * @param t then clause
   * @param e else clause
   */
  public If(final InputInfo ii, final Expr c, final Expr t, final Expr e) {
    super(ii, t, e);
    cond = c;
  }

  @Override
  public void checkUp() throws QueryException {
    checkNoUp(cond);
    checkAllUp(expr);
  }

  @Override
  public Expr compile(final QueryContext ctx) throws QueryException {
    cond = cond.compile(ctx).compEbv(ctx);
    // static condition: return branch in question
    if(cond.isValue()) return optPre(eval(ctx).compile(ctx), ctx);

    // compile and simplify branches
    super.compile(ctx);

    // if A then B else B -> B (errors in A will be ignored)
    if(expr[0].sameAs(expr[1])) return optPre(expr[0], ctx);

    // if not(A) then B else C -> if A then C else B
    if(cond.isFunction(Function.NOT)) {
      ctx.compInfo(OPTWRITE, this);
      cond = ((StandardFunc) cond).expr[0];
      final Expr tmp = expr[0];
      expr[0] = expr[1];
      expr[1] = tmp;
    }

    // if A then true() else false() -> boolean(A)
    if(expr[0] == Bln.TRUE && expr[1] == Bln.FALSE) {
      ctx.compInfo(OPTWRITE, this);
      return compBln(cond);
    }

    // if A then false() else true() -> not(A)
    // if A then B else true() -> not(A) or B
    if(expr[0].type().eq(SeqType.BLN) && expr[1] == Bln.TRUE) {
      ctx.compInfo(OPTWRITE, this);
      final Expr e = Function.NOT.get(info, cond);
      return expr[0] == Bln.FALSE ? e : new Or(info, e, expr[0]);
    }

    type = expr[0].type().intersect(expr[1].type());
    return this;
  }

  @Override
  public Iter iter(final QueryContext ctx) throws QueryException {
    return ctx.iter(eval(ctx));
  }

  @Override
  public Value value(final QueryContext ctx) throws QueryException {
    return ctx.value(eval(ctx));
  }

  @Override
  public Item item(final QueryContext ctx, final InputInfo ii) throws QueryException {
    return eval(ctx).item(ctx, info);
  }

  /**
   * Evaluates the condition and returns the matching expression.
   * @param ctx query context
   * @return resulting expression
   * @throws QueryException query exception
   */
  private Expr eval(final QueryContext ctx) throws QueryException {
    return expr[cond.ebv(ctx, info).bool(info) ? 0 : 1];
  }
  @Override
  public boolean uses(final Use u) {
    return cond.uses(u) || super.uses(u);
  }

  @Override
  public int count(final Var v) {
    return cond.count(v) + super.count(v);
  }

  @Override
  public boolean removable(final Var v) {
    return cond.removable(v) && super.removable(v);
  }

  @Override
  public Expr remove(final Var v) {
    cond = cond.remove(v);
    return super.remove(v);
  }

  @Override
  public boolean databases(final StringList db) {
    return cond.databases(db) && super.databases(db);
  }

  @Override
  public Expr indexEquivalent(final IndexContext ic) throws QueryException {
    for(int e = 0; e < expr.length; ++e) expr[e] = expr[e].indexEquivalent(ic);
    return this;
  }

  @Override
  public boolean isVacuous() {
    return expr[0].isVacuous() || expr[1].isVacuous();
  }

  @Override
  public Expr markTailCalls() {
    expr[0] = expr[0].markTailCalls();
    expr[1] = expr[1].markTailCalls();
    return this;
  }

  @Override
  public void plan(final FElem plan) {
    addPlan(plan, planElem(), cond, expr);
  }

  @Override
  public String toString() {
    return IF + '(' + cond + ") " + THEN + ' ' + expr[0] + ' ' + ELSE + ' ' + expr[1];
  }
}
