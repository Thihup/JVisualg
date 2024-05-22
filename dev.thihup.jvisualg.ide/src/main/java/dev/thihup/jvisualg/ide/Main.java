package dev.thihup.jvisualg.ide;

import dev.thihup.jvisualg.interpreter.*;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
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
import org.reactfx.Subscription;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

public class Main extends Application {

    public static final String DEFAULT_CONSOLE_STYLE = """
                    -fx-control-inner-background: black;
                    -fx-text-fill: white;
                    -fx-font-family: "Consolas";
                    -fx-font-size: 12;
            """;
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


    private Consumer<String> callback;

    private TextArea dosContent;
    private Stage dosWindow;

    private Subscription subscribe;
    private Launcher<LanguageServer> clientLauncher;
    private Future<Void> lspServer;
    private Future<Void> lspClient;
    private List<Diagnostic> diagnostics;
    private Interpreter interpreter;
    private List<Integer> breakpointLines = new ArrayList<>();
    private int lastPromptPosition = 0;

    private void handleExecutionSuccessfully() {
        appendOutput("\nFim da execução.", ToWhere.OUTPUT, When.LATER);
        appendOutput("\n>>> Fim da execução do programa !", ToWhere.DOS, When.LATER);
    }

    public record DebugState(String getEscopo, String getNome, String getTipo, String getValor) {
    }

    private int previousDebugLine = -1;

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

        dosWindow = new Stage();
        dosContent = new TextArea();

        dosContent.setStyle(DEFAULT_CONSOLE_STYLE);
        dosWindow.setTitle("Emulação do DOS");
        dosWindow.setScene(new Scene(dosContent));
        dosContent.setEditable(false);
        dosContent.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
        dosContent.addEventFilter(MouseEvent.MOUSE_CLICKED, this::handleMouseClick);

        interpreter = new Interpreter(new IO(this::readVariable, this::handleOutputEvent), programState -> {
            Platform.runLater(() -> {
                updateDebugArea(programState);
                highlightCurrentLine(programState.lineNumber());
            });
        });

        runButton.addEventHandler(ActionEvent.ACTION, _ -> {
            Platform.runLater(() -> {
                switch (interpreter.state()) {
                    case Interpreter.State.Running _ -> {
                    }
                    case Interpreter.State.PausedDebug _ -> interpreter.continueExecution();
                    case Interpreter.State.NotStarted _ -> {
                        resetExecution();

                        interpreter.run(codeArea.getText(), executor)
                                .thenRun(this::handleExecutionSuccessfully)
                                .exceptionally(this::handleExecutionError)
                                .whenComplete((_, _) -> Platform.runLater(this::removeDebugStyleFromPreviousLine));
                    }
                    case Interpreter.State.CompletedExceptionally _, Interpreter.State.CompletedSuccessfully _, Interpreter.State.ForcedStop _ -> {
                    }
                }
            });
        });


        setupErrorPopup();

