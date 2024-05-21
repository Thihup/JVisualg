package dev.thihup.jvisualg.interpreter;

class BreakException extends RuntimeException {
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
