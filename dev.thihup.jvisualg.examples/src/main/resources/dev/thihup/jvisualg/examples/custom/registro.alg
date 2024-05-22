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

VAR
    A: UDT1
    B: UDT2
    C: vetor[0..MAX] de UDT1
    D: vetor[0..MAX] de UDT2

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

fimalgoritmo