        if (Boolean.getBoolean("autoClose"))
            Platform.runLater(stage::close);
    }

    private Void handleExecutionError(Throwable e) {
        if (e.getCause() instanceof CancellationException) {
            appendOutput("\n*** Execução terminada pelo usuário.", ToWhere.DOS, When.LATER);
            appendOutput("\n*** Feche esta janela para retornar ao Visual.", ToWhere.DOS, When.LATER);

            appendOutput("\nExecução terminada pelo usuário.", ToWhere.OUTPUT, When.LATER);
            return null;
        } else if (e.getCause() instanceof TypeException) {
            appendOutput(e.getCause().getMessage(), ToWhere.OUTPUT, When.LATER);
        } else if (e.getCause() != null) {
            e.printStackTrace();
            appendOutput(e.getCause().toString(), ToWhere.OUTPUT, When.LATER);
        }

        appendOutput("\nExecução terminada por erro.", ToWhere.BOTH, When.LATER);
        appendOutput("\n>>> Fim da execução do programa !", ToWhere.DOS, When.LATER);
        return null;
    }

    private void resetExecution() {
        outputArea.clear();
        debugArea.getItems().clear();
        appendOutput("Início da execução\n", ToWhere.OUTPUT, When.NOW);
        interpreter.reset();
        previousDebugLine = -1;
        breakpointLines.forEach(interpreter::addBreakpoint);

        dosContent.setStyle(DEFAULT_CONSOLE_STYLE);
        dosContent.clear();
        dosWindow.show();
    }

    private void updateDebugArea(Interpreter.ProgramState programState) {
        debugArea.getItems().clear();
        programState.stack().entrySet().stream()
                .mapMulti((Map.Entry<String, Map<String, Object>> entry, Consumer<DebugState> consumer) -> {
                    entry.getValue().forEach((variableName, variableValue) -> {
                        final String scopeName = entry.getKey().toUpperCase();
                        final String variableNameUpperCase = variableName.toUpperCase();
                        addDebug(consumer, variableValue, scopeName, variableNameUpperCase);
                    });
                })
                .forEach(debugArea.getItems()::add);
    }

    private static void addDebug(Consumer<DebugState> consumer, Object variableValue, String scopeName, String variableNameUpperCase) {
        switch (variableValue) {
            case Object[][] multiObjects ->
                    addMultiArrayDebug(scopeName, variableNameUpperCase, consumer, multiObjects);
            case Object[] objects -> addArrayDebug(scopeName, variableNameUpperCase, consumer, objects);
            case Object _ -> addObjectDebug(scopeName, variableNameUpperCase, consumer, variableValue);
        }
    }

    private void highlightCurrentLine(int lineNumber) {
        if (lineNumber < 0) {
            return;
        }
        if (previousDebugLine != -1) {
            removeDebugStyleFromPreviousLine();
        }
        codeArea.showParagraphAtCenter(lineNumber);
        codeArea.getCaretSelectionBind().moveTo(lineNumber, 0);

        codeArea.setParagraphStyle(lineNumber, List.of("debug"));
        previousDebugLine = lineNumber;
    }

    private void removeDebugStyleFromPreviousLine() {
        codeArea.setParagraphStyle(previousDebugLine, List.of());
    }

    private static void addObjectDebug(String scope, String variableName, Consumer<DebugState> consumer, Object variableValue) {
        String visualgType = getVisualgType(variableValue);
        switch (variableValue) {
            case Boolean b ->
                    consumer.accept(new DebugState(scope, variableName, visualgType, b ? "VERDADEIRO" : "FALSO"));
            case String s -> consumer.accept(new DebugState(scope, variableName, visualgType, "\"" + s + "\""));
            case Integer _, Double _ ->
                    consumer.accept(new DebugState(scope, variableName, visualgType, variableValue.toString()));
            case UserDefinedValue userDefinedValue -> {
                userDefinedValue.values().forEach((fieldName, fieldValue) -> addDebug(consumer, fieldValue, scope, variableName + "." + fieldName));
            }
            default -> {
            }
        }
    }

    private static String getVisualgType(Object object) {
        return switch (object) {
            case Boolean _ -> "LOGICO";
            case String _ -> "CARACTER";
            case Double _ -> "REAL";
            case Integer _ -> "INTEIRO";
            default -> "Desconhecido";
        };
    }

    private static void addArrayDebug(String scope, String variableName, Consumer<DebugState> consumer, Object[] objects) {
        for (int i = 0; i < objects.length; i++) {
            Object object = objects[i];
            addObjectDebug(scope, variableName + "[" + i + "]", consumer, object);
        }
    }

    private static void addMultiArrayDebug(String scope, String variableName, Consumer<DebugState> consumer, Object[][] multiObjects) {
        for (int i = 0; i < multiObjects.length; i++) {
            Object[] objects = multiObjects[i];
            for (int j = 0; j < objects.length; j++) {
                Object object = objects[j];
                addObjectDebug(scope, variableName + "[" + i + ", " + j + "]", consumer, object);
            }
        }
    }

    enum ToWhere {
        DOS, OUTPUT, BOTH
    }

    enum When {
        NOW, LATER
    }


    private void appendOutput(String output, ToWhere where, When when) {
        Runnable runnable = () -> {
            switch (where) {
                case DOS -> dosContent.appendText(output);
                case OUTPUT -> outputArea.appendText(output);
                case BOTH -> {
                    dosContent.appendText(output);
                    outputArea.appendText(output);
                }
            }
        };

        switch (when) {
            case NOW -> runnable.run();
            case LATER -> Platform.runLater(runnable);
        }
    }

    private void handleOutputEvent(OutputEvent outputEvent) {
        Platform.runLater(() -> {
            switch (outputEvent) {
                case OutputEvent.Text text -> appendOutput(text.text(), ToWhere.BOTH, When.NOW);
                case OutputEvent.Clear _ -> dosContent.clear();
                case OutputEvent.ChangeColor(
                        OutputEvent.ChangeColor.Color color, OutputEvent.ChangeColor.Position position
                ) -> {
                    switch (position) {
                        case FOREGROUND ->
                                dosContent.setStyle(dosContent.getStyle() + ";-fx-text-fill:" + color.toString().toLowerCase() + ";");
                        case BACKGROUND ->
                                dosContent.setStyle(dosContent.getStyle() + ";-fx-control-inner-background:" + color.toString().toLowerCase() + ";");
                    }
                }
                case null, default -> {
                }
            }
        });
    }

    private void handleKeyPress(KeyEvent event) {
        TextArea textArea = (TextArea) event.getSource();
        switch (event.getCode()) {
            case ENTER -> {
                int currentCaretPosition = textArea.getCaretPosition();
                String fullText = textArea.getText();
                if (lastPromptPosition < currentCaretPosition) {
                    String newCommand = fullText.substring(lastPromptPosition, currentCaretPosition).trim();
                    if (callback != null) callback.accept(newCommand);
                }
                textArea.positionCaret(lastPromptPosition);
                event.consume();
            }
            case KeyCode.BACK_SPACE, KeyCode.DELETE, KeyCode.LEFT, KeyCode.HOME -> {
                if (textArea.getCaretPosition() <= lastPromptPosition) {
                    event.consume();
                }
            }
            case RIGHT, KeyCode.END -> {
                if (textArea.getCaretPosition() > lastPromptPosition) {
                    textArea.positionCaret(lastPromptPosition);
                    event.consume();
                }
            }
            case KeyCode.UP, KeyCode.DOWN -> event.consume();

            case KeyCode.A -> {
                if (event.isControlDown()) {
                    event.consume();
                }
            }
            case KeyCode.ESCAPE -> {
                event.consume();
                dosContent.clear();
                dosWindow.hide();
            }
            case KeyCode.F2 -> {
                if (event.isControlDown() && interpreter != null) {
                    interpreter.stop();
                }
            }
            default -> {
            }
        }
    }

    private void handleMouseClick(MouseEvent event) {
        TextArea textArea = (TextArea) event.getSource();
        if (textArea.getCaretPosition() < lastPromptPosition) {
            textArea.positionCaret(textArea.getText().length());
            event.consume();
        }
    }

    private CompletableFuture<InputValue> readVariable(InputRequestValue request) {
        CompletableFuture<InputValue> inputValueCompletableFuture = new CompletableFuture<>();

        Platform.runLater(() -> {
            if (dosWindow.isShowing()) {
                dosContent.setEditable(true);
                lastPromptPosition = dosContent.getText().length();
                callback = strip -> {
                    strip = strip.strip();
                    getValue(request, strip)
                            .ifPresentOrElse(inputValueCompletableFuture::complete,
                                    () -> inputValueCompletableFuture.complete(null));
                };
                return;
            }
            TextInputDialog textInputDialog = new TextInputDialog();
            textInputDialog.setContentText("Digite um valor " + request.type() + "  para a variável " + request.variableName());
            textInputDialog.showAndWait()
                    .flatMap(textValue -> getValue(request, textValue))
                    .ifPresentOrElse(inputValueCompletableFuture::complete,
                            () -> inputValueCompletableFuture.complete(null));
        });

        return inputValueCompletableFuture;
    }

    private static Optional<InputValue> getValue(InputRequestValue request, String textValue) {
        try {
            return Optional.of(switch (request.type()) {
                case CARACTER -> new InputValue.CaracterValue(textValue);
                case LOGICO -> new InputValue.LogicoValue(textValue.equalsIgnoreCase("VERDADEIRO"));
                case REAL -> new InputValue.RealValue(Double.parseDouble(textValue));
                case INTEIRO -> new InputValue.InteiroValue(Integer.parseInt(textValue));
            });
        } catch (Exception _) {
            return Optional.empty();
        }
    }

    private void showScene(Stage stage, Parent root) {
        Scene scene = new Scene(root);
        scene.setOnKeyPressed(x -> {
            switch (x.getCode()) {
                case F2 -> {
                    if (x.isControlDown() && interpreter != null) {
                        interpreter.stop();
                    }
                }
                case F9 -> runButton.fire();
                case F8 -> {
                    switch (interpreter.state()) {
                        case Interpreter.State.PausedDebug _ -> {
                            interpreter.step();
                        }
                        case Interpreter.State.NotStarted _, Interpreter.State.ForcedStop _, Interpreter.State.CompletedExceptionally _, Interpreter.State.CompletedSuccessfully _ -> {
                            breakpointLines.addLast(1);
                            runButton.fire();
                        }
                        case Interpreter.State.Running _ -> {
                        }
                    }
                }
                case ESCAPE -> {
                    switch (interpreter.state()) {
                        case Interpreter.State.ForcedStop _, Interpreter.State.CompletedExceptionally _, Interpreter.State.CompletedSuccessfully _ -> dosWindow.hide();
                        case Interpreter.State.PausedDebug _, Interpreter.State.NotStarted _, Interpreter.State.Running _ -> {}
                    }
                }
                default -> {
                }
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


    private final LongAdder counter = new LongAdder();

    private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
        String text = codeArea.getText();
        Task<StyleSpans<Collection<String>>> task = new Task<>() {
            @Override
            protected StyleSpans<Collection<String>> call() throws Exception {
                VersionedTextDocumentIdentifier versionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier("untitled://file", counter.intValue());
                DidChangeTextDocumentParams didChangeTextDocumentParams = new DidChangeTextDocumentParams(versionedTextDocumentIdentifier, Collections.singletonList(new TextDocumentContentChangeEvent(text)));
                clientLauncher.getRemoteProxy().getTextDocumentService().didChange(didChangeTextDocumentParams);
                return SyntaxHighlight.computeHighlighting(text);
            }
        };
        fxClientExecutor.execute(task);
        return task;
    }

    private void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
        codeArea.setStyleSpans(0, highlighting);
    }


}
