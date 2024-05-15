package dev.thihup.jvisualg.interpreter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.random.RandomGenerator;

public class StandardFunctions {
    private static final MethodHandle ABS;
    private static final MethodHandle ARCCOS;
    private static final MethodHandle ARCSEN;
    private static final MethodHandle ARCTAN;
    private static final MethodHandle COS;
    private static final MethodHandle COTAN;
    private static final MethodHandle EXP;
    private static final MethodHandle GRAUPRAD;
    private static final MethodHandle INT;
    private static final MethodHandle LOG;
    private static final MethodHandle LOGN;
    private static final MethodHandle PI;
    private static final MethodHandle QUAD;
    private static final MethodHandle RADPGRAU;
    private static final MethodHandle RAIZQ;
    private static final MethodHandle RAND;
    private static final MethodHandle RANDI;
    private static final MethodHandle SEN;
    private static final MethodHandle TAN;

    private static final MethodHandle ASC;
    private static final MethodHandle CARAC;
    private static final MethodHandle CARACPNUM;
    private static final MethodHandle COMPR;
    private static final MethodHandle COPIA;
    private static final MethodHandle MAIUSC;
    private static final MethodHandle MINUSC;
    private static final MethodHandle NUMPCARAC;
    private static final MethodHandle POS;

    public static final Map<String, MethodHandle> FUNCTIONS;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodType doubleToDobule = MethodType.methodType(double.class, double.class);
            ABS = lookup.findStatic(Math.class, "abs", doubleToDobule);
            ARCCOS = lookup.findStatic(Math.class, "acos", doubleToDobule);
            ARCSEN = lookup.findStatic(Math.class, "asin", doubleToDobule);
            ARCTAN = lookup.findStatic(Math.class, "atan", doubleToDobule);
            COS = lookup.findStatic(Math.class, "cos", doubleToDobule);
            COTAN = lookup.findStatic(StandardFunctions.class, "cotan", doubleToDobule);
            EXP = lookup.findStatic(Math.class, "pow", MethodType.methodType(double.class, double.class, double.class));
            GRAUPRAD = lookup.findStatic(Math.class, "toRadians", doubleToDobule);
            INT = lookup.findVirtual(Number.class, "intValue", MethodType.methodType(int.class));
            LOG = lookup.findStatic(Math.class, "log10", doubleToDobule);
            LOGN = lookup.findStatic(Math.class, "log", doubleToDobule);
            PI = lookup.findStaticGetter(Math.class, "PI", double.class);
            QUAD = MethodHandles.insertArguments(EXP, 1, 2);
            RADPGRAU = lookup.findStatic(Math.class, "toDegrees", doubleToDobule);
            RAIZQ = lookup.findStatic(Math.class, "sqrt", doubleToDobule);
            RAND = lookup.findStatic(Math.class, "random", MethodType.methodType(double.class));
            RANDI = lookup.findVirtual(RandomGenerator.class, "nextInt", MethodType.methodType(int.class, int.class)).bindTo(RandomGenerator.getDefault());
            SEN = lookup.findStatic(Math.class, "sin", doubleToDobule);
            TAN = lookup.findStatic(Math.class, "tan", doubleToDobule);
            MethodType stringToInt = MethodType.methodType(int.class, String.class);
            ASC = lookup.findStatic(StandardFunctions.class, "asc", stringToInt);
            CARAC = lookup.findStatic(StandardFunctions.class, "carac", MethodType.methodType(String.class, Number.class));
            CARACPNUM = lookup.findStatic(StandardFunctions.class, "caracpnum", stringToInt);
            COMPR = lookup.findStatic(StandardFunctions.class, "compr", stringToInt);
            COPIA = lookup.findStatic(StandardFunctions.class, "copia", MethodType.methodType(String.class, String.class, int.class, int.class));
            MethodType stringToString = MethodType.methodType(String.class, String.class);
            MAIUSC = lookup.findStatic(StandardFunctions.class, "maiusc", stringToString);
            MINUSC = lookup.findStatic(StandardFunctions.class, "minusc", stringToString);
            NUMPCARAC = lookup.findStatic(StandardFunctions.class, "numpcarac", MethodType.methodType(String.class, Number.class));
            POS = lookup.findStatic(StandardFunctions.class, "pos", MethodType.methodType(int.class, String.class, String.class));
            FUNCTIONS = Map.ofEntries(
                    Map.entry("abs", ABS),
                    Map.entry("arccos", ARCCOS),
                    Map.entry("arcsen", ARCSEN),
                    Map.entry("arctan", ARCTAN),
                    Map.entry("cos", COS),
                    Map.entry("cotan", COTAN),
                    Map.entry("exp", EXP),
                    Map.entry("grauprad", GRAUPRAD),
                    Map.entry("int", INT),
                    Map.entry("log", LOG),
                    Map.entry("logn", LOGN),
                    Map.entry("pi", PI),
                    Map.entry("quad", QUAD),
                    Map.entry("radpgrau", RADPGRAU),
                    Map.entry("raizq", RAIZQ),
                    Map.entry("rand", RAND),
                    Map.entry("randi", RANDI),
                    Map.entry("sen", SEN),
                    Map.entry("tan", TAN),
                    Map.entry("asc", ASC),
                    Map.entry("carac", CARAC),
                    Map.entry("caracpnum", CARACPNUM),
                    Map.entry("compr", COMPR),
                    Map.entry("copia", COPIA),
                    Map.entry("maiusc", MAIUSC),
                    Map.entry("minusc", MINUSC),
                    Map.entry("numpcarac", NUMPCARAC),
                    Map.entry("pos", POS)
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static double cotan(double angleInRadians) {
        return 1.0 / Math.tan(angleInRadians);
    }
    private static int asc(String s) {
        return s.codePointAt(0);
    }
    private static String carac(Number c) {
        return Character.toString(c.intValue());
    }
    private static int caracpnum(String c) {
        return Integer.parseInt(c);
    }
    private static int compr(String c) {
        return c.length();
    }
    private static String copia(String c, int p, int n) {
        return c.substring(p - 1, p + n - 1);
    }
    private static String maiusc(String c) {
        return c.toUpperCase();
    }
    private static String minusc(String c) {
        return c.toLowerCase();
    }
    private static String numpcarac(Number n) {
        return n.toString();
    }
    private static int pos(String subc, String c) {
        return c.indexOf(subc) + 1;
    }

}
