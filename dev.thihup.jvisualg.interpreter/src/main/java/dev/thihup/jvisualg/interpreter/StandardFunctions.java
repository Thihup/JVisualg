package dev.thihup.jvisualg.interpreter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Map;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

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
    private static final MethodHandle RADPGRAD;
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
            ABS = MethodHandles.lookup().findStatic(Math.class, "abs", MethodType.methodType(double.class, double.class));
            ARCCOS = MethodHandles.lookup().findStatic(Math.class, "acos", MethodType.methodType(double.class, double.class));
            ARCSEN = MethodHandles.lookup().findStatic(Math.class, "asin", MethodType.methodType(double.class, double.class));
            ARCTAN = MethodHandles.lookup().findStatic(Math.class, "atan", MethodType.methodType(double.class, double.class));
            COS = MethodHandles.lookup().findStatic(Math.class, "cos", MethodType.methodType(double.class, double.class));
            COTAN = MethodHandles.lookup().findStatic(Math.class, "tan", MethodType.methodType(double.class, double.class));
            EXP = MethodHandles.lookup().findStatic(Math.class, "exp", MethodType.methodType(double.class, double.class));
            GRAUPRAD = MethodHandles.lookup().findStatic(Math.class, "toRadians", MethodType.methodType(double.class, double.class));
            INT = MethodHandles.lookup().findStatic(Math.class, "floor", MethodType.methodType(double.class, double.class));
            LOG = MethodHandles.lookup().findStatic(Math.class, "log10", MethodType.methodType(double.class, double.class));
            LOGN = MethodHandles.lookup().findStatic(Math.class, "log", MethodType.methodType(double.class, double.class));
            PI = MethodHandles.lookup().findStaticGetter(Math.class, "PI", double.class);
            QUAD = MethodHandles.lookup().findStatic(Math.class, "pow", MethodType.methodType(double.class, double.class, double.class));
            RADPGRAD = MethodHandles.lookup().findStatic(Math.class, "toDegrees", MethodType.methodType(double.class, double.class));

            RAIZQ = MethodHandles.lookup().findStatic(Math.class, "sqrt", MethodType.methodType(double.class, double.class));
            RAND = MethodHandles.lookup().findStatic(Math.class, "random", MethodType.methodType(double.class));
            RANDI = MethodHandles.lookup().findVirtual(RandomGenerator.class, "nextInt", MethodType.methodType(int.class, int.class)).bindTo(RandomGenerator.getDefault());
            SEN = MethodHandles.lookup().findStatic(Math.class, "sin", MethodType.methodType(double.class, double.class));
            TAN = MethodHandles.lookup().findStatic(Math.class, "tan", MethodType.methodType(double.class, double.class));

            ASC = MethodHandles.lookup().findStatic(StandardFunctions.class, "asc", MethodType.methodType(int.class, String.class));
            CARAC = MethodHandles.lookup().findStatic(StandardFunctions.class, "carac", MethodType.methodType(String.class, Number.class));
            CARACPNUM = MethodHandles.lookup().findStatic(StandardFunctions.class, "caracpnum", MethodType.methodType(int.class, String.class));
            COMPR = MethodHandles.lookup().findStatic(StandardFunctions.class, "compr", MethodType.methodType(int.class, String.class));
            COPIA = MethodHandles.lookup().findStatic(StandardFunctions.class, "copia", MethodType.methodType(String.class, String.class, int.class, Integer.class));
            MAIUSC = MethodHandles.lookup().findStatic(StandardFunctions.class, "maiusc", MethodType.methodType(String.class, String.class));
            MINUSC = MethodHandles.lookup().findStatic(StandardFunctions.class, "minusc", MethodType.methodType(String.class, String.class));
            NUMPCARAC = MethodHandles.lookup().findStatic(StandardFunctions.class, "numpcarac", MethodType.methodType(String.class, Number.class));
            POS = MethodHandles.lookup().findStatic(StandardFunctions.class, "pos", MethodType.methodType(int.class, String.class, String.class));

            FUNCTIONS = Arrays.stream(StandardFunctions.class.getDeclaredFields()).filter(x -> !x.getName().equals("FUNCTIONS")).collect(Collectors.toMap(field -> field.getName().toLowerCase(), x -> {
                try {
                    return (MethodHandle) x.get(null);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static int asc(String s) {
        return s.charAt(0);
    }
    private static String carac(Number c) {
        return "" + c;
    }
    private static int caracpnum(String c) {
        return Integer.parseInt(c);
    }
    private static int compr(String c) {
        return c.length();
    }
    private static String copia(String c, int p, Integer n) {
        return c.substring(p, p + n - 1);
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
