package dev.thihup.jvisualg.frontend;

import dev.thihup.jvisualg.frontend.node.Location;

public record Error(String message, Location location) {
}
