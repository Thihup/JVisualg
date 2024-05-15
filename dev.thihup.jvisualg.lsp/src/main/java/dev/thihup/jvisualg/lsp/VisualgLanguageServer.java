package dev.thihup.jvisualg.lsp;

import dev.thihup.jvisualg.frontend.Error;
import dev.thihup.jvisualg.frontend.*;
import dev.thihup.jvisualg.frontend.node.Location;
import dev.thihup.jvisualg.frontend.node.Node;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class VisualgLanguageServer implements LanguageServer, LanguageClientAware, WorkspaceService, TextDocumentService {
    private LanguageClient client;

    private final Map<String, String> documentContent = new HashMap<>();

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams initializeParams) {
        return CompletableFuture.supplyAsync(() -> {
            ServerCapabilities capabilities = new ServerCapabilities();

            ServerInfo visualg = new ServerInfo("JVisualg", "3.0");
            capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);

            CompletionOptions completionProvider = new CompletionOptions();
            CompletionItemOptions completionItem = new CompletionItemOptions();
            completionProvider.setCompletionItem(completionItem);

            capabilities.setCompletionProvider(completionProvider);

            DiagnosticRegistrationOptions diagnosticProvider = new DiagnosticRegistrationOptions();
            diagnosticProvider.setInterFileDependencies(false);
            diagnosticProvider.setWorkspaceDiagnostics(false);
            capabilities.setDiagnosticProvider(diagnosticProvider);

            capabilities.setDocumentHighlightProvider(true);
            return new InitializeResult(capabilities, visualg);
        });
    }

    @Override
    public void initialized(InitializedParams params) {
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        exit();
        return CompletableFuture.completedFuture(new Object());
    }

    @Override
    public void exit() {
        documentContent.clear();
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return this;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return this;
    }

    @Override
    public void connect(LanguageClient languageClient) {
        languageClient.logMessage(new MessageParams(MessageType.Info, "Connected to Visualg Language Server"));
        this.client = languageClient;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        try {
            TextDocumentItem textDocument = params.getTextDocument();
            String uri = textDocument.getUri();
            documentContent.put(uri, textDocument.getText());
            checkForErrors(uri);
        } catch (Exception e) {
            client.logMessage(new MessageParams(MessageType.Error, "didOpen: " + Arrays.toString(e.getStackTrace())));
        }
    }

    private void checkForErrors(String uri) {
        try {
            String content = documentContent.get(uri);
            if (content == null) {
                return;
            }

            ASTResult astResult = VisualgParser.parse(content);

            List<Error> errors = astResult.errors();

            astResult.node()
                    .map(TypeChecker::semanticAnalysis)
                    .map(TypeCheckerResult::errors)
                    .ifPresent(errors::addAll);

            List<Diagnostic> list = errors.stream()
                    .map(VisualgLanguageServer::toDiagnostic)
                    .toList();

            PublishDiagnosticsParams diagnostics = new PublishDiagnosticsParams(content, list);
            client.publishDiagnostics(diagnostics);
        } catch (Throwable e) {
            client.logMessage(new MessageParams(MessageType.Error, "checkForErrors: " + Arrays.toString(e.getStackTrace())));
        }
    }

    private static Diagnostic toDiagnostic(Error s) {
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setMessage(s.message());
        diagnostic.setSeverity(DiagnosticSeverity.Error);
        Location location = s.location();
        Position startPosition = new Position(location.startLine() - 1, location.startColumn());
        Position endPosition = new Position(location.endLine() - 1, location.endColumn());
        diagnostic.setRange(new Range(startPosition, endPosition));
        return diagnostic;
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        try {
            String uri = params.getTextDocument().getUri();
            documentContent.put(uri, params.getContentChanges().getFirst().getText());
            checkForErrors(uri);
        } catch (Exception e) {
            client.logMessage(new MessageParams(MessageType.Error, "didChange: " + Arrays.toString(e.getStackTrace())));
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        try {
            documentContent.remove(params.getTextDocument().getUri());
        } catch (Exception e) {
            client.logMessage(new MessageParams(MessageType.Error, "didClose: " + Arrays.toString(e.getStackTrace())));
        }
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        try {
            documentContent.put(params.getTextDocument().getUri(), params.getText());
            checkForErrors(params.getTextDocument().getUri());
        } catch (Exception e) {
            client.logMessage(new MessageParams(MessageType.Error, e.getMessage()));
        }
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
    }


    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
        return CompletableFuture.supplyAsync(() -> Either.forLeft(CodeCompletion.HOLDER));
    }

    @Override
    public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams params) {
        return CompletableFuture.supplyAsync(() -> {
            ASTResult astResult = VisualgParser.parse(documentContent.get(params.getTextDocument().getUri()));

            Node node = astResult.node().orElseThrow();

            List<Node> possibleMatches = findPossibleMatches(node, positionToLocation(params.getPosition()));

            return findMostSpecificNode(possibleMatches)
                    .map(x -> new DocumentHighlight(locationToRange(x.location().orElse(Location.EMPTY)), DocumentHighlightKind.Text))
                    .map(List::of)
                    .orElse(List.of());
        });
    }

    private Range locationToRange(Location location) {
        return new Range(new Position(location.startLine() - 1, location.startColumn()), new Position(location.endLine() - 1, location.endColumn() + 1));
    }

    private Location positionToLocation(Position position) {
        return new Location(position.getLine() + 1, position.getCharacter(), position.getLine() + 1, position.getCharacter());
    }

    private Optional<Node> findMostSpecificNode(List<Node> possibleMatches) {
        return possibleMatches.reversed().stream().findFirst();
    }

    private List<Node> findPossibleMatches(Node node, Location position) {
        if (node == null)
            return List.of();

        return Stream.concat(Stream.of(node), node.visitChildren())
                .filter(child -> position.isInside(child.location().orElse(Location.EMPTY)))
                .toList();
    }

}
