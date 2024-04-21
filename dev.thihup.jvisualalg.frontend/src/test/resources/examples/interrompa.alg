Algoritmo "interrompa"
// Disciplina: [Linguagem e Lgica de programaao] 
// Funo : Demonstrar o uso dos comandos interrompa e fimrepita
// Autor :
// Data : 04/01/2004
// Seo de Declaraes 
var x : inteiro
inicio
x <- 0
repita
   x <- x + 1
   escreva(x)
   se x = 10 entao
      interrompa // no estruturado, mas usado por alguns autores...
   fimse
fimrepita
escreval()
// O trecho acima  o mesmo que:
x <- 0
repita
   x <- x + 1
   escreva(x)
ate x = 10
fimalgoritmo
