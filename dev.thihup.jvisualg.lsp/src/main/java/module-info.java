module dev.thihup.jvisualg.lsp {
    requires org.eclipse.lsp4j;
    requires org.eclipse.lsp4j.jsonrpc;
    requires com.google.gson;
    requires dev.thihup.jvisualg.frontend;

    exports dev.thihup.jvisualg.lsp to dev.thihup.jvisualg.ide;
}
