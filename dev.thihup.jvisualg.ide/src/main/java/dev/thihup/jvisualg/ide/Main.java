package dev.thihup.jvisualg.ide;

import dev.thihup.jvisualg.interpreter.DAPServer;
import dev.thihup.jvisualg.interpreter.DebugProtocolClientExtension;
import dev.thihup.jvisualg.lsp.VisualgLauncher;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.eclipse.lsp4j.debug.OutputEventArguments;
import org.eclipse.lsp4j.debug.SetBreakpointsArguments;
import org.eclipse.lsp4j.debug.SourceBreakpoint;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.debug.DebugLauncher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.event.MouseOverTextEvent;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends Application {

    private final ExecutorService fxClientExecutor = Executors.newSingleThreadExecutor();

    private final ExecutorService lspClientExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService lspServerExecutor = Executors.newSingleThreadExecutor();

    private final ExecutorService dapClientExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService dapServerExecutor = Executors.newSingleThreadExecutor();


    @FXML
    private CodeArea codeArea;

    @FXML
    private Button runButton;

    @FXML
    private TextArea outputArea;

    private Subscription subscribe;
    private Launcher<LanguageServer> clientLauncher;
    private Future<Void> lspServer;
    private Future<Void> lspClient;
    private Launcher<DebugProtocolClientExtension> dapServerLauncher;
    private Launcher<IDebugProtocolServer> dapClientLauncher;
    private Future<Void> dapServerListener;
    private Future<Void> dapClientListener;
    private List<Diagnostic> diagnostics;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("gui.fxml"));
        fxmlLoader.setController(this);
        Parent root = fxmlLoader.load();

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        setupSyntaxHighlighting();

        setupDefaultText();

        showScene(stage, root);

        setupLSP();

        setupDAP();

        runButton.addEventHandler(javafx.event.ActionEvent.ACTION, e -> {
            Platform.runLater(() -> {
                outputArea.clear();
                dapClientLauncher.getRemoteProxy().launch(Map.of("source", codeArea.getText()));
            });
        });


        setupErrorPopup();

        if (Boolean.getBoolean("autoClose"))
            Platform.runLater(stage::close);
    }

    private void showScene(Stage stage, Parent root) {
        Scene scene = new Scene(root);
        scene.setOnKeyPressed(x -> {
            switch (x.getCode()) {
                case F9 -> runButton.fire();
                default -> {}
            }
        });
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        stage.setTitle("JVisualG");
        stage.setScene(scene);
        stage.show();
    }

    private void setupDefaultText() {
        codeArea.replaceText(0, 0, """
                algoritmo "semnome"
                // Função :
                // Autor :
                // Data : %s
                // Seção de Declarações
                var

                inicio
                // Seção de Comandos
                fimalgoritmo
                """.formatted(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
    }

    private void setupSyntaxHighlighting() {
        subscribe = codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(50))
                .retainLatestUntilLater(fxClientExecutor)
                .supplyTask(this::computeHighlightingAsync)
                .awaitLatest(codeArea.multiPlainChanges())
                .filterMap(t -> {
                    if (t.isSuccess()) {
                        return Optional.of(t.get());
                    } else {
                        t.getFailure().printStackTrace();
                        return Optional.empty();
                    }
                })
                .subscribe(this::applyHighlighting);
    }

    private void setupLSP() throws IOException {
        PipedInputStream lspInputClient = new PipedInputStream();
        PipedOutputStream lspOutputClient = new PipedOutputStream();
        PipedInputStream lspInputServer = new PipedInputStream();
        PipedOutputStream lspOutputServer = new PipedOutputStream();

        lspInputClient.connect(lspOutputServer);
        lspOutputClient.connect(lspInputServer);

        lspServer = VisualgLauncher.startServer(lspInputServer, lspOutputServer, lspServerExecutor);
        clientLauncher = LSPLauncher.createClientLauncher(new VisualgLanguageClient(), lspInputClient, lspOutputClient, lspClientExecutor, null);

        lspClient = clientLauncher.startListening();
    }

    private void setupDAP() throws IOException {
        PipedInputStream dapInputClient = new PipedInputStream();
        PipedOutputStream dapOutputClient = new PipedOutputStream();
        PipedInputStream dapInputServer  = new PipedInputStream();
        PipedOutputStream dapOutputServer = new PipedOutputStream();

        dapInputClient.connect(dapOutputServer);
        dapOutputClient.connect(dapInputServer);

        DAPServer server = new DAPServer();
        dapServerLauncher = DebugLauncher.createLauncher(server, DebugProtocolClientExtension.class, dapInputServer, dapOutputServer, dapServerExecutor, null);
        server.connect(dapServerLauncher.getRemoteProxy());
        dapServerListener = dapServerLauncher.startListening();


        dapClientLauncher = DSPLauncher.createClientLauncher(new DAPClient(), dapInputClient, dapOutputClient, dapClientExecutor, null);
        dapClientListener = dapClientLauncher.startListening();
        dapClientLauncher.getRemoteProxy().initialize(new InitializeRequestArguments());
    }

    private void setupErrorPopup() {
        Popup popup = new Popup();
        Label popupMsg = new Label();
        popupMsg.setStyle(
                "-fx-background-color: black;" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 5;");
        popup.getContent().add(popupMsg);

        codeArea.setMouseOverTextDelay(Duration.ofMillis(200));
        codeArea.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN, e -> {
            handlePopupError(e, popupMsg, popup);
        });
        codeArea.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_END, e -> {
            popup.hide();
        });
    }

    private void handlePopupError(MouseOverTextEvent e, Label popupMsg, Popup popup) {
        int chIdx = e.getCharacterIndex();
        Point2D pos = e.getScreenPosition();
        if (codeArea.getText().isEmpty() || diagnostics == null)
            return;
        diagnostics.stream()
                .filter(diagnostic -> {
                    int start = toOffset(codeArea.getText(), diagnostic.getRange().getStart());
                    int end = toOffset(codeArea.getText(), diagnostic.getRange().getEnd());
                    return chIdx >= start && chIdx <= end;
                })
                .findFirst().ifPresent(diagnostic -> {
                    popupMsg.setText(diagnostic.getMessage());
                    popup.show(codeArea, pos.getX(), pos.getY() + 10);
                });
    }


    class VisualgLanguageClient implements LanguageClient {


        @Override
        public void telemetryEvent(Object object) {

        }

        @Override
        public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
            Main.this.diagnostics = diagnostics.getDiagnostics();
            diagnostics.getDiagnostics().forEach(diagnostic -> {
                System.out.println(diagnostic.getMessage());
                Platform.runLater(() -> {
                    if (codeArea.getText().isEmpty())
                        return;
                    int offsetStart = toOffset(codeArea.getText(), diagnostic.getRange().getStart());
                    int offsetEnd = toOffset(codeArea.getText(), diagnostic.getRange().getEnd()) + 1;
                    codeArea.setStyleClass(offsetStart, offsetEnd, "error");
                });

            });
        }

        @Override
        public void showMessage(MessageParams messageParams) {

        }

        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
            return null;
        }

        @Override
        public void logMessage(MessageParams message) {
            System.out.println(message.getMessage());
        }
    }

    @Override
    public void stop() throws Exception {
        if (subscribe != null) {
            subscribe.unsubscribe();
        }
        if (lspClient != null) {
            lspClient.cancel(true);
        }
        if (lspServer != null) {
            lspServer.cancel(true);
        }
        if (dapClientListener != null) {
            dapClientListener.cancel(true);
        }
        if (dapServerListener != null) {
            dapServerListener.cancel(true);
        }

        fxClientExecutor.shutdown();
        lspClientExecutor.shutdown();
        dapClientExecutor.shutdown();
        lspServerExecutor.shutdown();
        dapServerExecutor.shutdown();
    }

    private int toOffset(String text, Position position) {
        String[] lines = text.split("\n");
        int offset = 0;
        for (int i = 0; i < lines.length; i++) {
            if (i == position.getLine()) {
                return offset + position.getCharacter();
            }
            offset += lines[i].length() + 1;
        }
        return 0;
    }


    public static void main(String[] args) {
        launch(args);
    }

    private static final List<String> KEYWORDS = List.of(
            "Div", "Mod", "Se", "Entao", "Então", "Senao", "Senão", "FimSe", "Para", "De", "Ate", "Até", "Passo", "FimPara", "Faca", "Faça", "Enquanto", "FimEnquanto", "Retorne", "E", "Ou", "Nao", "Não", "Escolha", "FimEscolha", "Repita", "FimRepita", "Caso", "OutroCaso",
            "abs", "arccos", "arcsen", "arctan", "asc", "carac", "caracpnum", "compr", "copia", "cos", "cotan", "exp", "grauprad", "int", "log", "logn", "maiusc", "minusc", "numpcarac", "pos", "pi", "quad", "radpgrau", "raizq", "rand", "randi"

    );

    private static final List<String> DATA_TYPES = List.of(
            "Inteiro", "Real", "Logico", "Caracter", "Caractere", "Literal", "Vetor"
    );

    private static final List<String> SPECIAL_KEYWORDS = List.of(
            "Escreva", "Escreval", "Leia", "Algoritmo", "FimAlgoritmo", "Funcao", "Função", "FimFuncao", "FimFunção", "Procedimento", "FimProcedimento", "Inicio", "Var"
    );


    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String KEYWORD_PATTERN = "(?i)\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String SPECIAL_PATTERN = "(?i)\\b(" + String.join("|", SPECIAL_KEYWORDS) + ")\\b";
    private static final String TYPES_PATTERN = "(?i)\\b(" + String.join("|", DATA_TYPES) + ")\\b";
    private static final String COMMENT_PATTERN = "//[^\\n\\r]+?(?:\\*\\)|[\\n\\r])";
    private static final String NUMBER_PATTERN = "(?<![a-zA-Z])\\d+";

    private static final Pattern PATTERN = Pattern.compile(

            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
                    + "|(?<TYPE>" + TYPES_PATTERN + ")"
                    + "|(?<SPECIAL>" + SPECIAL_PATTERN + ")");

    private final LongAdder counter = new LongAdder();

    private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
        String text = codeArea.getText();
        Task<StyleSpans<Collection<String>>> task = new Task<>() {
            @Override
            protected StyleSpans<Collection<String>> call() throws Exception {
                VersionedTextDocumentIdentifier versionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier("untitled://file", counter.intValue());
                DidChangeTextDocumentParams didChangeTextDocumentParams = new DidChangeTextDocumentParams(versionedTextDocumentIdentifier, Collections.singletonList(new TextDocumentContentChangeEvent(text)));
                clientLauncher.getRemoteProxy().getTextDocumentService().didChange(didChangeTextDocumentParams);
                return computeHighlighting(text);
            }
        };
        fxClientExecutor.execute(task);
        return task;
    }

    private void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
        codeArea.setStyleSpans(0, highlighting);
    }

    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            List<String> style = switch (matcher) {

                case Matcher _ when matcher.group("KEYWORD") != null -> List.of("keyword");
                case Matcher _ when matcher.group("STRING") != null -> List.of("string");
                case Matcher _ when matcher.group("COMMENT") != null -> List.of("comment", "italic");
                case Matcher _ when matcher.group("NUMBER") != null -> List.of("number");
                case Matcher _ when matcher.group("TYPE") != null -> List.of("dataType", "underline");
                case Matcher _ when matcher.group("SPECIAL") != null -> List.of("special", "underline");
                default -> throw new IllegalStateException("Unexpected value: " + matcher);

            };

            spansBuilder.add(List.of(), matcher.start() - lastKwEnd);
            spansBuilder.add(style, matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(List.of(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    private class DAPClient implements DebugProtocolClientExtension {
        @Override
        public void output(OutputEventArguments args) {
            Platform.runLater(() -> outputArea.appendText(args.getOutput()));
        }

        @Override
        public CompletableFuture<String> input() {
            return CompletableFuture.completedFuture("SIMPLE TEXT");
        }
    }
}
