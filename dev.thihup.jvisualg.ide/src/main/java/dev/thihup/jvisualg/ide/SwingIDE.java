package dev.thihup.jvisualg.ide;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.icons.FlatAbstractIcon;
import dev.thihup.jvisualg.frontend.ASTResult;
import dev.thihup.jvisualg.frontend.Error;
import dev.thihup.jvisualg.frontend.TypeCheckerResult;
import dev.thihup.jvisualg.frontend.VisualgParser;
import dev.thihup.jvisualg.interpreter.*;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rtextarea.GutterIconInfo;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SwingIDE extends JFrame {

    private final TextEditorPane textArea;
    private final DefaultTableModel debugTable;

    private final RTextScrollPane scrollPane;
    private final JTextArea outputArea;
    private final JFrame dosWindow = new JFrame();
    private final JTextArea dosContent = new JTextArea();

    private final Interpreter interpreter;
    private final Map<Integer, GutterIconInfo> breakpointLines = new HashMap<>();


    private Consumer<String> callback;
    private int lastPromptPosition = 0;

    public SwingIDE() {
        FlatLightLaf.setup();

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        dosContent.setBackground(Color.BLACK);
        dosContent.setForeground(Color.WHITE);
        dosContent.setFont(Font.getFont(Font.MONOSPACED));
        dosWindow.add(dosContent);
        dosWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                dosWindow.setVisible(false);
            }
        });

        interpreter = new Interpreter(new IO(this::readVariable, this::handleOutputEvent), programState -> {
            SwingUtilities.invokeLater(() -> {
                updateDebugArea(programState);
                highlightCurrentLine(programState.lineNumber());
            });
        });

        textArea = new TextEditorPane();
        resetTextArea();
        textArea.setRows(20);
        textArea.setColumns(60);
        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping("text/x-visualg", "dev.thihup.jvisualg.ide.VisualgTokenMaker");
        textArea.setSyntaxEditingStyle("text/x-visualg");
        textArea.setCodeFoldingEnabled(false);
        textArea.setBracketMatchingEnabled(false);
        textArea.addParser(new TypeChecker());


        InputMap inputMap = textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = textArea.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, KeyEvent.CTRL_DOWN_MASK), "stopInterpreter");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "toggleBreakpoint");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), "startOrContinue");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), "stepDebugger");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "handleEscape");

        actionMap.put("stopInterpreter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (interpreter != null) {
                    interpreter.stop();
                }
            }
        });

        actionMap.put("toggleBreakpoint", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int currentParagraph = textArea.getCaretLineNumber();
                handleBreakpointChange(currentParagraph);
            }
        });

        actionMap.put("startOrContinue", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleStartOrContinue();
            }
        });

        actionMap.put("stepDebugger", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleStep();
            }
        });

        actionMap.put("handleEscape", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switch (interpreter.state()) {
                    case InterpreterState.ForcedStop _, InterpreterState.CompletedExceptionally _,
                         InterpreterState.CompletedSuccessfully _ -> dosWindow.hide();
                    default -> {
                    }
                }
            }
        });


        scrollPane = new RTextScrollPane(textArea);
        scrollPane.setIconRowHeaderEnabled(true);
        scrollPane.setMinimumSize(new Dimension(100, 400));
        setSize(1024, 768);

        setJMenuBar(createMenuBar());

        JToolBar toolBar = createToolbar();
        add(toolBar, BorderLayout.NORTH);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.add(scrollPane);
        mainSplitPane.setDividerLocation(0.8);

        JSplitPane bottomSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        debugTable = new DefaultTableModel(new String[]{"Escopo", "Nome", "Tipo", "Valor"}, 0);
        JTable debugArea = new JTable(debugTable);
        JScrollPane debugScrollPane = new JScrollPane(debugArea);
        bottomSplitPane.setLeftComponent(debugScrollPane);

        outputArea = new JTextArea();
        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        bottomSplitPane.setRightComponent(outputScrollPane);

        mainSplitPane.setBottomComponent(bottomSplitPane);
        add(mainSplitPane, BorderLayout.CENTER);

        setVisible(true);

    }

    private void resetTextArea() {
        textArea.setText(newFileText());
        textArea.discardAllEdits();
        textArea.setDirty(false);
        try {
            textArea.setCaretPosition(textArea.getLineStartOffset(8));
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    private static String newFileText() {
        return """
                algoritmo "semnome"
                // Função :
                // Autor :
                // Data : %s
                // Seção de Declarações
                var
                
                inicio
                
                fimalgoritmo
                """.formatted(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    private JToolBar createToolbar() {
        JToolBar toolBar = new JToolBar();
        String[] toolBarIcons = {"🗋", "📂", "🖬", "🖶", "✂", "🗐", "📋", "▶"};
        for (String icon : toolBarIcons) {
            toolBar.add(new JButton(icon));
        }

        JButton newFileButton = (JButton) toolBar.getComponent(0);
        newFileButton.addActionListener(_ -> resetTextArea());


        JButton openFileButton = (JButton) toolBar.getComponent(1);
        openFileButton.addActionListener(_ -> openFile());

        JButton saveFileButton = (JButton) toolBar.getComponent(2);
        saveFileButton.addActionListener(_ -> {
            try {
                textArea.save();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        JButton printButton = (JButton) toolBar.getComponent(3);
        printButton.addActionListener(_ -> {
            try {
                textArea.print();
            } catch (PrinterException e) {
                throw new RuntimeException(e);
            }
        });

        JButton cutButton = (JButton) toolBar.getComponent(4);
        cutButton.addActionListener(RTextArea.getAction(RTextArea.CUT_ACTION));

        JButton copyButton = (JButton) toolBar.getComponent(5);
        copyButton.addActionListener(RTextArea.getAction(RTextArea.COPY_ACTION));


        JButton pasteButton = (JButton) toolBar.getComponent(6);
        pasteButton.addActionListener(RTextArea.getAction(RTextArea.PASTE_ACTION));

        JButton runButton = (JButton) toolBar.getComponent(7);
        runButton.addActionListener(_ -> handleStartOrContinue());
        return toolBar;
    }

    private void highlightCurrentLine(int i) {
        try {
            if (i == 0) {
                textArea.setCaretPosition(textArea.getLineEndOffsetOfCurrentLine());
                textArea.removeAllLineHighlights();
                textArea.setHighlightCurrentLine(true);
                return;
            }
            textArea.removeAllLineHighlights();
            textArea.setHighlightCurrentLine(false);
            textArea.addLineHighlight(i, Color.decode("#2a5091"));
            textArea.setCaretPosition(textArea.getLineStartOffset(i));
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleStep() {
        switch (interpreter.state()) {
            case InterpreterState.PausedDebug _ -> interpreter.step();
            case InterpreterState.NotStarted _, InterpreterState.ForcedStop _,
                 InterpreterState.CompletedExceptionally _, InterpreterState.CompletedSuccessfully _ -> {
                resetExecution();
                Thread.startVirtualThread(() -> startExecution(new InterpreterState.PausedDebug(1)));
            }
            case InterpreterState.Running _ -> {
            }
        }
    }

    private JMenuBar createMenuBar() {

        JMenuBar menuBar = new JMenuBar();

        JMenu arquivo = new JMenu("Arquivo");
        JMenuItem abrir = new JMenuItem("Abrir");
        abrir.addActionListener(_ -> openFile());
        arquivo.add(abrir);
        menuBar.add(arquivo);


        JMenu editMenu = new JMenu("Edit");
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.UNDO_ACTION)));
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.REDO_ACTION)));
        editMenu.addSeparator();
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.CUT_ACTION)));
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.COPY_ACTION)));
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.PASTE_ACTION)));
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.DELETE_ACTION)));
        editMenu.addSeparator();
        editMenu.add(createMenuItem(RTextArea.getAction(RTextArea.SELECT_ALL_ACTION)));
        menuBar.add(editMenu);

        JMenu themes = new JMenu("Themes");
        themes.add(createTheme("Dark", "dark"));
        themes.add(createTheme("Default", "default"));
        themes.add(createTheme("Druid", "druid"));
        themes.add(createTheme("Eclipse", "eclipse"));
        themes.add(createTheme("IDEA", "idea"));
        themes.add(createTheme("Monokai", "monokai"));
        themes.add(createTheme("Visual Studio Code", "vs"));

        menuBar.add(themes);

        return menuBar;
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser(".");
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "VisualG Algoritmos", "alg");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(SwingIDE.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                textArea.load(FileLocation.create(chooser.getSelectedFile()));
                textArea.addLineHighlight(4, Color.RED);
            } catch (IOException | BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private JMenuItem createTheme(String title, String value) {
        JMenuItem changeStyleViaThemesItem = new JMenuItem(
                title);
        changeStyleViaThemesItem.addActionListener(_ -> changeStyleViaThemeXml(value));
        return changeStyleViaThemesItem;
    }

    private void changeStyleViaThemeXml(String name) {
        try {

            Theme theme = Theme.load(RSyntaxTextArea.class.getResourceAsStream(
                    "/org/fife/ui/rsyntaxtextarea/themes/%s.xml".formatted(name)));
            theme.apply(textArea);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private static JMenuItem createMenuItem(Action action) {
        JMenuItem item = new JMenuItem(action);
        item.setToolTipText(null);
        return item;
    }

    private CompletableFuture<Optional<InputValue>> readVariable(InputRequestValue request) {
        CompletableFuture<Optional<InputValue>> inputValueCompletableFuture = new CompletableFuture<>();

        SwingUtilities.invokeLater(() -> {
            if (dosWindow.isShowing()) {
                dosContent.setEditable(true);
                lastPromptPosition = dosContent.getText().length();
                callback = strip -> {
                    strip = strip.strip();
                    getValue(request, strip)
                            .ifPresentOrElse(value -> inputValueCompletableFuture.complete(Optional.of(value)),
                                    () -> inputValueCompletableFuture.complete(Optional.empty()));
                };
                return;
            }
            Optional.ofNullable(JOptionPane.showInputDialog("Digite um valor " + request.type() + "  para a variável " + request.variableName()))
                    .flatMap(textValue -> getValue(request, textValue))
                    .ifPresentOrElse(value -> inputValueCompletableFuture.complete(Optional.of(value)),
                            () -> inputValueCompletableFuture.complete(Optional.empty()));

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

    private void handleBreakpointChange(int currentParagraph) {
        if (breakpointLines.containsKey(currentParagraph)) {
            scrollPane.getGutter().removeTrackingIcon(breakpointLines.remove(currentParagraph));
            interpreter.removeBreakpoint(currentParagraph + 1);

        } else {
            interpreter.addBreakpoint(currentParagraph + 1);

            try {
                breakpointLines.put(currentParagraph, scrollPane.getGutter().addLineTrackingIcon(currentParagraph, new FlatAbstractIcon(10, 10, Color.RED) {
                    @Override
                    protected void paintIcon(Component component, Graphics2D graphics2D) {
                        graphics2D.fillOval(0, 0, 10, 10);
                    }
                }));
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void appendOutput(String output, ToWhere where, When when) {
        Runnable runnable = () -> {
            switch (where) {
                case DOS -> dosContent.append(output);
                case OUTPUT -> outputArea.append(output);
                case BOTH -> {
                    dosContent.append(output);
                    outputArea.append(output);
                }
            }
        };

        switch (when) {
            case NOW -> runnable.run();
            case LATER -> SwingUtilities.invokeLater(runnable);
        }
    }

    private void handleOutputEvent(OutputEvent outputEvent) {
        SwingUtilities.invokeLater(() -> {
            switch (outputEvent) {
                case OutputEvent.Text text -> appendOutput(text.text(), ToWhere.BOTH, When.NOW);
                case OutputEvent.Clear _ -> dosContent.setText("");
                case OutputEvent.ChangeColor(
                        OutputEvent.ChangeColor.Color color, OutputEvent.ChangeColor.Position position
                ) -> {
                    switch (position) {
                        case FOREGROUND -> dosContent.setForeground(Color.getColor(color.toString().toLowerCase()));
                        case BACKGROUND ->
                                dosContent.setBackground(Color.getColor(color.toString().toLowerCase() + ";"));
                    }
                }
                case null, default -> {
                }
            }
        });
    }

    private void handleExecutionSuccessfully() {
        appendOutput("\nFim da execução.", ToWhere.OUTPUT, When.LATER);
        appendOutput("\n>>> Fim da execução do programa !", ToWhere.DOS, When.LATER);
    }

    public record DebugState(String getEscopo, String getNome, String getTipo, String getValor) {
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

    private static void addDebug(Consumer<DebugState> consumer, Object variableValue, String scopeName, String variableNameUpperCase) {
        switch (variableValue) {
            case Object[][] multiObjects ->
                    addMultiArrayDebug(scopeName, variableNameUpperCase, consumer, multiObjects);
            case Object[] objects -> addArrayDebug(scopeName, variableNameUpperCase, consumer, objects);
            case Object _ -> addObjectDebug(scopeName, variableNameUpperCase, consumer, variableValue);
        }
    }

    private void resetExecution() {
        outputArea.setText("");
        debugTable.setRowCount(0);
        appendOutput("Início da execução\n", ToWhere.OUTPUT, When.NOW);
        interpreter.reset();
        breakpointLines.forEach((location, _) -> interpreter.addBreakpoint(location + 1));

        dosContent.setText("");
        dosWindow.setSize(320, 240);
        dosWindow.show();
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

    private void startExecution(InterpreterState interpreterState) {
        interpreter.runWithState(textArea.getText(), interpreterState);
        switch (interpreter.state()) {
            case InterpreterState.CompletedSuccessfully _ -> this.handleExecutionSuccessfully();
            case InterpreterState.CompletedExceptionally(Throwable e) -> this.handleExecutionError(e);
            default -> throw new IllegalStateException("Unexpected value: " + interpreter.state());
        }
        textArea.removeAllLineHighlights();
    }

    private void handleStartOrContinue() {
        switch (interpreter.state()) {
            case InterpreterState.PausedDebug _ -> {
                interpreter.continueExecution();
            }
            case InterpreterState.NotStarted _, InterpreterState.ForcedStop _,
                 InterpreterState.CompletedExceptionally _, InterpreterState.CompletedSuccessfully _ -> {
                resetExecution();
                Thread.startVirtualThread(() -> startExecution(InterpreterState.Running.INSTANCE));
            }
            case InterpreterState.Running _ -> {
            }
        }
    }

    private void updateDebugArea(ProgramState programState) {
        debugTable.setRowCount(0);

        programState.stack().entrySet().stream()
                .mapMulti((Map.Entry<String, Map<String, Object>> entry, Consumer<DebugState> consumer) -> {
                    entry.getValue().forEach((variableName, variableValue) -> {
                        String scopeName = entry.getKey().toUpperCase();
                        String variableNameUpperCase = variableName.toUpperCase();
                        addDebug(consumer, variableValue, scopeName, variableNameUpperCase);
                    });
                })
                .map(x -> new Object[]{x.getEscopo(), x.getNome(), x.getTipo(), x.getValor()})
                .forEach(debugTable::addRow);
    }

    private static class TypeChecker extends AbstractParser {
        @Override
        public ParseResult parse(RSyntaxDocument doc, String style) {
            DefaultParseResult parseResult = new DefaultParseResult(this);
            long start = System.currentTimeMillis();
            parseResult.setParsedLines(0, doc.getDefaultRootElement().getElementCount());

            try {
                String text = doc.getText(0, doc.getLength());
                ASTResult astResult = VisualgParser.parse(text);

                List<Error> errors = astResult.errors();

                astResult.node()
                        .map(dev.thihup.jvisualg.frontend.TypeChecker::semanticAnalysis)
                        .map(TypeCheckerResult::errors)
                        .ifPresent(errors::addAll);

                errors.stream()
                        .map(x -> {
                            int offset = doc.getTokenListForLine(x.location().startLine() - 1).getOffset();
                            return new DefaultParserNotice(this, x.message(), x.location().startLine() - 1, offset + x.location().startColumn(), x.location().endColumn() - x.location().startColumn() + 1);
                        })
                        .forEach(parseResult::addNotice);
            } catch (Exception e) {
                parseResult.setError(e);
            }
            parseResult.setParseTime(System.currentTimeMillis() - start);
            return parseResult;
        }
    }
}
