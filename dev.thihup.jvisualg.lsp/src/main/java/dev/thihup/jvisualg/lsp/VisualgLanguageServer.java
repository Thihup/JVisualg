package dev.thihup.jvisualg.lsp;

import dev.thihup.jvisualg.frontend.Error;
import dev.thihup.jvisualg.frontend.Main;
import dev.thihup.jvisualg.frontend.TypeChecker;
import dev.thihup.jvisualg.frontend.node.Location;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VisualgLanguageServer implements LanguageServer, LanguageClientAware, WorkspaceService, TextDocumentService {
    private LanguageClient client;


    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams initializeParams) {
        ServerCapabilities capabilities = new ServerCapabilities();

        ServerInfo visualg = new ServerInfo("Visualg", "2.5");
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);

        CompletionOptions completionProvider = new CompletionOptions();
        CompletionItemOptions completionItem = new CompletionItemOptions();
        completionProvider.setCompletionItem(completionItem);

        capabilities.setCompletionProvider(completionProvider);

        DiagnosticRegistrationOptions diagnosticProvider = new DiagnosticRegistrationOptions();
        diagnosticProvider.setInterFileDependencies(false);
        diagnosticProvider.setWorkspaceDiagnostics(false);
        capabilities.setDiagnosticProvider(diagnosticProvider);

        return CompletableFuture.supplyAsync(() -> new InitializeResult(capabilities, visualg));
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
        checkForErrors(params.getTextDocument());
    }

    private void checkForErrors(TextDocumentItem params) {
        try {
            String text = params.getText();

            byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);
            Main.Result astResult = Main.buildAST(new ByteArrayInputStream(bytes));

            List<Error> errors = astResult.errors();

            astResult.node()
                    .map(TypeChecker::semanticAnalysis)
                    .map(Main.Result::errors)
                    .ifPresent(errors::addAll);

            List<Diagnostic> list = errors.stream()
                    .map(s -> {
                        Diagnostic diagnostic = new Diagnostic();
                        diagnostic.setMessage(s.message());
                        diagnostic.setSeverity(DiagnosticSeverity.Error);
                        Location location = s.location();
                        Position startPosition = new Position(location.startLine() - 1, location.startColumn());
                        Position endPosition = new Position(location.endLine() - 1, location.endColumn());
                        diagnostic.setRange(new Range(startPosition, endPosition));
                        return diagnostic;
                    }).toList();

            PublishDiagnosticsParams diagnostics = new PublishDiagnosticsParams(params.getUri(), list);
            client.publishDiagnostics(diagnostics);
        } catch (Throwable e) {
            client.logMessage(new MessageParams(MessageType.Error, e.getMessage()));
        }
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        try {
            checkForErrors(
                    new TextDocumentItem(params.getTextDocument().getUri(), "Visualg", params.getTextDocument().getVersion(),
                            params.getContentChanges().getFirst().getText()));
        } catch (Exception e) {
            client.logMessage(new MessageParams(MessageType.Error, e.getMessage()));
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        try {
            checkForErrors(new TextDocumentItem(params.getTextDocument().getUri(), "Visualg", 0, params.getText()));
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

        return CompletableFuture.supplyAsync(() -> Either.forLeft(defaultCompletionList()));
    }

    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(SignatureHelpParams params) {
        return TextDocumentService.super.signatureHelp(params);
    }

    @Override
    public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams params) {
        DocumentHighlight e1 = new DocumentHighlight();
        e1.setKind(Math.random() < 0.5 ? DocumentHighlightKind.Text : DocumentHighlightKind.Read);
        e1.setRange(new Range(new Position(0, 0), new Position(5, 5)));
        return CompletableFuture.completedFuture(List.of(e1));

    }

    private List<CompletionItem> defaultCompletionList() {

        class Holder {
            static final List<CompletionItem> HOLDER = Stream.of(defaultFunctions(), keywords(), snippets()).flatMap(s -> s).toList();

            private static Stream<CompletionItem> defaultFunctions() {
                return TypeChecker.DEFAULT_GLOBAL_SCOPE.functions()
                        .entrySet()
                        .stream()
                        .map(Holder::functionCompletionItem);
            }

            private static CompletionItem functionCompletionItem(Map.Entry<String, TypeChecker.Declaration.Function> func) {
                CompletionItem item = new CompletionItem();
                item.setLabel(func.getKey());
                item.setKind(CompletionItemKind.Function);
                TypeChecker.Declaration.Function value = func.getValue();

                item.setDetail("(" + value.parameters().values().stream().map(x -> x.name() + ": " + x.type()).collect(Collectors.joining(",")) + "): " + value.returnType());
                return item;
            }

            private static Stream<CompletionItem> snippets() {
                return Map.ofEntries(
                        Map.entry("!", """
                                algoritmo "${0:semnome}"
                                inicio
                                    $1
                                fimalgoritmo
                                """),
                        Map.entry("#", """
                                // Algoritmo : $0
                                // Função : $1
                                // Autor : $2
                                // Data : ${3}
                                """),
                        Map.entry("ale", "aleatorio"),
                        Map.entry("aof", "aleatorio off"),
                        Map.entry("aon", "aleatorio on"),
                        Map.entry("arq", "arquivo"),
                        Map.entry("cof", "cronometro off"),
                        Map.entry("con", "cronometro on"),
                        Map.entry("dcc", "var $0 :caractere"),
                        Map.entry("dcl", "var $0 :logico"),
                        Map.entry("dcr", "var $0 :real"),
                        Map.entry("deb", "debug"),
                        Map.entry("eof", "eco off"),
                        Map.entry("eon", "eco on"),
                        Map.entry("esc", "escreva"),
                        Map.entry("escl", """
                                escolha $0 faca
                                caso
                                    $1
                                fimescolha"""),
                        Map.entry("esco", """
                                escolha $0 faca
                                caso $1
                                    $2
                                outrocaso
                                    $3
                                fimescolha"""),
                        Map.entry("enq", """
                                enquanto $0 faca
                                    $1
                                fimenquanto"""),
                        Map.entry("fal", "fimalgoritmo"),
                        Map.entry("ini", "inicio"),
                        Map.entry("int", "interrompa"),
                        Map.entry("lep", "leia"),
                        Map.entry("par", """
                                para $0 de ${1:1} ate $2 faca
                                    $3
                                fimpara"""),
                        Map.entry("parp", """
                                para $0 de ${1:1} ate $2 passo $3 faca
                                    $4
                                fimpara"""),
                        Map.entry("rep", """
                                repita
                                    $0
                                ate $1"""),
                        Map.entry("repf", """
                                repita
                                    $0
                                fimrepita"""),
                        Map.entry("see", """
                                se $0 entao
                                    $1
                                fimse"""),
                        Map.entry("ses", """
                                se $0 entao
                                    $1
                                senao
                                    $2
                                fimse"""),
                        Map.entry("tim", """
                                timer on
                                $0
                                timer off"""),
                        Map.entry("tof", "timer off"),
                        Map.entry("ton", "timer on")


                ).entrySet()
                .stream()
                .map(Holder::snippetCompletionItem);
            }

            private static CompletionItem snippetCompletionItem(Map.Entry<String, String> entry) {
                CompletionItem completionItem = new CompletionItem();
                completionItem.setKind(CompletionItemKind.Snippet);
                completionItem.setLabel(entry.getKey());
                completionItem.setInsertText(entry.getValue());
                completionItem.setInsertTextFormat(InsertTextFormat.Snippet);
                completionItem.setSortText("ZZZZZZZZZZZZZZZZZZZZ" + entry.getKey());
                return completionItem;
            }


            static Stream<CompletionItem> keywords() {
                return Map.<String, String>ofEntries(
                                Map.entry("aleatorio", "on|off|<menor valor>, <maior valor>"),
                                Map.entry("algoritmo", "<nome do algoritmo>"),
                                Map.entry("arquivo", "<nome do arquivo de dados>"),
                                Map.entry("caracter", ""),
                                Map.entry("caso", "<lista de expressoes>"),
                                Map.entry("const", ""),
                                Map.entry("cronometro", "on|off"),
                                Map.entry("de", ""),
                                Map.entry("debug", "<expressao logica>"),
                                Map.entry("div", ""),
                                Map.entry("dos", ""),
                                Map.entry("e", ""),
                                Map.entry("eco", "on|off"),
                                Map.entry("enquanto", "<expressao logica> faca"),
                                Map.entry("entao", ""),
                                Map.entry("escolha", "<expressao de selecao>"),
                                Map.entry("escreva", "<lista de expressoes>"),
                                Map.entry("escreval", "<lista de expressoes>"),
                                Map.entry("faca", ""),
                                Map.entry("falso", ""),
                                Map.entry("fimprocedimento", ""),
                                Map.entry("fimse", ""),
                                Map.entry("fimfuncao", ""),
                                Map.entry("fimenquanto", ""),
                                Map.entry("fimalgoritmo", ""),
                                Map.entry("fimescolha", ""),
                                Map.entry("fimpara", ""),
                                Map.entry("fimrepita", ""),

                                Map.entry("funcao", "<nome> (parametros) : tipo de retorno"),
                                Map.entry("inicio", ""),
                                Map.entry("inteiro", ""),
                                Map.entry("interrompa", ""),

                                Map.entry("leia", "(var1, var2, ..., varN)"),
                                Map.entry("limpatela", ""),
                                Map.entry("lista", "<nome da lista>"),
                                Map.entry("literal", ""),
                                Map.entry("logico", ""),
                                Map.entry("mod", ""),
                                Map.entry("nao", ""),
                                Map.entry("numerico", ""),
                                Map.entry("ou", ""),
                                Map.entry("outrocaso", ""),
                                Map.entry("para", "<variavel> de <inicio> ate <fim> faca"),
                                Map.entry("passo", ""),
                                Map.entry("pausa", ""),
                                Map.entry("procedimento", "<nome> (parametros)"),
                                Map.entry("real", ""),
                                Map.entry("repita", ""),
                                Map.entry("retorne", "<valor de retorno da funcao>"),
                                Map.entry("se", "<expressao logica> entao"),
                                Map.entry("senao", ""),
                                Map.entry("timer", "on|off|intervalo"),
                                Map.entry("var", ""),
                                Map.entry("verdadeiro", ""),
                                Map.entry("vetor", ""))
                        .entrySet()
                        .stream()
                        .map(Holder::keywordCompletionItem);
            }

            private static CompletionItem keywordCompletionItem(Map.Entry<String, String> entry) {
                CompletionItem completionItem = new CompletionItem();
                completionItem.setKind(CompletionItemKind.Keyword);
                completionItem.setLabel(entry.getKey());
                completionItem.setDetail(entry.getValue());
                return completionItem;
            }
        }

        return Holder.HOLDER;
    }
}
