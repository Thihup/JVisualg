module dev.thihup.jvisualg.ide {
    requires dev.thihup.jvisualg.lsp;
    requires dev.thihup.jvisualg.interpreter;
    requires org.fife.RSyntaxTextArea;
    requires com.formdev.flatlaf;
    requires dev.thihup.jvisualg.frontend;
    requires org.antlr.antlr4.runtime;
    requires rsyntaxtextarea.antlr4.extension;
    requires java.desktop;
    requires org.graalvm.nativeimage;

    exports dev.thihup.jvisualg.ide to org.fife.RSyntaxTextArea;
}
