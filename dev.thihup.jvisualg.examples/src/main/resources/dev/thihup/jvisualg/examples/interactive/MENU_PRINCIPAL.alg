Algoritmo "semnome"
// Disciplina  : [Linguagem e L�gica de Programa��o]
// Professor(a): Antonio Carlos Nicolodi
// Descri��o   : Aqui voc� descreve o que o programa faz! (fun��o)
// Autor(a)    : Nome do(a) aluno(a)
// Data atual  : 15/09/2015
Var
   // Se��o de Declara��es das vari�veis
   numero : vetor[1..5] de inteiro
   NOME   : vetor[1..5] de Caracter
   IDADE  : vetor[1..5] de caracter
   indice : inteiro
     L,XL :  LOGICO
    OPCAO : Caracter


   
FUN��O PESQUISA(XV: INTEIRO) : LOGICO
VAR II : INTEIRO
   A : LOGICO
INICIO
   A := FALSO
   PARA II DE 1 AT� 5 FA�A
      SE numero[ii] = xv ENT�O
         A := VERDADEIRO
         interrompa
      FIMSE
   FIMPARA
   RETORNE A
FIMFUN��O


PROCEDIMENTO INCLUIR
VAR II  : INTEIRO
     L  :  LOGICO
   xnum : INTEIRO
   xnom : caracter
   xidd : caracter
INICIO
   limpaTela
   Escreval(" INCLUIR ")
   SE indice <= 5 ENT�O
      xnum := 0
      Xnom := ""
      xidd := ""
      L := VERDADEIRO
      ENQUANTO L FA�A
         ESCREVA("DIGITE O NUMERO",Indice," : ")
         LEIA(xnum)
         L := PESQUISA(xnum)
      FIMENQUANTO
      ESCREVA("DIGITE o Nome : ")
      LEIA(xnom)
      ESCREVA("DIGITE a Idade: ")
      LEIA(xidd)

      Numero[indice] := xnum
      Nome[indice]   := xnom
      Idade[indice]  := xidd
      indice := indice + 1
   FIMSE

   ESCREVAL(" ")
   ESCREVA("TECLE <ENTER> ")
   LEIA(XL)

FIMPROCEDIMENTO

PROCEDIMENTO CONSULTAR
VAR II : INTEIRO
   L  :  LOGICO
   xnum : INTEIRO
   xnom : caracter
   xidd : caracter

INICIO
   limpaTela

   Escreval(" CONSULTA ")

   PARA II DE 1 AT� 5 FA�A
      ESCREVAL("[",II,"] -> ",NUMERO[II]," ",NOME[II]," ",IDADE[II])
   FIMPARA

   ESCREVAL(" ")
   ESCREVA("TECLE <ENTER> ")
   LEIA(XL)

FIMPROCEDIMENTO

PROCEDIMENTO EXCLUIR
VAR  II : INTEIRO
     L  :  LOGICO
   xnum : INTEIRO
   xnom : caracter
   xidd : caracter


INICIO
   limpaTela
   Escreval(" EXCLUIR ")
   xnum := 0
   L := FALSO
   ENQUANTO L FA�A
      ESCREVA("DIGITE O NUMERO ",II," : ")
      LEIA(xnum)
      L := PESQUISA(xnum)
   FIMENQUANTO

   ESCREVAL("DIGITE o Nome : ",Nome[ii])
   ESCREVAL("DIGITE a Idade: ",Idade[ii])

   Nome[ii] := "  "
   Idade[ii]:= "  "

   ESCREVAL(" ")
   ESCREVA("TECLE <ENTER> ")
   LEIA(XL)

FIMPROCEDIMENTO


PROCEDIMENTO ALTERAR
VAR  II : INTEIRO
     L  :  LOGICO
   xnum : INTEIRO
   xnom : caracter
   xidd : caracter

INICIO
   limpaTela
   Escreval(" ALTERA��O ")
   xnum := 0
   Xnom := ""
   xidd := ""
   L := Falso
   ENQUANTO L FA�A
      ESCREVA("DIGITE O NUMERO : ")
      LEIA(xnum)
      L := PESQUISA(xnum)
   FIMENQUANTO

   ESCREVAL("DIGITE o Nome : ",Nome[ii])
   ESCREVAL("DIGITE a Idade: ",Idade[ii])

   ESCREVA("DIGITE o Nome : ")
   LEIA(xnom)
   ESCREVA("DIGITE a Idade: ")
   LEIA(xidd)

   Numero[II] := xnum
   Nome[ii]   := xnom
   Idade[ii]  := xidd

   ESCREVAL(" ")
   ESCREVA("TECLE <ENTER> ")
   LEIA(XL)

FIMPROCEDIMENTO

Inicio

   indice := 1
   L := VERDADEIRO
   OPCAO := ""
   ENQUANTO L FA�A
      LIMPATELA
      ESCREVAL("   MENU   ")
      ESCREVAL("1-INCLUS�O")
      ESCREVAL("2-CONSULTA")
      ESCREVAL("3-EXCLUS�O")
      ESCREVAL("4-ALTERA��O")
      ESCREVAL("5-FINALIZAR")
      ESCREVAL(" ")
      Escreva("Digite a op��o:")
      Leia(opcao)
      ESCOLHA OPCAO FA�A
         CASO "1"
              INCLUIR
         CASO "2"
              CONSULTAR
         CASO "3"
              EXCLUIR
         CASO "4"
              ALTERAR
         CASO "5"
              L := FALSO
      OUTROCASO
          ESCREVAL("OP��O INV�LIDA !!!")
          ESCREVAL(" ")
          ESCREVA("TECLE <ENTER> ")
          LEIA(XL)
      FIMESCOLHA
   FIMENQUANTO
Fimalgoritmo
