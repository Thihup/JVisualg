Algoritmo "Invertenome"
// Disciplina: [Linguagem e L�gica de programa�ao] 
// Professor: Antonio Carlos Nicolodi
// Fun��o: Calcula os passos de um caracol para subir o po�o
// Autor    : Antonio Carlos Nicolodi
// Data     : 05/04/2015
// Fun��o :
// Autor :
// Data : 09/04/2015
// Se��o de Declara��es 
var
    s,c : caracter
    i,x : inteiro
inicio
// Se��o de Comandos 
    Escreva("Digite um nome : ")
    Leia(s)
    x <- compr(s)
    c <- ""
    para i de x ate 1 passo -1 faca
       c <- c + copia(s,i,1)
    fimpara
    Escreva(" ")
    Escreval(c)
fimalgoritmo
