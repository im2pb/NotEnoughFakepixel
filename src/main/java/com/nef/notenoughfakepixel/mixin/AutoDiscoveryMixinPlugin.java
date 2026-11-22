/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.mixin;

import lombok.Getter;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AutoDiscoveryMixinPlugin implements IMixinConfigPlugin {
    @Getter private static final List<AutoDiscoveryMixinPlugin> mixinPlugins = new ArrayList<>();
    @Getter private String mixinPackage;

    @Override
    public void onLoad(String mixinPackage) {
        this.mixinPackage = mixinPackage;
        mixinPlugins.add(this);
    }

    public URL getBaseUrlForClassUrl(URL classUrl) {
        String string = classUrl.toString();
        if (classUrl.getProtocol().equals("jar")) {
            try {
                return new URL(string.substring(4).split("!")[0]);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        if (string.endsWith(".class")) {
            try {
                return new URL(string.replace("\\", "/")
                        .replace(getClass().getCanonicalName()
                                .replace(".", "/") + ".class", ""));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return classUrl;
    }

    public String getMixinBaseDir() {
        return mixinPackage.replace(".", "/");
    }

    private List<String> mixins = null;

    public void tryAddMixinClass(String className) {
        if (!className.endsWith(".class")) return; 

        String norm = className.substring(0, className.length() - ".class".length())
                .replace("\\", "/")
                .replace("/", ".");

        String pluginName = getClass().getName();
        if (norm.equals(pluginName)) return;

        if (norm.startsWith(getMixinPackage() + ".") && !norm.endsWith(".")) {
            mixins.add(norm.substring(getMixinPackage().length() + 1));
        }
    }

    @Override
    public List<String> getMixins() {
        if (mixins != null) return mixins;
        System.out.println("Trying to discover mixins");
        mixins = new ArrayList<>();
        URL classUrl = getClass().getProtectionDomain().getCodeSource().getLocation();
        System.out.println("Found classes at " + classUrl);
        Path file;
        try {
            file = Paths.get(getBaseUrlForClassUrl(classUrl).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Base directory found at " + file);
        if (Files.isDirectory(file)) {
            walkDir(file);
        } else {
            walkJar(file);
        }
        System.out.println("Found mixins: " + mixins);

        return mixins;
    }

    private void walkDir(Path classRoot) {
        System.out.println("Trying to find mixins from directory");
        try (Stream<Path> classes = Files.walk(classRoot.resolve(getMixinBaseDir()))) {
            classes.map(it -> classRoot.relativize(it).toString())
                    .forEach(this::tryAddMixinClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void walkJar(Path file) {
        System.out.println("Trying to find mixins from jar file");
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(file))) {
            ZipEntry next;
            while ((next = zis.getNextEntry()) != null) {
                tryAddMixinClass(next.getName());
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }
}
