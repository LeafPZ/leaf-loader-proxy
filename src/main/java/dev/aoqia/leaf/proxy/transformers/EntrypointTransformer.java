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

    private boolean hasTransformedClient = false;
    private boolean hasTransformedServer = false;

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

        final String clientEntrypoint = Utils.internalName(Constants.CLIENT_ENTRYPOINT);
        if (clientEntrypoint.equals(className)) {
            if (hasTransformedServer && !inst.removeTransformer(this)) {
                Utils.log("Failed to remove client entrypoint transformer");
                return null;
            }

            hasTransformedClient = true;
            return Utils.remapClass(entrypointClassBytes, clientEntrypoint);
        }

        final String serverEntrypoint = Utils.internalName(Constants.SERVER_ENTRYPOINT);
        if (serverEntrypoint.equals(className)) {
            if (hasTransformedClient && !inst.removeTransformer(this)) {
                Utils.log("Failed to remove server entrypoint transformer");
                return null;
            }

            hasTransformedServer = true;
            return Utils.remapClass(entrypointClassBytes, serverEntrypoint);
        }

        return null;
    }
}
