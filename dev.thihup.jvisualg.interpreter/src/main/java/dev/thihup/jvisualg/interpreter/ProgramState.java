package dev.thihup.jvisualg.interpreter;

import java.util.Map;

public record ProgramState(int lineNumber, Map<String, Map<String, Object>> stack) {
}
