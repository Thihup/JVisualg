package dev.thihup.jvisualg.interpreter;

import java.util.Map;

public record UserDefinedValue(UserDefinedType type, Map<String, Object> values) {}
