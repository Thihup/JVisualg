package dev.thihup.jvisualg.interpreter;

public sealed interface InputValue {
    record InteiroValue(int value) implements InputValue {
    }

    record RealValue(double value) implements InputValue {
    }

    record CaracterValue(String value) implements InputValue {
    }

    record LogicoValue(boolean value) implements InputValue {
    }
}
