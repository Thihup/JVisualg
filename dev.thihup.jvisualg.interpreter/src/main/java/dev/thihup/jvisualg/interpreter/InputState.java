package dev.thihup.jvisualg.interpreter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.random.RandomGenerator;

sealed interface InputState {

    <E extends Throwable> InputValue generateValue(InputRequestValue requestValue) throws E;

    static InputState compose(InputState first, InputState second) {
        return new CompositeInputState(first, second);
    }

    record Aleatorio(RandomGenerator random, int start, int end, int decimalPlaces) implements InputState {
        @Override
        public InputValue generateValue(InputRequestValue requestValue) {
            return switch (requestValue.type()) {
                case CARACTER -> new InputValue.CaracterValue(random.ints(65, 91)
                        .limit(5)
                        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString());
                case LOGICO -> new InputValue.LogicoValue(random.nextBoolean());
                case REAL -> new InputValue.RealValue(generateRandomDouble(this));
                case INTEIRO -> new InputValue.InteiroValue(random.nextInt(start, end));
            };

        }

        private double generateRandomDouble(Aleatorio aleatorio) {
            double v = random.nextDouble(aleatorio.start(), aleatorio.end());

            if (aleatorio.decimalPlaces == 0) {
                return (int) v;
            }
            BigDecimal bd = new BigDecimal(v);
            bd = bd.setScale(aleatorio.decimalPlaces, RoundingMode.HALF_UP);
            return bd.doubleValue();

        }
    }

    record ReadInput(IO io) implements InputState {
        @Override
        public InputValue generateValue(InputRequestValue requestValue) throws Exception {
            return io.input().apply(requestValue).get().orElseThrow();
        }
    }

    final class Arquivo implements InputState {

        private final Scanner scanner;

        public Arquivo(String filePath) throws IOException {
            scanner = new Scanner(Paths.get(filePath));
        }

        @Override
        public InputValue generateValue(InputRequestValue requestValue) {
            String value = scanner.nextLine();
            return switch (requestValue.type()) {
                case CARACTER -> new InputValue.CaracterValue(value);
                case LOGICO -> new InputValue.LogicoValue(value.equalsIgnoreCase("VERDADEIRO"));
                case REAL -> new InputValue.RealValue(Double.parseDouble(value));
                case INTEIRO -> new InputValue.InteiroValue(Integer.parseInt(value));
            };
        }
    }
}

final class CompositeInputState implements InputState {
    private final InputState first;
    private final InputState second;

    public CompositeInputState(InputState first, InputState second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public InputValue generateValue(InputRequestValue requestValue) {
        try {
            return first.generateValue(requestValue);
        } catch (Exception e) {
            try {
            return second.generateValue(requestValue);
            } catch (Exception ee){
                ee.addSuppressed(e);
                throw e;
            }
        }
    }
}
