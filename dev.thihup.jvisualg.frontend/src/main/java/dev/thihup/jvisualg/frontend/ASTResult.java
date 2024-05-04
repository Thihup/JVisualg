package dev.thihup.jvisualg.frontend;

import dev.thihup.jvisualg.frontend.node.Node;

import java.util.List;
import java.util.Optional;

public record ASTResult(Optional<Node> node, List<Error> errors) {
}
