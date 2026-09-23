package dev.aoqia.leaf.proxy.entrypoints;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.runtime.Settings;

import dev.aoqia.leaf.proxy.Constants;
import dev.aoqia.leaf.proxy.Main;
import dev.aoqia.leaf.proxy.Utils;
import dev.aoqia.leaf.proxy.json.InstallerJson;

import static dev.aoqia.leaf.proxy.Utils.log;

public class ClientEntrypoint {
    private static final DslJson<Object> JSON = new DslJson<>(Settings.basicSetup());

    static void main(final String[] args) {
        try {
            final File proxyJar = getProxyJarPath();
            Utils.initLog(proxyJar.getParentFile());
            log("Initialised logging for leaf-loader-proxy.");
            log("Proxy args: " + Arrays.toString(Main.args.toArray()));

            final File gameJar = proxyJar.toPath().getParent().resolve("projectzomboid.jar").toFile();

            // If there's a better way to not hardcode this ID...
            final File[] loaderJars = getLoaderJars(proxyJar, "3776625738");

            final URL[] urls = new URL[loaderJars.length + 1];
            for (int i = 0; i < loaderJars.length; ++i) {
                urls[i] = loaderJars[i].toURI().toURL();
            }
            urls[urls.length - 1] = gameJar.toURI().toURL();

            // Remove the proxy jar from the classpath ASAP
            Utils.removeFromClasspath(s -> s.endsWith(proxyJar.getName()));

            Utils.addToClasspath(Arrays.stream(urls).map(url -> {
                try {
                    return new File(url.toURI()).getAbsolutePath();
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }).toArray(String[]::new));
            log("Classpath is currently: " + System.getProperty(Constants.Properties.CLASS_PATH));

            try (final URLClassLoader cl = new URLClassLoader(urls,
                ClientEntrypoint.class.getClassLoader().getParent())) {
                log("Classloader was created successfully.");

                final Class<?> entrypoint = Class.forName(getLoaderEntrypoint(cl), true, cl);
                final Method entryMethod = entrypoint.getMethod("main", String[].class);

                log("Invoking loader entrypoint, have fun!");
                try {
                    entryMethod.invoke(null, (Object) args);
                } catch (InvocationTargetException e) {
                    log("Failed on thread " + Thread.currentThread().getName() + " with TCCL "
                        + Thread.currentThread().getContextClassLoader());
                    log(e);
                    throw e;
                }
            }
        } catch (Exception e) {
            Utils.closeLog();
            throw new RuntimeException(e);
        }
    }

    private static File getProxyJarPath() throws URISyntaxException {
        return new File(ClientEntrypoint.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    private static File[] getLoaderJars(final File proxyJar, final String workshopId) {
        log("Attempting to get loader jars...");

        final Path jarDir;
        if (Main.args.containsKey("loaderPath")) {
            jarDir = Path.of(Main.args.get("loaderPath"));
        } else {
            final Path workshopPath = getWorkshopPath(proxyJar);
            final Path modPath = workshopPath.resolve(workshopId, "mods", "LeafLoader");
            jarDir = modPath.resolve("common", "media", "java");
        }

        final File[] jars = jarDir
            .toFile()
            .listFiles((_, name) -> name.endsWith(".jar") && !name.endsWith("-sources.jar"));

        if (jars == null || jars.length == 0) {
            final var e = new RuntimeException("Loader JAR doesn't exist in the workshop mod");
            log(e);
            throw e;
        }

        return jars;
    }

    private static Path getWorkshopPath(final File proxyJar) {
        Path gamePath = proxyJar.toPath().getParent();
        if ("projectzomboid".equals(gamePath.getFileName().toString())) {
            gamePath = gamePath.getParent();
        }

        return gamePath.getParent().getParent().resolve("workshop", "content", "108600");
    }

    private static String getLoaderEntrypoint(final URLClassLoader cl) throws IOException {
        byte[] jsonBytes;
        try (InputStream is = cl.getResourceAsStream("leaf-installer.json")) {
            if (is == null) {
                final var e = new RuntimeException("leaf-installer.json doesn't exist in the loader JAR!");
                log(e);
                throw e;
            }

            jsonBytes = is.readAllBytes();
        }

        final InstallerJson installerJson = JSON.deserialize(InstallerJson.class, jsonBytes, jsonBytes.length);
        if (installerJson == null) {
            final var e = new RuntimeException("Failed to deserialize installer json");
            log(e);
            throw e;
        }

        return installerJson.mainClass().client();
    }
}
