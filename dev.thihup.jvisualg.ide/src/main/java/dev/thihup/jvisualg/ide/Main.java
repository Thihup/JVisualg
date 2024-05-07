package dev.thihup.jvisualg.ide;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends Application {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @FXML
    private CodeArea codeArea;

    private Subscription subscribe;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("gui.fxml"));
        fxmlLoader.setController(this);

        Parent root = fxmlLoader.load();

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        subscribe = codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(50))
                .retainLatestUntilLater(executor)
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


        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        stage.setTitle("JVisualG");
        stage.setScene(scene);
        stage.show();
    }


    @Override
    public void stop() throws Exception {
        if (subscribe != null) {
            subscribe.unsubscribe();
        }
        executor.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static final List<String> KEYWORDS = List.of(
            "Div", "Mod", "Se", "Entao", "Então", "Senao", "Senão", "FimSe", "Para", "De", "Ate", "Até", "Passo", "FimPara", "Faca", "Faça", "Enquanto", "FimEnquanto",  "Retorne", "E", "Ou", "Nao", "Não",   "Escolha", "FimEscolha", "Repita", "FimRepita", "Caso", "OutroCaso",
            "abs", "arccos", "arcsen", "arctan", "asc", "carac", "caracpnum", "compr", "copia", "cos", "cotan", "exp", "grauprad", "int", "log", "logn", "maiusc", "minusc", "numpcarac", "pos", "pi", "quad", "radpgrau", "raizq", "rand", "randi"

    );

    private static final List<String> DATA_TYPES = List.of(
            "Inteiro", "Real", "Logico", "Caracter", "Caractere", "Literal", "Vetor"
    );

    private static final List<String> SPECIAL_KEYWORDS = List.of(
            "Escreva", "Escreval", "Leia", "Algoritmo", "FimAlgoritmo", "Funcao", "Função", "FimFuncao", "FimFunção", "Procedimento", "FimProcedimento","Inicio", "Var"
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

    private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
        String text = codeArea.getText();
        Task<StyleSpans<Collection<String>>> task = new Task<>() {
            @Override
            protected StyleSpans<Collection<String>> call() throws Exception {
                return computeHighlighting(text);
            }
        };
        executor.execute(task);
        return task;
    }

    private void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
        codeArea.setStyleSpans(0, highlighting);
    }

    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while(matcher.find()) {
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

}
