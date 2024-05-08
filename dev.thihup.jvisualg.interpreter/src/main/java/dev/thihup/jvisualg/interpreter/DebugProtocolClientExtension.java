package dev.thihup.jvisualg.interpreter;

import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;

import java.util.concurrent.CompletableFuture;

public interface DebugProtocolClientExtension extends org.eclipse.lsp4j.debug.services.IDebugProtocolClient {

    @JsonNotification("input")
    CompletableFuture<String> input();

}
