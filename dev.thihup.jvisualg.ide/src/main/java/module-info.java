module dev.thihup.jvisualg.ide {
    requires javafx.fxml;
    requires javafx.controls;
    requires org.fxmisc.richtext;
    requires reactfx;
    requires dev.thihup.jvisualg.lsp;

    exports dev.thihup.jvisualg.ide to javafx.graphics;

    opens dev.thihup.jvisualg.ide to javafx.fxml;
}
