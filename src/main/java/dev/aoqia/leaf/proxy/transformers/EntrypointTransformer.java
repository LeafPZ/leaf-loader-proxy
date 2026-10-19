package dev.aoqia.leaf.proxy.transformers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

import dev.aoqia.leaf.proxy.Main;

abstract class EntrypointTransformer implements ClassFileTransformer {
    final Instrumentation inst;
    final byte[] entrypointClassBytes;

    @SuppressWarnings("rawtypes")
    EntrypointTransformer(Instrumentation inst, Class entrypointClass) {
        this.inst = inst;

        String entry = entrypointClass.getName().replace('.', '/') + ".class";
        try (final InputStream is = Main.class.getClassLoader().getResourceAsStream(entry)) {
            assert is != null;
            this.entrypointClassBytes = is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
