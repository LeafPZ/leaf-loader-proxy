package dev.aoqia.leaf.proxy;

import java.lang.instrument.Instrumentation;

import org.jspecify.annotations.Nullable;

import dev.aoqia.leaf.proxy.transformers.EntrypointTransformer;

public class Main {
    public static Arguments args;

    public static void premain(@Nullable String args, Instrumentation inst) {
        Main.args = new Arguments();
        if (args != null) {
            Main.args.parse(args.split(";"));
        }

        inst.addTransformer(new EntrypointTransformer(inst), false);
    }
}
