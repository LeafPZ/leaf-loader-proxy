package dev.aoqia.leaf.proxy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import dev.aoqia.leaf.proxy.json.InstallerJson;

import static dev.aoqia.leaf.proxy.Utils.log;

public final class PathUtils {
    public static Path gameJar() throws IOException, URISyntaxException {
        Utils.log("Trying to get game JAR from proxy location");

        Path gameDir = proxyJar().toPath().getParent().getParent().getParent().toAbsolutePath().normalize();
        Path gameJar = gameDir.resolve(Constants.GAME_JAR_PATHS[0]);
        if (Files.exists(gameJar)) {
            return gameJar.toAbsolutePath().normalize();
        }

        Utils.log("Trying to get game JAR from working directory");

        Path workingDir = workingDir();
        for (String jarPathStr : Constants.GAME_JAR_PATHS) {
            Path p = workingDir.resolve(jarPathStr);
            if (!Files.exists(p)) {
                continue;
            }

            return p.toAbsolutePath().normalize();
        }

        throw new FileNotFoundException("Cannot find projectzomboid.jar relative to ");
    }

    public static Path workingDir() {
        return Path.of("").toAbsolutePath().normalize();
    }

    public static File proxyJar() throws URISyntaxException {
        return new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    public static Path loaderDirectory(Path gameJar, @Nullable String override) throws IOException {
        if (override != null) {
            if (override.isBlank()) {
                throw new IOException("-loaderPath must name the directory containing the loader and dependency JARs");
            }
            return Path.of(override).toAbsolutePath().normalize();
        }

        for (Path directory = gameJar.getParent(); directory != null; directory = directory.getParent()) {
            if (directory.getFileName() != null
                && directory.getFileName().toString().equalsIgnoreCase("steamapps")) {
                return directory.resolve("workshop/content/108600").resolve(Constants.WORKSHOP_ID)
                    .resolve("mods/LeafLoader/common/media/java");
            }
        }

        throw new RuntimeException("Cannot locate Steam Workshop relative to " + gameJar
            + ". Use the proxy argument -loaderPath=<directory containing the loader JARs>.");
    }

    public static File[] getLoaderJars(Path gameJar) throws IOException {
        assert Main.args != null;

        log("Attempting to get loader jars...");
        Path jarDir = PathUtils.loaderDirectory(gameJar, Main.args.get(Constants.Arguments.LOADER_PATH));
        log("Loader directory: " + jarDir);
        return PathUtils.loaderJars(jarDir).stream().map(Path::toFile).toArray(File[]::new);
    }

    public static List<Path> loaderJars(Path directory) throws IOException {
        assert Main.args != null;

        if (!Files.isDirectory(directory)) {
            throw new FileNotFoundException(String.format("Loader directory does not exist: %s."
                    + "Subscribe to Leaf Loader (Workshop %s), finish its download, or set -loaderPath.", directory,
                Constants.WORKSHOP_ID));
        }

        try (var files = Files.list(directory)) {
            List<Path> jars = files.filter(Files::isRegularFile)
                .filter(path -> {
                    String name = path.getFileName().toString();

                    if (!name.endsWith(".jar") || name.endsWith("-sources.jar") || name.endsWith("-javadoc.jar")
                        || name.endsWith("-fat.jar")) {
                        return false;
                    }

                    if (name.startsWith("loader-")) {
                        // Assumes version information in the JAR name. If this causes a problem, can check the
                        //   jar manifest later on.
                        return Main.args.contains(Constants.Arguments.SNAPSHOT) == name.endsWith("-SNAPSHOT.jar");
                    }

                    return true;
                })
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .toList();

            if (jars.isEmpty()) {
                throw new RuntimeException("No loader JARs found in " + directory);
            }

            return jars;
        }
    }

    public static String getLoaderEntrypoint(URLClassLoader cl, GameEnv env) throws IOException {
        byte[] jsonBytes;
        try (InputStream is = cl.getResourceAsStream("leaf-installer.json")) {
            if (is == null) {
                var e = new RuntimeException("leaf-installer.json doesn't exist in the loader JAR!");
                log(e);
                throw e;
            }

            jsonBytes = is.readAllBytes();
        }

        InstallerJson installerJson = Constants.JSON.deserialize(InstallerJson.class, jsonBytes, jsonBytes.length);
        if (installerJson == null) {
            var e = new RuntimeException("Failed to deserialize installer json");
            log(e);
            throw e;
        }

        return env == GameEnv.SERVER ? installerJson.mainClass().server() : installerJson.mainClass().client();
    }
}
