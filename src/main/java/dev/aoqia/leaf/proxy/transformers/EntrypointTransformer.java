package dev.aoqia.leaf.proxy.transformers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.jspecify.annotations.Nullable;

import dev.aoqia.leaf.proxy.Constants;
import dev.aoqia.leaf.proxy.GameEnv;
import dev.aoqia.leaf.proxy.Main;
import dev.aoqia.leaf.proxy.Utils;

public final class EntrypointTransformer implements ClassFileTransformer {
    final Instrumentation inst;
    final GameEnv env;
    final byte[] entrypointClassBytes;

    @SuppressWarnings("rawtypes")
    public EntrypointTransformer(Instrumentation inst, GameEnv env, Class entrypointClass) {
        this.inst = inst;
        this.env = env;

        String entry = Utils.internalName(entrypointClass.getName()) + ".class";
        try (final InputStream is = Main.class.getClassLoader().getResourceAsStream(entry)) {
            assert is != null;
            this.entrypointClassBytes = is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte @Nullable [] transform(@Nullable ClassLoader loader, String className,
        @Nullable Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer)
    {
        try {
            if (!className.startsWith("zombie")) {
                return null;
            }

            if (loader != ClassLoader.getSystemClassLoader()) {
                return null;
            }

            String rawEntrypoint = env == GameEnv.SERVER ? Constants.SERVER_ENTRYPOINT : Constants.CLIENT_ENTRYPOINT;
            String gameEntrypoint = Utils.internalName(rawEntrypoint);
            if (!gameEntrypoint.equals(className)) {
                return null;
            }

            if (!inst.removeTransformer(this)) {
                Utils.log("Failed to remove " + env.name() + " entrypoint transformer");
                return null;
            }

            return Utils.remapClass(entrypointClassBytes, gameEntrypoint);
        } catch (Throwable t) {
            Utils.log("An error occurred during the transform stage:");
            Utils.log(t);
            return null;
        }
    }
}
