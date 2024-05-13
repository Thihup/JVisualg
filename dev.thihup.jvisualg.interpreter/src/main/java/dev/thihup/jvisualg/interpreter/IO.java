package dev.thihup.jvisualg.interpreter;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public record IO(Function<InputRequestValue, CompletableFuture<InputValue>> input, Consumer<String> output) {}
