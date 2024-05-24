package dev.thihup.jvisualg.interpreter;

import dev.thihup.jvisualg.examples.ExamplesBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class InterpreterTest extends ExamplesBase {
    @Test
    void test() {
        StringWriter stringWriter = new StringWriter();
        IO io = new IO(_ -> null, s -> {
            switch (s) {
                case OutputEvent.Text(String text) -> stringWriter.write(text);
                case OutputEvent.ChangeColor _, OutputEvent.Clear _ -> {
                }
            }
        });
        new Interpreter(io)
                .run("""
                        algoritmo "Teste"
                        var
                        a: inteiro
                        inicio
                        escreval("Hello, World!", 5)
                        
                        escreval(5)
                        escreval(5.1)
                        escreval(5.10)
                        escreval(5.100)
                        escreval(5.1000)
                        
                        escreval(6.12)
                        escreval(6.125)
                        escreval(6.1258)
                        escreval(6.20000)
                        
                        para a de 0 ate 10 faca
                           escreval("VALOR ESPACO", a)
                           escreval(5 : a)
                           escreval(5.1 : a)
                           escreval(5.10 : a)
                           escreval(5.100 : a)
                           escreval(5.1000 : a)
                        
                           escreval(6.12 : a)
                           escreval(6.125 : a)
                           escreval(6.1258 : a)
                           escreval(6.20000 : a)
                        
                           escreval("VALOR ESPACO E PRECISAO", a)
                           escreval(5 : a : a)
                           escreval(5.1 : a : a)
                           escreval(5.10 : a : a)
                           escreval(5.100 : a : a)
                           escreval(5.1000 : a : a)
                        
                           escreval(6.12 : a : a)
                           escreval(6.125 : a : a)
                           escreval(6.1258 : a : a)
                           escreval(6.20000 : a : a)
                        
                           escreval("VALOR ESPACO 0 E PRECISAO", a)
                           escreval(5 : 0 : a)
                           escreval(5.1 : 0 : a)
                           escreval(5.10 : 0 : a)
                           escreval(5.100 : 0 : a)
                           escreval(5.1000 : 0 : a)
                        
                           escreval(6.12 : 0 : a)
                           escreval(6.125 : 0 : a)
                           escreval(6.1258 : 0 : a)
                           escreval(6.20000 : 0 : a)
                        fimpara
                        
                        escreval(verdadeiro)
                        escreval(falso)
                        escreval(a)
                        fimalgoritmo
                        """);
        assertEquals(
                """
                        Hello, World! 5
                         5
                         5.1
                         5.1
                         5.1
                         5.1
                         6.12
                         6.125
                         6.1258
                         6.2
                        VALOR ESPACO 0
                         5
                         5.1
                         5.1
                         5.1
                         5.1
                         6.12
                         6.125
                         6.1258
                         6.2
                        VALOR ESPACO E PRECISAO 0
                         5
                         5.1
                         5.1
                         5.1
                         5.1
                         6.12
                         6.125
                         6.1258
                         6.2
                        VALOR ESPACO 0 E PRECISAO 0
                         5
                         5.1
                         5.1
                         5.1
                         5.1
                         6.12
                         6.125
                         6.1258
                         6.2
                        VALOR ESPACO 1
                        5
                        5
                        5
                        5
                        5
                        6
                        6
                        6
                        6
                        VALOR ESPACO E PRECISAO 1
                        5.0
                        5.1
                        5.1
                        5.1
                        5.1
                        6.1
                        6.1
                        6.1
                        6.2
                        VALOR ESPACO 0 E PRECISAO 1
                         5
                         5.1
                         5.1
                         5.1
                         5.1
                         6.12
                         6.125
                         6.1258
                         6.2
                        VALOR ESPACO 2
                         5
                         5
                         5
                         5
                         5
                         6
                         6
                         6
                         6
                        VALOR ESPACO E PRECISAO 2
                        5.00
                        5.10
                        5.10
                        5.10
                        5.10
                        6.12
                        6.13
                        6.13
                        6.20
                        VALOR ESPACO 0 E PRECISAO 2
                         5
                         5.1
                         5.1
                         5.1
                         5.1
                         6.12
                         6.125
                         6.1258
                         6.2
                        VALOR ESPACO 3
                          5
                          5
                          5
                          5
                          5
                          6
                          6
                          6
                          6
                        VALOR ESPACO E PRECISAO 3
                        5.000
                        5.100
                        5.100
                        5.100
                        5.100
                        6.120
                        6.125
                        6.126
                        6.200
                        VALOR ESPACO 0 E PRECISAO 3
                         5
                         5.1
                         5.1
                         5.1
                         5.1
                         6.12
                         6.125
                         6.1258
                         6.2
                        VALOR ESPACO 4
                           5
                           5
                           5
                           5
                           5
                           6
                           6
                           6
                           6
                        VALOR ESPACO E PRECISAO 4
                        5.0000
                        5.1000
                        5.1000
                        5.1000
                        5.1000
                        6.1200
                        6.1250
                        6.1258
                        6.2000
                        VALOR ESPACO 0 E PRECISAO 4
                         5
                         5.1
                         5.1
                         5.1
                         5.1
                         6.12
                         6.125
                         6.1258
                         6.2
                        VALOR ESPACO 5
                            5
                            5
                            5
                            5
                            5
                            6
                            6
                            6
                            6
                        VALOR ESPACO E PRECISAO 5
                        5.00000
                        5.10000
                        5.10000
                        5.10000
                        5.10000
                        6.12000
                        6.12500
                        6.12580
                        6.20000
                        VALOR ESPACO 0 E PRECISAO 5
                         5
                         5.1
                         5.1
                         5.1
                         5.1
                         6.12
                         6.125
                         6.1258
                         6.2
                        VALOR ESPACO 6
                             5
                             5
                             5
                             5
                             5
                             6
                             6
                             6
                             6
                        VALOR ESPACO E PRECISAO 6
                        5.000000
                        5.100000
                        5.100000
                        5.100000
                        5.100000
                        6.120000
                        6.125000
                        6.125800
                        6.200000
                        VALOR ESPACO 0 E PRECISAO 6
                         5
                         5.1
                         5.1
                         5.1
                         5.1
                         6.12
                         6.125
                         6.1258
                         6.2
                        VALOR ESPACO 7
                              5
                              5
                              5
                              5
                              5
                              6
                              6
                              6
                              6
                        VALOR ESPACO E PRECISAO 7
                        5.0000000
                        5.1000000
                        5.1000000
                        5.1000000
                        5.1000000
                        6.1200000
                        6.1250000
                        6.1258000
                        6.2000000
                        VALOR ESPACO 0 E PRECISAO 7
                         5
                         5.1
                         5.1
                         5.1
                         5.1
                         6.12
                         6.125
                         6.1258
                         6.2
                        VALOR ESPACO 8
                               5
                               5
                               5
                               5
                               5
                               6
                               6
                               6
                               6
                        VALOR ESPACO E PRECISAO 8
                        5.00000000
                        5.10000000
                        5.10000000
                        5.10000000
                        5.10000000
                        6.12000000
                        6.12500000
                        6.12580000
                        6.20000000
                        VALOR ESPACO 0 E PRECISAO 8
                         5
                         5.1
                         5.1
                         5.1
                         5.1
                         6.12
                         6.125
                         6.1258
                         6.2
                        VALOR ESPACO 9
                                5
                                5
                                5
                                5
                                5
                                6
                                6
                                6
                                6
                        VALOR ESPACO E PRECISAO 9
                        5.000000000
                        5.100000000
                        5.100000000
                        5.100000000
                        5.100000000
                        6.120000000
                        6.125000000
                        6.125800000
                        6.200000000
                        VALOR ESPACO 0 E PRECISAO 9
                         5
                         5.1
                         5.1
                         5.1
                         5.1
                         6.12
                         6.125
                         6.1258
                         6.2
                        VALOR ESPACO 10
                                 5
                                 5
                                 5
                                 5
                                 5
                                 6
                                 6
                                 6
                                 6
                        VALOR ESPACO E PRECISAO 10
                        5.0000000000
                        5.1000000000
                        5.1000000000
                        5.1000000000
                        5.1000000000
                        6.1200000000
                        6.1250000000
                        6.1258000000
                        6.2000000000
                        VALOR ESPACO 0 E PRECISAO 10
                         5
                         5.1
                         5.1
                         5.1
                         5.1
                         6.12
                         6.125
                         6.1258
                         6.2
                         VERDADEIRO
                         FALSO
                         11
                        """, stringWriter.toString());
    }

    @Test
    void testRead() {
        StringWriter stringWriter = new StringWriter();
        Interpreter interpreter = new Interpreter(new IO(_ -> CompletableFuture.completedFuture(Optional.of(new InputValue.InteiroValue(5))), s -> {
            switch (s) {
                case OutputEvent.Text(String text) -> stringWriter.write(text);
                case OutputEvent.ChangeColor _, OutputEvent.Clear _ -> {
                }
            }
        }));
        interpreter.run("""
                algoritmo "Teste"
                var
                a: inteiro
                inicio
                    escreva("Number: ")
                    leia(a)
                    escreval(a)
                fimalgoritmo
                """);

        assertEquals(
                """
                        Number:  5
                        """, stringWriter.toString());
    }

    @ParameterizedTest
    @MethodSource({"examplesV25", "examplesV30", "examplesCustom"})
    void testExamples(Path path) throws Throwable {
        RandomGenerator aDefault = RandomGenerator.getDefault();
        StringBuilder stringBuilder = new StringBuilder();
        IO io = new IO(
                inputRequest -> CompletableFuture.completedFuture(Optional.of(switch (inputRequest.type()) {
                    case INTEIRO -> new InputValue.InteiroValue(aDefault.nextInt(1, 10));
                    case REAL -> new InputValue.RealValue(aDefault.nextDouble(1, 10));
                    case LOGICO -> new InputValue.LogicoValue(aDefault.nextBoolean());
                    case CARACTER -> new InputValue.CaracterValue(aDefault.ints(65, 91)
                            .limit(5)
                            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString());
                }))
                , a -> {
            switch (a) {
                case OutputEvent.Text(String t) -> stringBuilder.append(t);
                default -> {
                }
            }
        });

        Interpreter interpreter = new Interpreter(io);

        interpreter.run(Files.readString(path, StandardCharsets.ISO_8859_1));

        System.out.println(stringBuilder);
    }

    record FileAndError(Path path, Class<? extends Exception> e){}

    private static Stream<FileAndError> exampleErrorsWithType() throws Throwable {
        return Stream.of(
            ExamplesBase.examples("errors/assignment").map(x -> new FileAndError(x, TypeException.InvalidAssignment.class)),
            ExamplesBase.examples("errors/invalidOperand").map(x -> new FileAndError(x, TypeException.InvalidOperand.class)),
            ExamplesBase.examples("errors/unsupported").map(x -> new FileAndError(x, UnsupportedOperationException.class)),
            ExamplesBase.examples("errors/wrongArguments").map(x -> new FileAndError(x, TypeException.WrongNumberOfArguments.class)),
            ExamplesBase.examples("errors/notfound/function").map(x -> new FileAndError(x, TypeException.FunctionNotFound.class)),
            ExamplesBase.examples("errors/notfound/variable").map(x -> new FileAndError(x, TypeException.VariableNotFound.class)),
            ExamplesBase.examples("errors/notfound/procedure").map(x -> new FileAndError(x, TypeException.ProcedureNotFound.class)),
            ExamplesBase.examples("errors/notfound/type").map(x -> new FileAndError(x, TypeException.TypeNotFound.class)),
            ExamplesBase.examples("errors/index").map(x -> new FileAndError(x, TypeException.IndexOutOfBounds.class))
        ).flatMap(s -> s);
    }

    @ParameterizedTest
    @MethodSource("exampleErrorsWithType")
    void testExamplesErrors(FileAndError file) throws Throwable {
        StringBuilder stringBuilder = new StringBuilder();
        IO io = new IO(
            _ -> CompletableFuture.completedFuture(Optional.empty()),
            a -> {
                switch (a) {
                    case OutputEvent.Text(String t) -> stringBuilder.append(t);
                    default -> {
                    }
                }
        });

        Interpreter interpreter = new Interpreter(io);

        interpreter.run(Files.readString(file.path, StandardCharsets.ISO_8859_1));
        System.out.println(stringBuilder);
        switch (interpreter.state()) {
            case InterpreterState.CompletedExceptionally(Throwable e) -> assertInstanceOf(file.e, e);
            default -> fail();
        }

    }

    enum Types {
        CARACTER, LOGICO, REAL, INTEIRO
    }

    @CartesianTest
    void testBinaryOperands(@CartesianTest.Values(strings = {">", ">=", "<", "<=", "<>", "=", "e", "ou"}) String operand,
                            @CartesianTest.Enum(Types.class) Types type1,
                            @CartesianTest.Enum(Types.class) Types type2) throws Throwable {
        StringBuilder stringBuilder = new StringBuilder();
        IO io = new IO(
                _ -> CompletableFuture.completedFuture(Optional.empty()),
                a -> {
                    switch (a) {
                        case OutputEvent.Text(String t) -> stringBuilder.append(t);
                        default -> {
                        }
                    }
                });

        Interpreter interpreter = new Interpreter(io);

        String program = """
                algoritmo "operands"
                var
                    x: %s
                    y: %s
                inicio
                    escreval(x %s y)
                fimalgoritmo
                """;

        interpreter.run(program.formatted(type1, type2, operand));
        System.out.println(stringBuilder);
        switch (interpreter.state()) {
            case InterpreterState.CompletedSuccessfully _ -> {}
            case InterpreterState.CompletedExceptionally(Throwable e) -> assertInstanceOf(TypeException.InvalidOperand.class, e);
            default -> fail();
        }

    }

    @CartesianTest
    void testUnaryOperands(@CartesianTest.Values(strings = {"+", "-", "nao"}) String operand,
                            @CartesianTest.Enum(Types.class) Types type) throws Throwable {
        StringBuilder stringBuilder = new StringBuilder();
        IO io = new IO(
                _ -> CompletableFuture.completedFuture(Optional.empty()),
                a -> {
                    switch (a) {
                        case OutputEvent.Text(String t) -> stringBuilder.append(t);
                        default -> {
                        }
                    }
                });

        Interpreter interpreter = new Interpreter(io);

        String program = """
                algoritmo "operands"
                var
                    x: %s
                inicio
                    escreval(%s x)
                fimalgoritmo
                """;

        interpreter.run(program.formatted(type, operand));
        System.out.println(stringBuilder);
        switch (interpreter.state()) {
            case InterpreterState.CompletedSuccessfully _ -> {}
            case InterpreterState.CompletedExceptionally(Throwable e) -> assertInstanceOf(TypeException.InvalidOperand.class, e);
            default -> fail();
        }

    }

}
