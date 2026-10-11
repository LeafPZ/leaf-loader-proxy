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
import dev.aoqia.leaf.proxy.ProxyPaths;
import dev.aoqia.leaf.proxy.Utils;
import dev.aoqia.leaf.proxy.json.InstallerJson;

import static dev.aoqia.leaf.proxy.Utils.log;

public class ClientEntrypoint {
    private static final DslJson<Object> JSON = new DslJson<>(Settings.basicSetup());
    public static final String WORKSHOP_ID = "3776625738";

    static void main(final String[] args) {
        try {
            final File proxyJar = getProxyJarPath();
            final Path gameJar = ProxyPaths.gameJar(proxyJar.toPath(), Path.of(""));
            Utils.initLog(gameJar.getParent().toFile());
            log("Initialised logging for leaf-loader-proxy.");
            log("Proxy args: " + Arrays.toString(Main.args.toArray()));

            log("Proxy JAR: " + proxyJar);
            log("Game JAR: " + gameJar);
            if (System.getProperty("leaf.gameJarPath") == null
                && System.getProperty("leaf.gameJarPath.client") == null) {
                System.setProperty("leaf.gameJarPath", gameJar.toString());
            }
            log("Leaf common game JAR: " + System.getProperty("leaf.gameJarPath"));
            log("Leaf client game JAR: " + System.getProperty("leaf.gameJarPath.client"));

            // If there's a better way to not hardcode this ID...
            final File[] loaderJars = getLoaderJars(gameJar);

            final URL[] urls = new URL[loaderJars.length + 1];
            for (int i = 0; i < loaderJars.length; ++i) {
                urls[i] = loaderJars[i].toURI().toURL();
            }
            urls[urls.length - 1] = gameJar.toUri().toURL();

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

            final URLClassLoader cl = new URLClassLoader(urls,
                ClassLoader.getPlatformClassLoader());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    cl.close();
                } catch (IOException e) {
                    e.printStackTrace(System.err);
                }
            }, "leaf-proxy-classloader-close"));
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
        } catch (Exception e) {
            Utils.closeLog();
            throw new RuntimeException(e);
        }
    }

    private static File getProxyJarPath() throws URISyntaxException {
        return new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    private static File[] getLoaderJars(final Path gameJar) throws IOException {
        log("Attempting to get loader jars...");
        final Path jarDir = ProxyPaths.loaderDirectory(gameJar, Main.args.get("loaderPath"));
        log("Loader directory: " + jarDir);
        return ProxyPaths.loaderJars(jarDir).stream().map(Path::toFile).toArray(File[]::new);
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
