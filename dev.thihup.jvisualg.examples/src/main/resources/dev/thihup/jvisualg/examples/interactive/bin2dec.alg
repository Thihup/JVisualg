Algoritmo "semnome"
// Disciplina  : [Linguagem e L�gica de Programa��o]
// Professor(a): Antonio Carlos Nicolodi
// Descri��o   : Aqui voc� descreve o que o programa faz! (fun��o)
// Autor(a)    : Nome do(a) aluno(a)
// Data atual  : 22/09/2015
Var
// Se��o de Declara��es das vari�veis
   X,xvariav : caracter
   xdecim,xmul,xm : Real
   xcomp,ind,xsoma : inteiro
   xret : Logico
//======================================================
FUNCAO MAGRO(xvar:caracter): caracter
Var i: inteiro
   x,p: caracter
inicio
   X := ""
   p := ""
   PARA i DE 1 ATe COMPR(xvar) FAcA
      p := copia(xvar,i,1)
      SE P <> " " ENTaO
         X := X + P
      FIMSE
   FIMPARA
   RETORNE X
FIMFUNCAO
// ================================================
FUNCAO E_BINARIO(xvar:caracter):Logico
Var ret: Logico
   x,i,c: Inteiro
   P: Caracter
inicio
   ret := Verdadeiro
   C := Compr(xvar)
   PARA i DE 1 ATe C FAcA
      p := Copia(xvar,i,1)
      x := CaracPnum(p)
      SE (x > 1) OU (X < 0) ENTaO
         ret := falso
      FIMSE
   FIMPARA
   RETORNE ret
FIMFUNCAO
//==================================================
Inicio
// Se��o de Comandos, procedimento, fun��es, operadores, etc...
   Limpatela
   Escreval(" ")
   Escreva("Digite um numero em bin�rio: ")
   Leia(xvariav)
   xret := E_BINARIO(xvariav)
   SE xret ENTaO
      xvariav := MAGRO(xvariav)
      xcomp := Compr(xvariav)
      X := ""
      xsoma := 0
      xmul  := 0
      PARA Ind de xcomp ATe 1 PASSO -1 FAcA
         X := Copia(xvariav,Ind,1)
         xdecim := CaracPnum(X)
         xm := xdecim * Exp(2,xmul)
         xsoma:= (xsoma + INT(xm))
         xmul := xmul + 1
      FIMPARA
   SENaO
      Escreva("Esse n�mero n�o � bin�rio! - Tecle <enter>")
      Leia(xret)
      Interrompa
   FIMSE
   Escreval(" ")
   Escreval("O resultado �: ",xsoma)
//==================================================
Fimalgoritmo