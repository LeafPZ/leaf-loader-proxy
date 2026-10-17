package dev.aoqia.leaf.proxy;

import org.objectweb.asm.Opcodes;

public class Constants {
    public static final int ASM_VERSION = Opcodes.ASM9;

    public static final String WORKSHOP_ID = "3776625738";

    public static final String CLIENT_ENTRYPOINT = "zombie.gameStates.MainScreenState";
    public static final String SERVER_ENTRYPOINT = "zombie.server.GameServer";

    public static final class Properties {
        public static final String CLASS_PATH = "java.class.path";
    }

    public static final class Arguments {
        public static final String LOADER_PATH = "loaderPath";
        public static final String SNAPSHOT = "snapshot";
    }
}
