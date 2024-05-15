package dev.thihup.jvisualg.ide;

import dev.thihup.jvisualg.interpreter.IO;
import dev.thihup.jvisualg.interpreter.InputRequestValue;
import dev.thihup.jvisualg.interpreter.InputValue;
import dev.thihup.jvisualg.interpreter.Interpreter;
import dev.thihup.jvisualg.lsp.VisualgLauncher;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
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
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends Application {

    private final ExecutorService fxClientExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @FXML
    private CodeArea codeArea;

    @FXML
    private Button runButton;

    @FXML
    private TextArea outputArea;

    @FXML
    private TableView<DebugState> debugArea;

    private Subscription subscribe;
    private Launcher<LanguageServer> clientLauncher;
    private Future<Void> lspServer;
    private Future<Void> lspClient;
    private List<Diagnostic> diagnostics;
    private Interpreter interpreter;
    private List<Integer> breakpointLines = new ArrayList<>();

    public record DebugState(String getEscopo, String getNome, String getTipo, String getValor){}

    private int previousLine = -1;

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


        interpreter = new Interpreter(new IO(this::readVariable, this::appendOutput), programState -> {
            Platform.runLater(() -> {
                int lineNumber = programState.lineNumber();
                codeArea.showParagraphAtCenter(lineNumber);
                codeArea.getCaretSelectionBind().moveTo(lineNumber, 0);
                if (previousLine != -1) {
                    codeArea.setStyle(previousLine, List.of());
                }
                previousLine = lineNumber;
                codeArea.setStyle(lineNumber, List.of("debug"));

                debugArea.getItems().clear();
                programState.stack().entrySet().stream()
                        .mapMulti((Map.Entry<String, Map<String, Object>> value, Consumer<DebugState> consumer) -> {
                            value.getValue().forEach((variableName, variableValue) -> {
                                final String scopeName = value.getKey();
                                switch (variableValue) {
                                    case Object[][] multiObjects -> addMultiArrayDebug(scopeName.toUpperCase(), variableName.toUpperCase(), consumer, multiObjects);
                                    case Object[] objects -> addArrayDebug(scopeName.toUpperCase(), variableName.toUpperCase(), consumer, objects);
                                    case Object _ -> consumer.accept(new DebugState(scopeName.toUpperCase(), variableName.toUpperCase(), variableValue.getClass().getSimpleName(), variableValue.toString()));
                                }
                            });
                        })
                        .forEach(x -> debugArea.getItems().add(x));

            });
        });

        runButton.addEventHandler(ActionEvent.ACTION, _ -> {
            Platform.runLater(() -> {
                if (interpreter.state() == Interpreter.State.PAUSED) {
                    if (previousLine != -1) {
                        codeArea.setStyle(previousLine, List.of());
                    }
                    interpreter.continueExecution();
                    return;
                }
                outputArea.clear();
                debugArea.getItems().clear();
                outputArea.appendText("Início da execução\n");
                interpreter.reset();
                previousLine = 0;
                breakpointLines.forEach(interpreter::addBreakpoint);

                interpreter.run(codeArea.getText(), executor)
                    .thenRun(() -> appendOutput("\nFim da execução."))
                    .exceptionally(e -> {
                        e.printStackTrace();
                        appendOutput(e.getCause().toString());
                        appendOutput("\nExecução terminada por erro.");
                        return null;
                    }).whenComplete((_, _) -> {
                        Platform.runLater(() -> {
                            if (previousLine != -1) {
                                codeArea.setStyle(previousLine, List.of());
                            }
                            previousLine = -1;
                        });
                    });

            });
        });


        setupErrorPopup();

        if (Boolean.getBoolean("autoClose"))
            Platform.runLater(stage::close);
    }

    private static void addArrayDebug(String scope, String variableName, Consumer<DebugState> consumer, Object[] objects) {
        for (int i = 0; i < objects.length; i++) {
            Object object = objects[i];
            consumer.accept(new DebugState(scope, variableName + "["+i+"]", object.getClass().getSimpleName(), object.toString()));
        }
    }

    private static void addMultiArrayDebug(String scope, String variableName, Consumer<DebugState> consumer, Object[][] multiObjects) {
        for (int i = 0; i < multiObjects.length; i++) {
            Object[] objects = multiObjects[i];
            for (int j = 0; j < objects.length; j++) {
                Object object = objects[j];
                consumer.accept(new DebugState(scope, variableName + "["+i+", "+j+"]", object.getClass().getSimpleName(), object.toString()));
            }
        }
    }

    private void appendOutput(String output) {
        Platform.runLater(() -> outputArea.appendText(output));
    }

    private CompletableFuture<InputValue> readVariable(InputRequestValue request) {
        CompletableFuture<InputValue> inputValueCompletableFuture = new CompletableFuture<>();

        Platform.runLater(() -> {
            TextInputDialog textInputDialog = new TextInputDialog();
            textInputDialog.setContentText("Digite um valor " + request.type() + "  para a variável " + request.variableName());
            textInputDialog.showAndWait()
                    .flatMap(textValue -> {
                        try {
                            return Optional.<InputValue>of(switch (request.type()) {
                                case CARACTER -> new InputValue.CaracterValue(textValue);
                                case LOGICO -> new InputValue.LogicoValue(Boolean.parseBoolean(textValue));
                                case REAL -> new InputValue.RealValue(Double.parseDouble(textValue));
                                case INTEIRO -> new InputValue.InteiroValue(Integer.parseInt(textValue));
                            });
                        } catch (Exception e) {
                            return Optional.empty();
                        }
                    }).ifPresentOrElse(inputValueCompletableFuture::complete,
                            () -> inputValueCompletableFuture.complete(null));
        });

        return inputValueCompletableFuture;
    }

    private void showScene(Stage stage, Parent root) {
        Scene scene = new Scene(root);
        scene.setOnKeyPressed(x -> {
            switch (x.getCode()) {
                case F9 -> runButton.fire();
                case F8 -> {
                    switch (interpreter.state()) {
                        case PAUSED -> {
                            interpreter.step();
                        }
                        case STOPPED -> {
                            breakpointLines.addLast(1);
                            runButton.fire();
                        }
                        case RUNNING  -> {
                        }
                    }
                }
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
        Pipe clientToServerPipe = Pipe.open();
        Pipe serverToClientPipe = Pipe.open();

        lspServer = VisualgLauncher.startServer(Channels.newInputStream(clientToServerPipe.source()), Channels.newOutputStream(serverToClientPipe.sink()), executor);
        clientLauncher = LSPLauncher.createClientLauncher(new VisualgLanguageClient(), Channels.newInputStream(serverToClientPipe.source()), Channels.newOutputStream(clientToServerPipe.sink()), executor, null);

        lspClient = clientLauncher.startListening();
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
        fxClientExecutor.shutdown();
        executor.shutdown();
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
    private static final String BOOLEAN_PATTERN = "(?i)\\b(VERDADEIRO|FALSO)\\b";
    private static final String COMMENT_PATTERN = "//[^\\n\\r]+?(?:\\*\\)|[\\n\\r])";
    private static final String NUMBER_PATTERN = "(?<![a-zA-Z])\\d+";

    private static final Pattern PATTERN = Pattern.compile(

            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
                    + "|(?<BOOLEAN>" + BOOLEAN_PATTERN + ")"
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
                case Matcher _ when matcher.group("BOOLEAN") != null -> List.of("number");
                default -> throw new IllegalStateException("Unexpected value: " + matcher);

            };

            spansBuilder.add(List.of(), matcher.start() - lastKwEnd);
            spansBuilder.add(style, matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(List.of(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
