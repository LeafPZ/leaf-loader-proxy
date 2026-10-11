package dev.aoqia.leaf.proxy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

public class Utils {
    private static PrintWriter LOG;

    public static void initLog(final File gameDir) throws IOException {
        File leafDir = new File(gameDir, ".leaf/");
        if (!leafDir.exists()) {
            Files.createDirectories(leafDir.toPath());
        }

        File logFile = new File(leafDir, "proxy.log");
        LOG = new PrintWriter(new BufferedWriter(new FileWriter(logFile, false)), true);
    }

    public static void closeLog() {
        if (LOG == null) {
            return;
        }

        LOG.close();
        LOG = null;
    }

    public static void log(final String s) {
        System.out.println(s);
        if (LOG == null) {
            return;
        }
        LOG.println(s);
    }

    public static void log(final Exception e) {
        e.printStackTrace(System.err);
        if (LOG == null) {
            return;
        }
        e.printStackTrace(LOG);
    }

    public static byte[] remapClass(final byte[] originalBytes, final String newInternalName) {
        ClassReader reader = new ClassReader(originalBytes);
        ClassWriter writer = new ClassWriter(0);
        ClassRemapper remapper = new ClassRemapper(writer,
            new SimpleRemapper(Constants.ASM_VERSION, reader.getClassName(), newInternalName));
        reader.accept(remapper, 0);
        return writer.toByteArray();
    }

    public static String internalName(final String s) {
        return s.replace('.', '/');
    }

    public static void addToClasspath(final String... entries) {
        final List<String> origClassPath = new ArrayList<>(
            Arrays.asList(System.getProperty(Constants.Properties.CLASS_PATH).split(File.pathSeparator)));

        String classPath = String.join(File.pathSeparator, origClassPath);
        if (!classPath.isEmpty()) {
            classPath += File.pathSeparator;
        }
        classPath += String.join(File.pathSeparator, entries);

        System.setProperty(Constants.Properties.CLASS_PATH, classPath);
    }

    public static void removeFromClasspath(final Predicate<String> predicate) {
        List<String> origClassPath = new ArrayList<>(
            Arrays.asList(System.getProperty(Constants.Properties.CLASS_PATH).split(File.pathSeparator)));
        origClassPath.removeIf(predicate);

        final String classPath = String.join(File.pathSeparator, origClassPath);
        System.setProperty(Constants.Properties.CLASS_PATH, classPath);
    }
}
