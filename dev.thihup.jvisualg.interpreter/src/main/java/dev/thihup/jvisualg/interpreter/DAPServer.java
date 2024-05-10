package dev.thihup.jvisualg.interpreter;

import dev.thihup.jvisualg.frontend.ASTResult;
import dev.thihup.jvisualg.frontend.VisualgParser;
import dev.thihup.jvisualg.frontend.node.Location;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

public class DAPServer implements IDebugProtocolServer {

    private Reader reader;
    private Writer writer;

    Interpreter interpreter;

    private ReentrantLock lock = new ReentrantLock();
    private DebugProtocolClientExtension client;

    @Override
    public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
        reader = new Reader() {
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                return client.input().thenApply(x -> {
                    x.getChars(0, x.length(), cbuf, off);
                    return x.length();
                }).join();
            }

            @Override
            public void close() throws IOException {

            }
        };


        writer = new StringWriter();
        interpreter = new Interpreter(reader, new FilterWriter(writer) {
            @Override
            public void write(String str, int off, int len) throws IOException {
                super.write(str, off, len);

                OutputEventArguments args1 = new OutputEventArguments();
                args1.setOutput(str.substring(off, off + len));
                args1.setCategory(OutputEventArgumentsCategory.STDOUT);
                client.output(args1);
            }
        }, lock);
        return CompletableFuture.completedFuture(new Capabilities());
    }

    @Override
    public CompletableFuture<Void> launch(Map<String, Object> args) {
        return CompletableFuture.runAsync(() -> {
            ByteArrayInputStream source = new ByteArrayInputStream(args.get("source").toString().getBytes(StandardCharsets.ISO_8859_1));
            ASTResult astResult = VisualgParser.parse(source);

            OutputEventArguments outputStart = new OutputEventArguments();
            outputStart.setOutput("Início da execução\n");
            client.output(outputStart);

            try {
                astResult.node().ifPresent(interpreter::run);
                OutputEventArguments outputEnd = new OutputEventArguments();
                outputEnd.setOutput("\nFim da execução.");
                client.output(outputEnd);
            } catch (Exception e) {
                OutputEventArguments outputEnd = new OutputEventArguments();
                outputEnd.setOutput("\nErro: " + e.getMessage());
                client.output(outputEnd);
            }
            client.terminated(new TerminatedEventArguments());
            reset();
        });
    }

    private void reset() {
    }

    @Override
    public CompletableFuture<Void> stepIn(StepInArguments args) {
        lock.unlock();
        return CompletableFuture.completedFuture(null);
    }

    // F9
    @Override
    public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
        lock.unlock();
        return CompletableFuture.completedFuture(new ContinueResponse());
    }

    @Override
    public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args) {
        Arrays.stream(args.getBreakpoints())
            .map(x -> new Location(x.getLine(), x.getColumn(), x.getLine(), x.getColumn()))
            .forEach(interpreter::addBreakpoint);
        return CompletableFuture.completedFuture(new SetBreakpointsResponse());
    }

    public void connect(DebugProtocolClientExtension remoteProxy) {
        this.client = remoteProxy;
    }
}
