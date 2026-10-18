package dev.aoqia.leaf.proxy.transformers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.jspecify.annotations.Nullable;

import dev.aoqia.leaf.proxy.Constants;
import dev.aoqia.leaf.proxy.Main;
import dev.aoqia.leaf.proxy.Utils;
import dev.aoqia.leaf.proxy.entrypoints.MainEntrypoint;

public class EntrypointTransformer implements ClassFileTransformer {
    private final Instrumentation inst;

    public EntrypointTransformer(Instrumentation inst) {
        this.inst = inst;
    }

    @Override
    public byte @Nullable [] transform(@Nullable ClassLoader loader, String className,
        @Nullable Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer)
    {
        final String entrypointClass = MainEntrypoint.class.getName().replace('.', '/') + ".class";
        byte[] entrypointClassBytes;
        try (final InputStream is = Main.class.getClassLoader().getResourceAsStream(entrypointClass)) {
            assert is != null;
            entrypointClassBytes = is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // TODO: Differentiate between client and server? Maybe a -server arg or something
        //   or maybe we can determine client vs server based on original entrypoint?
        final String entrypoint = Utils.internalName(Constants.CLIENT_ENTRYPOINT);
        if (entrypoint.equals(className) && inst.removeTransformer(this)) {
            return Utils.remapClass(entrypointClassBytes, entrypoint);
        }

        return null;
    }
}
