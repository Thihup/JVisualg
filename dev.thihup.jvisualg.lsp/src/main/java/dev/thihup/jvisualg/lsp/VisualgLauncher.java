package dev.thihup.jvisualg.lsp;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class VisualgLauncher {
    public static void main(String[] args) throws Throwable {
        startServer(System.in, System.out, Executors.newSingleThreadExecutor());
    }

    public static Future<Void> startServer(InputStream inputStream, OutputStream outputStream, ExecutorService executor) {
        VisualgLanguageServer server = new VisualgLanguageServer();

        Launcher<LanguageClient> serverLauncher = LSPLauncher.createServerLauncher(server, inputStream, outputStream, executor, null);
        server.connect(serverLauncher.getRemoteProxy());

        return serverLauncher.startListening();
    }
}
