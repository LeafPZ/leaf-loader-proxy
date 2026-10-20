package dev.aoqia.leaf.proxy;

import java.io.IOException;
import java.lang.instrument.Instrumentation;

import org.jspecify.annotations.Nullable;

import dev.aoqia.leaf.proxy.entrypoints.ClientEntrypointHook;
import dev.aoqia.leaf.proxy.entrypoints.ServerEntrypointHook;
import dev.aoqia.leaf.proxy.transformers.EntrypointTransformer;

public class Main {
    public static @Nullable Arguments args;

    public static void premain(@Nullable String args, Instrumentation inst) {
        try {
            Utils.initLog(PathUtils.workingDir());
        } catch (IOException e) {
            System.err.println("Failed to initialise proxy logging system");
        }

        Main.args = new Arguments();
        if (args != null) {
            Main.args.parse(args.split(","));
        }
        assert Main.args != null;

        inst.addTransformer(new EntrypointTransformer(inst, GameEnv.CLIENT, ClientEntrypointHook.class), false);
        inst.addTransformer(new EntrypointTransformer(inst, GameEnv.SERVER, ServerEntrypointHook.class), false);
    }
}
