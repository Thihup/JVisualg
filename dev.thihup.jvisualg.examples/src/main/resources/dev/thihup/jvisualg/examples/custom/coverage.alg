algoritmo "coverage"

arquivo "inexistente"

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

var

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

inicio

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
