Algoritmo "EXMPLO1"
// Disciplina: [Linguagem e Lógica de programaçao] 
// Professor : Antonio Carlos Nicolodi 
// Função :
// Autor : Antonio
// Data : 01/06/2015
var
// Seção de Declarações das variáveis 
    A,B,C,X,J,K,TEMP : INTEIRO
    XVET : VETOR[1..3] DE INTEIRO
    test: logico
inicio
// Seção de Comandos, procedimento, funções, operadores, etc... 
   ESCREVAL("Digite 3 numeros inteiro!")
   ESCREVA("Primeiro numero: ")
   LEIA(A)
   ESCREVA("Segundo numero: ")
   LEIA(B)
   ESCREVA("Terceiro numero: ")
   LEIA(C)

   xvet[1] := A
   xvet[2] := B
   xvet[3] := C

   PARA J DE 1 ATE 3 FACA
      PARA K DE J ATE 3 FACA
          SE xvet[J] > xvet[K] ENTAO
             TEMP := xvet[J]
             xvet[J] := xvet[K]
             xvet[K] := TEMP
          FIMSE
      FIMPARA
   FIMPARA

   ESCREVAL("ESCOLHA AS OPÇOES ABAIXO")
   ESCREVAL("1 - ORDEM CRESCENTE")
   ESCREVAL("2 - ORDEM DECRESCENTE")
   ESCREVAL("3 - ORDEM DE ENTRADA")
   ESCREVA("Digite: ")
   LEIA(X)
   SE (X >= 1) e (X <= 3) ENTAO
      SE X = 1 ENTAO
         ESCREVAL("ORDEM ALFABÉTICA CRESCENTE !!!")
         PARA J DE 1 ATE 3 FACA
            ESCREVAL(J," VALOR ",xvet[J])
         FIMPARA
      FIMSE

      SE X = 2 ENTAO
         ESCREVAL("ORDEM ALFABÉTICA DECRESCENTE !!!")
         PARA J DE 3 ATE 1 PASSO -1 FACA
            ESCREVAL(J," VALOR ",xvet[J])
         FIMPARA
      FIMSE

      SE X = 3 ENTAO
         ESCREVAL("ORDEM DE ENTRADA !!!")
         ESCREVAL(1," VALOR ",A)
         ESCREVAL(2," VALOR ",B)
         ESCREVAL(3," VALOR ",C)
      FIMSE
   FIMSE










fimalgoritmo
