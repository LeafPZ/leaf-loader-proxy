package dev.aoqia.leaf.proxy.entrypoints;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import dev.aoqia.leaf.proxy.PathUtils;
import dev.aoqia.leaf.proxy.Utils;
import dev.aoqia.leaf.proxy.json.InstallerJson;

import static dev.aoqia.leaf.proxy.Utils.log;

public class EntrypointHook {
    private static final DslJson<Object> JSON = new DslJson<>(Settings.basicSetup());

    static void main(String[] args) {
        assert Main.args != null;

        try {
            File proxyJar = PathUtils.proxyJar();
            Path gameJar = PathUtils.gameJar(EntrypointHook.class);

            // Try init again just to be sure
            Utils.initLog(gameJar.getParent());

            log("Initialised logging for leaf-loader-proxy");
            log("Proxy version: " + Constants.VERSION);
            log("Proxy JAR: " + proxyJar);
            log("Proxy args: " + Arrays.toString(Main.args.toArray()));
            log("Game JAR: " + gameJar);

            File[] loaderJars = getLoaderJars(gameJar);

            URL[] urls = new URL[loaderJars.length + 1];
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

            try (URLClassLoader cl = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader())) {
                log("Classloader was created successfully.");

                Class<?> entrypoint = Class.forName(getLoaderEntrypoint(cl), true, cl);
                Method entryMethod = entrypoint.getMethod("main", String[].class);

                log("Invoking loader entrypoint, have fun!");
                try {
                    entryMethod.invoke(null, (Object) args);
                } catch (Exception e) {
                    log(String.format("Failed on thread %s with TCCL %s", Thread.currentThread().getName(),
                        Thread.currentThread().getContextClassLoader().toString()));
                    log(e);
                    throw e;
                }
            }
        } catch (Exception e) {
            Utils.closeLog();
            throw new RuntimeException(e);
        }
    }
}
