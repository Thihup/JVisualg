algoritmo "FuncoesMatematicasEStrings"


var
  x, y, resultado: real
  texto1, texto2, resultadoStr: caracter
  i: inteiro

inicio
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

fimalgoritmo
