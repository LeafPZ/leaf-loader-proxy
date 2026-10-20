package dev.aoqia.leaf.proxy.entrypoints;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;

import dev.aoqia.leaf.proxy.Constants;
import dev.aoqia.leaf.proxy.GameEnv;
import dev.aoqia.leaf.proxy.Main;
import dev.aoqia.leaf.proxy.PathUtils;
import dev.aoqia.leaf.proxy.Utils;

public abstract class EntrypointHook {
    public static void run(String[] args, GameEnv env) {
        try {
            assert Main.args != null;
            File proxyJar = PathUtils.proxyJar();
            Path gameJar = PathUtils.gameJar();

            // Try init again just to be sure
            Utils.initLog(gameJar.getParent());

            Utils.log("Initialised logging for leaf-loader-proxy!");
            Utils.log("Working directory: " + PathUtils.workingDir());
            Utils.log("Proxy version: " + Constants.VERSION);
            Utils.log("Proxy JAR: " + proxyJar);
            Utils.log("Proxy args: " + Arrays.toString(Main.args.toArray()));
            Utils.log("Game JAR: " + gameJar);
            Utils.log("Game args: " + Arrays.toString(args));
            Utils.log("Classpath: " + System.getProperty(Constants.Properties.CLASS_PATH));

            File[] loaderJars = PathUtils.getLoaderJars(gameJar);

            URL[] urls = new URL[loaderJars.length + 1];
            for (int i = 0; i < loaderJars.length; ++i) {
                urls[i] = loaderJars[i].toURI().toURL();
            }
            // TODO: do we really need the game jar on this path?
            urls[urls.length - 1] = gameJar.toUri().toURL();

            // Remove the proxy jar from the classpath just in case
            Utils.removeFromClasspath(s -> s.endsWith(proxyJar.getName()));

            Utils.addToClasspath(Arrays.stream(urls).map(url -> {
                try {
                    return new File(url.toURI()).getAbsolutePath();
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }).toArray(String[]::new));

            try (URLClassLoader cl = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader())) {
                Utils.log("Classloader was created with classpath: " + Arrays.toString(cl.getURLs()));

                Class<?> entrypoint = Class.forName(PathUtils.getLoaderEntrypoint(cl, env), true, cl);
                Method entryMethod = entrypoint.getMethod("main", String[].class);

                Utils.log("Invoking loader entrypoint, have fun!");
                try {
                    // Thread.currentThread().setContextClassLoader(cl);
                    entryMethod.invoke(null, (Object) args);
                } catch (Exception e) {
                    Utils.log(String.format("Failed on thread %s with TCCL %s", Thread.currentThread().getName(),
                        Thread.currentThread().getContextClassLoader().toString()));
                    Utils.log(e);
                    throw e;
                }
            }
        } catch (Exception e) {
            Utils.closeLog();
            throw new RuntimeException(e);
        }
    }
}
