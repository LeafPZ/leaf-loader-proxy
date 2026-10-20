package dev.aoqia.leaf.proxy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

public class Utils {
    private static @Nullable PrintWriter LOG;

    public static void initLog(Path workingDir) throws IOException {
        if (LOG != null) {
            return;
        }

        Path leafDir = workingDir.resolve(".leaf/");
        if (!Files.exists(leafDir)) {
            Files.createDirectories(leafDir);
        }

        File logFile = leafDir.resolve("proxy.log").toFile();
        LOG = new PrintWriter(new BufferedWriter(new FileWriter(logFile, false)), true);
    }

    public static void closeLog() {
        if (LOG == null) {
            return;
        }

        LOG.close();
        LOG = null;
    }

    public static void log(String s) {
        System.out.println("[leaf-loader-proxy]: " + s);

        assert LOG != null;
        LOG.println(s);
    }

    public static void log(Throwable t) {
        t.printStackTrace(System.err);
        t.printStackTrace(LOG);
    }

    public static byte[] remapClass(byte[] originalBytes, String newInternalName) {
        ClassReader reader = new ClassReader(originalBytes);
        ClassWriter writer = new ClassWriter(0);
        ClassRemapper remapper = new ClassRemapper(writer,
            new SimpleRemapper(Constants.ASM_VERSION, reader.getClassName(), newInternalName));
        reader.accept(remapper, 0);
        return writer.toByteArray();
    }

    public static String internalName(String s) {
        return s.replace('.', '/');
    }

    public static void addToClasspath(String... entries) {
        List<String> origClassPath = new ArrayList<>(
            Arrays.asList(System.getProperty(Constants.Properties.CLASS_PATH).split(File.pathSeparator)));

        String classPath = String.join(File.pathSeparator, origClassPath);
        if (!classPath.isEmpty()) {
            classPath += File.pathSeparator;
        }
        classPath += String.join(File.pathSeparator, entries);

        System.setProperty(Constants.Properties.CLASS_PATH, classPath);
    }

    public static void removeFromClasspath(Predicate<String> predicate) {
        List<String> origClassPath = new ArrayList<>(
            Arrays.asList(System.getProperty(Constants.Properties.CLASS_PATH).split(File.pathSeparator)));
        origClassPath.removeIf(predicate);

        String classPath = String.join(File.pathSeparator, origClassPath);
        System.setProperty(Constants.Properties.CLASS_PATH, classPath);
    }
}
