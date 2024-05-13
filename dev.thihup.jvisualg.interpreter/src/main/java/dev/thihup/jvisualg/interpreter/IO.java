package dev.thihup.jvisualg.interpreter;

import java.util.function.Consumer;
import java.util.function.Function;

record IO(Function<InputRequestValue, InputValue> input, Consumer<String> output) {}
