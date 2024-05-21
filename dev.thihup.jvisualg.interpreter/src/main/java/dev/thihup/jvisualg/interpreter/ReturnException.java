package dev.thihup.jvisualg.interpreter;

class ReturnException extends RuntimeException {
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
