package org.basex.query.func;

import static javax.xml.datatype.DatatypeConstants.*;
import static org.basex.query.QueryText.*;
import static org.basex.query.util.Err.*;
import static org.basex.util.Token.*;

import java.lang.reflect.*;
import java.math.*;
import java.net.*;
import java.util.*;

import javax.xml.datatype.*;
import javax.xml.namespace.*;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.item.*;
import org.basex.query.item.Type;
import org.basex.query.iter.*;
import org.basex.util.*;
import org.w3c.dom.*;

/**
 * This class contains common methods for executing Java code and mapping
 * Java objects to XQuery values.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public abstract class JavaMapping extends Arr {
  /** New keyword. */
  static final String NEW = "new";
  /** Input Java types. */
  private static final Class<?>[] JAVA = {
    String.class,     boolean.class,  Boolean.class,      byte.class,
    Byte.class,       short.class,    Short.class,        int.class,
    Integer.class,    long.class,     Long.class,         float.class,
    Float.class,      double.class,   Double.class,       BigDecimal.class,
    BigInteger.class, QName.class,    CharSequence.class, char.class,
    Character.class,  URI.class,      URL.class,          Map.class
  };
  /** Resulting XQuery types. */
  private static final Type[] XQUERY = {
    AtomType.STR, AtomType.BLN, AtomType.BLN, AtomType.BYT,
    AtomType.BYT, AtomType.SHR, AtomType.SHR, AtomType.INT,
    AtomType.INT, AtomType.LNG, AtomType.LNG, AtomType.FLT,
    AtomType.FLT, AtomType.DBL, AtomType.DBL, AtomType.DEC,
    AtomType.ITR, AtomType.QNM, AtomType.STR, AtomType.STR,
    AtomType.STR, AtomType.URI, AtomType.URI, FuncType.ANY_FUN
  };

  /**
   * Constructor.
   * @param ii input info
   * @param a arguments
   */
  JavaMapping(final InputInfo ii, final Expr[] a) {
    super(ii, a);
  }

  @Override
  public final Iter iter(final QueryContext ctx) throws QueryException {
    return value(ctx).iter();
  }

  @Override
  public final Value value(final QueryContext ctx) throws QueryException {
    final Value[] args = new Value[expr.length];
    for(int a = 0; a < expr.length; ++a) {
      args[a] = ctx.value(expr[a]);
      if(args[a].isEmpty()) XPEMPTY.thrw(input, description());
    }
    return toValue(eval(args, ctx));
  }

  /**
   * Returns the result of the evaluated Java function.
   * @param args arguments
   * @param ctx query context
   * @return arguments
   * @throws QueryException query exception
   */
  protected abstract Object eval(final Value[] args, final QueryContext ctx)
      throws QueryException;

  /**
   * Converts the specified result to an XQuery value.
   * @param res result object
   * @return value
   * @throws QueryException query exception
   */
  public static Value toValue(final Object res) throws QueryException {
    if(res == null) return Empty.SEQ;
    if(res instanceof Value) return (Value) res;
    if(res instanceof Iter) return ((Iter) res).value();
    // find XQuery mapping for specified type
    final Type type = type(res);
    if(type != null) return type.cast(res, null);

    if(!res.getClass().isArray()) return new Jav(res);

    final ValueBuilder vb = new ValueBuilder();
    if(res instanceof boolean[]) {
      for(final boolean o : (boolean[]) res) vb.add(Bln.get(o));
    } else if(res instanceof char[]) {
      vb.add(Str.get(new String((char[]) res)));
    } else if(res instanceof byte[]) {
      for(final byte o : (byte[]) res) vb.add(new Int(o, AtomType.BYT));
    } else if(res instanceof short[]) {
      for(final short o : (short[]) res) vb.add(Int.get(o));
    } else if(res instanceof int[]) {
      for(final int o : (int[]) res) vb.add(Int.get(o));
    } else if(res instanceof long[]) {
      for(final long o : (long[]) res) vb.add(Int.get(o));
    } else if(res instanceof float[]) {
      for(final float o : (float[]) res) vb.add(Flt.get(o));
    } else if(res instanceof double[]) {
      for(final double o : (double[]) res) vb.add(Dbl.get(o));
    } else {
      for(final Object o : (Object[]) res) {
        vb.add(o instanceof Value ? (Value) o : new Jav(o));
      }
    }
    return vb.value();
  }

  /**
   * Returns a new Java function instance.
   * @param name function name
   * @param args arguments
   * @param ctx query context
   * @param ii input info
   * @return Java function
   * @throws QueryException query exception
   */
  static JavaMapping get(final QNm name, final Expr[] args, final QueryContext ctx,
      final InputInfo ii) throws QueryException {

    // resolve function name: convert dashes to upper-case initials
    final TokenBuilder m = new TokenBuilder();
    final byte[] ln = name.local();
    boolean dash = false;
    for(int p = 0; p < ln.length; p += cl(ln, p)) {
      final int ch = cp(ln, p);
      if(dash) {
        m.add(Character.toUpperCase(ch));
        dash = false;
      } else {
        dash = ch == '-';
        if(!dash) m.add(ch);
      }
    }
    final String mth = m.toString();

    // class path
    final String path = string(substring(name.uri(), JAVAPRE.length));

    // finds imported Java module
    final Object jm  = ctx.modules.findJava(path);
    if(jm != null) {
      for(final Method meth : jm.getClass().getMethods()) {
        if(meth.getName().equals(mth)) return new JavaModuleFunc(ii, jm, meth, args);
      }
      WHICHJAVA.thrw(ii, path + '.' + mth);
    }

    // finds direct class reference
    Class<?> cls = Reflect.find(path);
    // finds reference in EXPath module
    if(cls == null) cls = ctx.modules.find(path);
    // return exception if none of the lookups was successful
    if(cls == null) WHICHJAVA.thrw(ii, path + '.' + mth);

    return new JavaFunc(ii, cls, mth, args);
  }

  /**
   * Returns an appropriate XQuery data type for the specified Java object.
   * @param o object
   * @return xquery type, or {@code null} if no appropriate type was found
   */
  public static Type type(final Object o) {
    final Type t = type(o.getClass());
    if(t != null) return t;

    if(o instanceof Element) return NodeType.ELM;
    if(o instanceof Document) return NodeType.DOC;
    if(o instanceof DocumentFragment) return NodeType.DOC;
    if(o instanceof Attr) return NodeType.ATT;
    if(o instanceof Comment) return NodeType.COM;
    if(o instanceof ProcessingInstruction) return NodeType.PI;
    if(o instanceof Text) return NodeType.TXT;

    if(o instanceof Duration) {
      final Duration d = (Duration) o;
      return !d.isSet(YEARS) && !d.isSet(MONTHS) ? AtomType.DTD :
        !d.isSet(HOURS) && !d.isSet(MINUTES) && !d.isSet(SECONDS) ?
          AtomType.YMD : AtomType.DUR;
    }

    if(o instanceof XMLGregorianCalendar) {
      final QName type = ((XMLGregorianCalendar) o).getXMLSchemaType();
      if(type == DATE) return AtomType.DAT;
      if(type == DATETIME) return AtomType.DTM;
      if(type == TIME) return AtomType.TIM;
      if(type == GYEARMONTH) return AtomType.YMO;
      if(type == GMONTHDAY) return AtomType.MDA;
      if(type == GYEAR) return AtomType.YEA;
      if(type == GMONTH) return AtomType.MON;
      if(type == GDAY) return AtomType.DAY;
    }
    return null;
  }

  /**
   * Returns an appropriate XQuery data type for the specified Java class.
   * @param type Java type
   * @return xquery type
   */
  static Type type(final Class<?> type) {
    for(int j = 0; j < JAVA.length; ++j) {
      if(JAVA[j].isAssignableFrom(type)) return XQUERY[j];
    }
    return null;
  }

  @Override
  public boolean uses(final Use u) {
    return u == Use.NDT || super.uses(u);
  }
}
