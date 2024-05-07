module dev.thihup.jvisualg.ide {
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.fxml;
    requires org.fxmisc.richtext;
    requires reactfx;

    exports dev.thihup.jvisualg.ide to javafx.graphics;

    opens dev.thihup.jvisualg.ide to javafx.fxml;
}
