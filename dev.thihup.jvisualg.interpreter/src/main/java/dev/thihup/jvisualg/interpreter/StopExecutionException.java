package dev.thihup.jvisualg.interpreter;

class StopExecutionException extends RuntimeException {
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
