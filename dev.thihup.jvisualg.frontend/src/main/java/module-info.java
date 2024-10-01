import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.thihup.jvisualg.frontend {
    requires org.antlr.antlr4.runtime;
    requires static org.jspecify;

    exports dev.thihup.jvisualg.frontend.node to
        dev.thihup.jvisualg.backend.java,
        dev.thihup.jvisualg.lsp,
        dev.thihup.jvisualg.interpreter, dev.thihup.jvisualg.ide;
    exports dev.thihup.jvisualg.frontend to
        dev.thihup.jvisualg.lsp,
        dev.thihup.jvisualg.interpreter, dev.thihup.jvisualg.ide;
    exports dev.thihup.jvisualg.frontend.impl.antlr;

}
