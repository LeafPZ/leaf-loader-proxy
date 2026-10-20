package dev.aoqia.leaf.proxy.entrypoints;

import dev.aoqia.leaf.proxy.GameEnv;

public class ServerEntrypointHook extends EntrypointHook {
    static void main(String[] args) {
        run(args, GameEnv.SERVER);
    }
}
