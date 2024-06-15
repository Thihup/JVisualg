module dev.thihup.jvisualg.ide {
    requires java.desktop;

    requires dev.thihup.jvisualg.interpreter;

    requires org.fife.RSyntaxTextArea;
    requires com.formdev.flatlaf;
    requires autocomplete;
    requires org.eclipse.lsp4j;
    requires rstaui;

    requires rsyntaxtextarea.antlr4.extension;
    requires dev.thihup.jvisualg.lsp;
    requires dev.thihup.jvisualg.frontend;
    requires org.antlr.antlr4.runtime;

    exports dev.thihup.jvisualg.ide to org.fife.RSyntaxTextArea;
}
