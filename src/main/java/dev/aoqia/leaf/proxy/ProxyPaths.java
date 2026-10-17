package dev.aoqia.leaf.proxy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public final class ProxyPaths {
    private static final List<String> GAME_JAR_PATHS = List.of("projectzomboid.jar",
        "projectzomboid/projectzomboid.jar", "Contents/Java/projectzomboid.jar");

    public static Path gameJar(final Path proxyJar, final Path workingDirectory) throws IOException {
        for (Path directory = proxyJar.toAbsolutePath().normalize().getParent();
            directory != null; directory = directory.getParent()) {
            for (String relativePath : GAME_JAR_PATHS) {
                final Path candidate = directory.resolve(relativePath);
                if (Files.isRegularFile(candidate)) {
                    return candidate.toRealPath();
                }
            }
        }

        final Path candidate = workingDirectory.toAbsolutePath().normalize().resolve("projectzomboid.jar");
        if (Files.isRegularFile(candidate)) {
            return candidate.toRealPath();
        }

        throw new FileNotFoundException("Cannot find projectzomboid.jar relative to " + proxyJar
            + " or in " + workingDirectory.toAbsolutePath());
    }

    public static Path loaderDirectory(final Path gameJar, final String override) throws IOException {
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

    public static List<Path> loaderJars(final Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new FileNotFoundException(String.format("Loader directory does not exist: %s."
                    + "Subscribe to Leaf Loader (Workshop %s), finish its download, or set -loaderPath.", directory,
                Constants.WORKSHOP_ID));
        }

        try (var files = Files.list(directory)) {
            final List<Path> jars = files.filter(Files::isRegularFile)
                .filter(path -> {
                    String name = path.getFileName().toString();

                    if (!name.endsWith(".jar") || name.endsWith("-sources.jar") || name.endsWith("-javadoc.jar")
                        || name.endsWith("-fat.jar")) {
                        return false;
                    }

                    if (name.startsWith("loader-")) {
                        return !Main.args.contains(Constants.Arguments.SNAPSHOT) || name.endsWith("-SNAPSHOT.jar");
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
}
