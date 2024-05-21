package dev.thihup.jvisualg.interpreter;

import dev.thihup.jvisualg.examples.ExamplesBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
                        """, Executors.newVirtualThreadPerTaskExecutor()).join();
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
        Interpreter interpreter = new Interpreter(new IO(_ -> CompletableFuture.completedFuture(new InputValue.InteiroValue(5)), s -> {
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
                """, Executors.newVirtualThreadPerTaskExecutor()).join();

        assertEquals(
                """
                Number:  5
                """, stringWriter.toString());
    }

    @ParameterizedTest
    @MethodSource({"examplesV25", "examplesV30"})
    void testExamples(Path path) throws Throwable {
        RandomGenerator aDefault = RandomGenerator.getDefault();

        IO io = new IO(
                inputRequest -> CompletableFuture.completedFuture(switch (inputRequest.type()) {
                    case INTEIRO -> new InputValue.InteiroValue(aDefault.nextInt(10));
                    case REAL -> new InputValue.RealValue(aDefault.nextDouble(10));
                    case LOGICO -> new InputValue.LogicoValue(aDefault.nextBoolean());
                    case CARACTER -> new InputValue.CaracterValue(aDefault.ints(65, 91)
                            .limit(5)
                            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString());
                })
                , _ -> {
        });

        Interpreter interpreter = new Interpreter(io);

        interpreter.run(Files.readString(path, StandardCharsets.ISO_8859_1), Executors.newVirtualThreadPerTaskExecutor()).get(10, TimeUnit.SECONDS);
    }
}
