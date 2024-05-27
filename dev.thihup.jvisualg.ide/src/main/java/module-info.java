module dev.thihup.jvisualg.ide {
    requires java.desktop;

    requires dev.thihup.jvisualg.frontend;
    requires dev.thihup.jvisualg.interpreter;

    requires org.fife.RSyntaxTextArea;
    requires com.formdev.flatlaf;

    requires org.antlr.antlr4.runtime;
    requires rsyntaxtextarea.antlr4.extension;

    exports dev.thihup.jvisualg.ide to org.fife.RSyntaxTextArea;
}
