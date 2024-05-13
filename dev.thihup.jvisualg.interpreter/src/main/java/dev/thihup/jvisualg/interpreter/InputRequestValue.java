package dev.thihup.jvisualg.interpreter;

public record InputRequestValue(String variableName, Type type) {
    public enum Type {
        CARACTER, LOGICO, REAL, INTEIRO;

        public static Type fromClass(Class<?> classType) {
            return switch (classType) {
                case Class<?> x when x == Integer.class -> INTEIRO;
                case Class<?> x when x == Double.class -> REAL;
                case Class<?> x when x == Boolean.class -> LOGICO;
                case Class<?> x when x == String.class -> CARACTER;
                default -> throw new IllegalArgumentException(classType.getName());
            };
        }
    }
}
