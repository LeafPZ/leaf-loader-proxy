<div align="center">

**A loader that loads [leaf-loader][LeafLoader] from the Workshop. Ironic?!**

![License](https://img.shields.io/github/license/LeafPZ/leaf-loader-proxy?label=License)
![Gradle version](https://img.shields.io/badge/Gradle-9.7.1-teal?logo=gradle)
![Build status](https://github.com/LeafPZ/leaf-loader-proxy/actions/workflows/build.yml/badge.svg?branch=main&label=build)
![Downloads](https://img.shields.io/github/downloads/LeafPZ/leaf-loader-proxy/total?label=Downloads)
![Code Size](https://img.shields.io/github/languages/code-size/LeafPZ/leaf-loader-proxy?label=Code%20Size)
![Maven status](https://img.shields.io/website?url=https%3A%2F%2Fmaven.aoqia.dev%2F&label=Maven)

</div>

The purpose of the proxy is to load the loader. Essentially, it discovers the loader JAR and all of its dependencies
from the [leaf-loader][LeafLoaderWorkshop] Workshop mod and will load them instead of the game directly.

It allows for one major thing to happen: the loader is able to load dynamically, allowing for automatic loader updates
directly from the workshop. This provides the most convenience and ease-of-use as compared to manual installation.

### Requirements

- Java 25 or higher

### Installation

To install the proxy, you should be using [leaf-installer][LeafInstaller]. If you want to install it purely for testing
in-dev, you can run the `copyToGame` Gradle task.

### Usage

The installer will help you with the following, but if you wish, you can do it manually:

The proxy jar should be placed in the game folder under the `.leaf/lib/` path. You can allow the game to use the proxy
by adding `-javaagent:.leaf/lib/loader-proxy:0.3.0.jar` to the game's launch options. The version number in this example
may not be the same as the version of your installed proxy, so change it if needed.

If you need help with this, you should read [Startup Parameters][StartupParameters].

### Configuration

You may want to provide arguments to the proxy. The format for these are `-arg123` for single args and `-key=value` for
value args. Arguments are separated with a semicolon `;`. The table for all available arguments is below.

| Argument     | Type   | Example                        |
|--------------|--------|--------------------------------|
| `loaderPath` | Single | `-loaderPath="path/to/loader"` |

### Development

You can build the project like so:

```shell
./gradlew build
```

[LeafLoader]: https://github.com/LeafPZ/leaf-loader
[LeafLoaderWorkshop]: https://steamcommunity.com/sharedfiles/filedetails/?id=3776625738
[LeafInstaller]: https://github.com/LeafPZ/leaf-installer
[StartupParameters]: https://pzwiki.net/wiki/Startup_parameters
