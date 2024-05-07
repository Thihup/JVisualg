module dev.thihup.jvisualg.ide {
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.controls;
    requires org.fxmisc.richtext;
    requires reactfx;
    requires transitive dev.thihup.jvisualg.lsp;

    exports dev.thihup.jvisualg.ide to javafx.graphics;

    opens dev.thihup.jvisualg.ide to javafx.fxml;
}
