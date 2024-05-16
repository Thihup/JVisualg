package dev.thihup.jvisualg.interpreter;

import dev.thihup.jvisualg.frontend.node.Node;

import java.util.Map;

public record UserDefinedType(String name, Map<String, Node.TypeNode> fields) {}
