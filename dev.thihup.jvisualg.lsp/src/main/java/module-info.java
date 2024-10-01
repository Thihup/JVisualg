import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.thihup.jvisualg.lsp {
    requires org.eclipse.lsp4j;
    requires org.eclipse.lsp4j.jsonrpc;
    requires com.google.gson;
    requires dev.thihup.jvisualg.frontend;
    requires static org.jspecify;

    exports dev.thihup.jvisualg.lsp to dev.thihup.jvisualg.ide;
}
