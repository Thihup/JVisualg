module dev.thihup.jvisualg.ide {
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.fxml;
    requires org.fxmisc.richtext;
    requires reactfx;
    requires dev.thihup.jvisualg.lsp;
    requires org.eclipse.lsp4j;
    requires org.eclipse.lsp4j.jsonrpc;
    requires javafx.controls;
    exports dev.thihup.jvisualg.ide to javafx.graphics;

    opens dev.thihup.jvisualg.ide to javafx.fxml;
}
