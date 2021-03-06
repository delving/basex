package org.basex.query.func;

import static org.basex.query.QueryText.*;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.util.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.util.*;

/**
 * Partial function application.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Leo Woerteler
 */
public final class PartFunc extends UserFunc {
  /**
   * Function constructor for static calls.
   * @param ii input info
   * @param fun typed function expression
   * @param arg arguments
   */
  public PartFunc(final InputInfo ii, final TypedFunc fun, final Var[] arg) {
    super(ii, new QNm(), nn(fun.type.type(arg)), fun.ret(), null);
    expr = fun.fun;
  }

  /**
   * Function constructor for dynamic calls.
   * @param ii input info
   * @param func function expression
   * @param arg arguments
   */
  public PartFunc(final InputInfo ii, final Expr func, final Var[] arg) {
    // [LW] XQuery/HOF: dynamic type propagation
    super(ii, new QNm(), nn(arg), func.type(), null);
    expr = func;
  }

  @Override
  public Expr compile(final QueryContext ctx) throws QueryException {
    compile(ctx, false);
    // defer creation of function item because of closure
    return new InlineFunc(info, ret, args, expr, ann).compile(ctx);
  }

  @Override
  public void plan(final FElem plan) {
    final FElem el = planElem();
    addPlan(plan, el, expr);
    for(int i = 0; i < args.length; ++i) {
      el.add(planAttr(ARG + i, args[i].name.string()));
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(FUNCTION).append('(');
    for(final Var v : args)
      sb.append(v).append(v == args[args.length - 1] ? "" : ", ");
    return sb.append(") { ").append(expr).append(" }").toString();
  }

  /**
   * Collects all non-{@code null} variables from the array.
   * @param vars array of variables, can contain {@code null}s
   * @return all non-{@code null} variables
   */
  private static Var[] nn(final Var[] vars) {
    Var[] out = {};
    for(final Var v : vars) if(v != null) out = Array.add(out, v);
    return out;
  }

  @Override
  boolean tco() {
    return false;
  }
}
