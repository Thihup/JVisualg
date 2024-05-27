package dev.thihup.jvisualg.ide;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeForeignAccess;

import java.lang.foreign.FunctionDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class NativeImageFeature implements Feature {

    @Override
    public void duringSetup(DuringSetupAccess access) {
        RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(ADDRESS, JAVA_LONG));
        RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(ADDRESS, ADDRESS));
        RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.ofVoid(ADDRESS));
        RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS));
        RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS));
        RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS));
        RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS));
        RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
        RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
        RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS));
        RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.ofVoid(JAVA_FLOAT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_FLOAT, JAVA_FLOAT, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS));

        try {
            Method registerForUpcall = RuntimeForeignAccess.class.getDeclaredMethod("registerForUpcall", Object.class, Object[].class);

            Object[] objects = new Object[0];
            registerForUpcall.invoke(null, FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS), objects);
            registerForUpcall.invoke(null, FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS), objects);
            registerForUpcall.invoke(null, FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS), objects);
            registerForUpcall.invoke(null, FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS), objects);
            registerForUpcall.invoke(null, FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS), objects);
            registerForUpcall.invoke(null, FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS), objects);
            registerForUpcall.invoke(null, FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS), objects);

        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }
}
