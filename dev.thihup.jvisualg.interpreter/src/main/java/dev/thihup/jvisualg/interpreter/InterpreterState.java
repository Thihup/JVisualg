package dev.thihup.jvisualg.interpreter;

public sealed interface InterpreterState {
    enum NotStarted implements InterpreterState {INSTANCE}

    enum Running implements InterpreterState {INSTANCE}

    record PausedDebug(int lineNumber) implements InterpreterState {
    }

    enum CompletedSuccessfully implements InterpreterState {INSTANCE}

    record CompletedExceptionally(Throwable throwable) implements InterpreterState {
    }

    enum ForcedStop implements InterpreterState {INSTANCE}
}
