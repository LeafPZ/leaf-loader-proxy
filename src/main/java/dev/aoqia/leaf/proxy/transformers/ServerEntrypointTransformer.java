package dev.aoqia.leaf.proxy.transformers;

import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.jspecify.annotations.Nullable;

import dev.aoqia.leaf.proxy.Constants;
import dev.aoqia.leaf.proxy.Utils;

public class ServerEntrypointTransformer extends EntrypointTransformer {
    @SuppressWarnings("rawtypes")
    public ServerEntrypointTransformer(Instrumentation inst, Class entrypointClass) {
        super(inst, entrypointClass);
    }

    @Override
    public byte @Nullable [] transform(@Nullable ClassLoader loader, String className,
        @Nullable Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer)
    {
        if (!className.startsWith("zombie")) {
            return null;
        }

        final String gameEntrypoint = Utils.internalName(Constants.SERVER_ENTRYPOINT);
        if (!gameEntrypoint.equals(className)) {
            return null;
        }

        if (!inst.removeTransformer(this)) {
            Utils.log("Failed to remove server entrypoint transformer");
            return null;
        }

        return Utils.remapClass(entrypointClassBytes, gameEntrypoint);
    }
}
