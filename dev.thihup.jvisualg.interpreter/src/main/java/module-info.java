import org.jspecify.annotations.NullMarked;

@NullMarked
module dev.thihup.jvisualg.interpreter {
    requires dev.thihup.jvisualg.frontend;
    requires static org.jspecify;

    exports dev.thihup.jvisualg.interpreter to dev.thihup.jvisualg.ide;
}
