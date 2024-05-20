package dev.thihup.jvisualg.interpreter;

public sealed interface OutputEvent {
    record Text(String text) implements OutputEvent {}
    record Clear() implements OutputEvent {}
    record ChangeColor(Color color, Position position) implements OutputEvent {
        public enum Color {
            YELLOW, BLUE, WHITE, BLACK, GREEN, RED;

            public static Color fromString(String color) {
                return switch (color.toLowerCase()) {
                    case "amarelo" -> YELLOW;
                    case "azul" -> BLUE;
                    case "branco" -> WHITE;
                    case "preto" -> BLACK;
                    case "verde" -> GREEN;
                    case "vermelho" -> RED;
                    default -> throw new IllegalArgumentException("Invalid color: " + color);
                };
            }
        }
        public enum Position {
            FOREGROUND, BACKGROUND;

            public static Position fromString(String position) {
                return switch (position.toLowerCase()) {
                    case "frente" -> FOREGROUND;
                    case "fundos" -> BACKGROUND;
                    default -> throw new IllegalArgumentException("Invalid position: " + position);
                };
            }

        }
    }

}
