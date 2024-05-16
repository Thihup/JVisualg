module dev.thihup.jvisualg.interpreter {
    requires dev.thihup.jvisualg.frontend;
    requires jdk.unsupported;

    exports dev.thihup.jvisualg.interpreter to dev.thihup.jvisualg.ide;
}
