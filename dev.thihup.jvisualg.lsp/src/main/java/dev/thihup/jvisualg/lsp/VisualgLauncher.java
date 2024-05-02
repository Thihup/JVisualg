package dev.thihup.jvisualg.lsp;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class VisualgLauncher {
    public static void main(String[] args) throws Throwable {

        VisualgLanguageServer server = new VisualgLanguageServer();

        Launcher<LanguageClient> serverLauncher = LSPLauncher.createServerLauncher(server, System.in, System.out, Executors.newSingleThreadExecutor(), null);
        server.connect(serverLauncher.getRemoteProxy());

        Future<Void> voidFuture = serverLauncher.startListening();
    }
}
