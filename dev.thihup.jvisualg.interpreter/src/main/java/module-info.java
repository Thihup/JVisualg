module dev.thihup.jvisualg.interpreter {
    requires dev.thihup.jvisualg.frontend;
    requires org.eclipse.lsp4j.debug;
    requires org.eclipse.lsp4j.jsonrpc;

    exports dev.thihup.jvisualg.interpreter to dev.thihup.jvisualg.ide, org.eclipse.lsp4j.jsonrpc;
}
