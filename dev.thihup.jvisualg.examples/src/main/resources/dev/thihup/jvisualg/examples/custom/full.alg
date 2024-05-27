algoritmo "semnome"

CONST
    MAX = 2

tipo

    UDT1 = registro
        x: inteiro
    fimregistro

    UDT2 = registro
        x: inteiro
        z: UDT1
    fimregistro

procedimento referencia(var x: logico)
inicio
    x <- verdadeiro
fimprocedimento

funcao narrowArguments(x: inteiro): inteiro
inicio
    retorne x
fimfuncao

funcao widenArguments(x: real): real
inicio
    retorne x
fimfuncao

VAR
    A: UDT1
    B: UDT2
    C: vetor[0..MAX] de UDT1
    D: vetor[0..MAX] de UDT2

arrayInteiro: vetor[1..10] de inteiro
matrizInteiro: vetor[1..2,1..2] de inteiro

arrayReal: vetor[1..10] de real
matrizReal: vetor[1..2,1..2] de real


arrayLogico: vetor[1..10] de logico
matrizLogico: vetor[1..2,1..2] de logico


arrayCaracter: vetor[1..10] de caracter
matrizCaracter: vetor[1..2,1..2] de caracter


flutuante: real

meuInteiro: inteiro
meuReal: real
meuCaracter: caracter
meuLogico: logico

x, y, resultado: real
  texto1, texto2, resultadoStr: caracter
  i: inteiro

inicio

    aleatorio on

    leia(A.x)
    leia(B.x)
    leia(C[0].x)
    leia(D[0].x)

    escreval(A.x)

    escreval(B.x)

    escreval(C[0].x)

    escreval(D[0].x)

    A.x <- 4
    B.x <- 1
    C[0].x <- 3
    D[0].x <- 8

    escreval(A.x)

    escreval(B.x)

    escreval(C[0].x)

    escreval(D[0].x)

  // *** Funções Matemáticas ***

  x := 5.0
  y := 10.0

  resultado := ABS(x)
  resultado := ARCCOS(x)
  resultado := ARCSEN(x)
  resultado := ARCTAN(x)
  resultado := COS(x)
  resultado := COTAN(x)
  resultado := EXP(x, y)
  resultado := GRAUPRAD(x)
  resultado := INT(x)
  resultado := LOG(x)
  resultado := LOGN(x)
  resultado := PI()
  resultado := QUAD(x)
  resultado := RADPGRAU(x)
  resultado := RAIZQ(x)
  resultado := RAND()
  i := RANDI(100)
  resultado := SEN(x)
  resultado := TAN(x)

  // *** Funções de Strings ***

  texto1 := "1000"
  texto2 := "1"

  i := ASC(texto1)
  resultadoStr := CARAC(65)
  i := CARACPNUM(texto1)
  i := COMPR(texto1)
  resultadoStr := COPIA(texto1, 2, 3)
  resultadoStr := MAIUSC(texto1)
  resultadoStr := MINUSC(texto1)
  resultadoStr := NUMPCARAC(65)
  i := POS(texto2, texto1)


 aleatorio on

    mudaCor("VERMELHO", "FRENTE")
    mudaCor("Amarelo", "FUNDOS")
    mudaCor("azul", "FRENTE")
    mudaCor("branco", "FUNDOS")
    mudaCor("preto", "FUNDOS")
    mudaCor("verde", "FUNDOS")

    flutuante <- pi
    meuInteiro <- narrowArguments(flutuante)
    escreval(meuInteiro)

    meuReal <- widenArguments(12)
    escreval(meuReal)
    meuReal <- widenArguments(meuInteiro)

    leia(arrayInteiro[1])
    leia(matrizInteiro[1,2])

    escreval(arrayInteiro[1])
    escreval(matrizInteiro[1,2])

    arrayInteiro[1] <- 5
    matrizInteiro[1,2] <- 10

    arrayReal[1] <- arrayInteiro[1]
    matrizReal[1, 2] <- arrayInteiro[1]

    meuReal <- 2 ^ 5

    debug 1 < 1

    referencia(meuLogico)

    escreval(+meuInteiro)
    escreval(+meuReal)

    escolha arrayInteiro[1] faca
        caso 4
            escreval("Nao aparecer")
        caso 5
            escreval("Apareci")
    fimescolha

    escolha matrizInteiro[1,2] faca
        caso 1 ate 3
            escreval("Nao aparecer")
        caso 4 ate 10
            escreval("Apareci")
    fimescolha

    leia(meuCaracter)
    leia(meuLogico)
    leia(meuInteiro)
    leia(meuReal)

fimalgoritmo
