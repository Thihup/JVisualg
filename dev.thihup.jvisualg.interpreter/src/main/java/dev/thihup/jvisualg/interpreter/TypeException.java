package dev.thihup.jvisualg.interpreter;

public sealed class TypeException extends RuntimeException {
    public TypeException(String message) {
        super(message);
    }


    public static final class WrongNumberOfArguments extends TypeException {
        public WrongNumberOfArguments(int expected, int actual) {
            super("Expected " + expected + " arguments, but got " + actual + ".");
        }
    }

    public static final class FunctionNotFound extends TypeException {
        public FunctionNotFound(String functionName) {
            super("Function " + functionName + " not found.");
        }
    }

    public static final class ProcedureNotFound extends TypeException {
        public ProcedureNotFound(String procedureName) {
            super("Procedure " + procedureName + " not found.");
        }
    }

    public static final class VariableNotFound extends TypeException {
        public VariableNotFound(String variableName) {
            super("Variable " + variableName + " not found.");
        }
    }

    public static final class InvalidAssignment extends TypeException {
        public InvalidAssignment(Class<?> expected, Class<?> actual) {
            super("Cannot assign a value of type " + TypeException.toVisualgClass(actual) + " to a variable of type " + TypeException.toVisualgClass(expected) + ".");
        }
    }

    public static final class MissingInput extends TypeException {
        public MissingInput(InputRequestValue inputName) {
            super("Missing input for " + inputName.variableName() + ".");
        }
    }

    public static final class InvalidIndex extends TypeException {
        public InvalidIndex(int index) {
            super("Unsupported number of indexes: " + index);
        }
    }

    public static final class InvalidOperand extends TypeException {
        public enum Operator {
            ADD("+"),
            SUBTRACT("-"),
            MULTIPLY("*"),
            DIVIDE("/"),
            MODULO("%"),
            POW("^"),
            EQUALS("="),
            NOT_EQUALS("<>"),
            GREATER_THAN(">"),
            LESS_THAN("<"),
            GREATER_THAN_OR_EQUALS(">="),
            LESS_THAN_OR_EQUALS("<="),
            AND("e"),
            OR("ou"),
            NOT("nao");
            final String operator;

            Operator(String operator) {
                this.operator = operator;
            }
        }
        public InvalidOperand(Operator operator, Class<?> left, Class<?> right) {
            super("Invalid operand types for operator " + operator.operator + ": " + TypeException.toVisualgClass(left) + " and " + TypeException.toVisualgClass(right) + ".");
        }
        public InvalidOperand(Operator operator, Class<?> opClass) {
            super("Invalid operand types for operator " + operator.operator + ":  " + TypeException.toVisualgClass(opClass) + ".");
        }
    }

    private static String toVisualgClass(Class<?> clazz) {
        if (clazz == Integer.class) {
            return "Inteiro";
        } else if (clazz == Double.class) {
            return "Real";
        } else if (clazz == String.class) {
            return "Caractere";
        } else if (clazz == Boolean.class) {
            return "Logico";
        } else {
            return clazz.getSimpleName();
        }
    }
}
