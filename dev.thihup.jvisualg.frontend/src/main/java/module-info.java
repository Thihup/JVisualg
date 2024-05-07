module dev.thihup.jvisualg.frontend {
    requires org.antlr.antlr4.runtime;

    exports dev.thihup.jvisualg.frontend.node to
        dev.thihup.jvisualg.backend.java,
        dev.thihup.jvisualg.lsp,
        dev.thihup.jvisualg.interpreter;
    exports dev.thihup.jvisualg.frontend to
        dev.thihup.jvisualg.lsp,
        dev.thihup.jvisualg.interpreter;

}
