package com.nef.notenoughfakepixel.env.registers;

import net.fabricmc.loader.api.ModContainer;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

final class RegistrationScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("notenoughfakepixel");
    private static final String MOD_PACKAGE = "com/nef/notenoughfakepixel";
    private static final Set<String> TYPE_ANNOTATIONS = Set.of(
            Type.getDescriptor(RegisterEvents.class), Type.getDescriptor(RegisterCommand.class));
    private static final Set<String> FIELD_ANNOTATIONS = Set.of(
            Type.getDescriptor(RegisterInstance.class), Type.getDescriptor(RegisterKeybind.class));

    private RegistrationScanner() {
    }

    static List<String> findAnnotatedClasses(ModContainer mod) {
        Set<String> classes = new TreeSet<>();
        for (Path root : mod.getRootPaths()) {
            Path ownClasses = root.resolve(MOD_PACKAGE);
            if (!Files.isDirectory(ownClasses)) {
                continue;
            }
            Path dependencies = ownClasses.resolve("deps");
            try (var files = Files.walk(ownClasses)) {
                files.filter(path -> path.getFileName().toString().endsWith(".class"))
                        .filter(path -> !path.startsWith(dependencies))
                        .forEach(path -> readClass(path, classes));
            } catch (IOException | UncheckedIOException error) {
                LOGGER.error("Could not scan {} for registration annotations", ownClasses, error);
            }
        }
        return List.copyOf(classes);
    }

    // Read annotations without linking classes that might use optional mod APIs.
    private static void readClass(Path file, Set<String> classes) {
        try (var input = Files.newInputStream(file)) {
            ClassReader reader = new ClassReader(input);
            String className = reader.getClassName().replace('/', '.');
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    if (visible && TYPE_ANNOTATIONS.contains(descriptor)) {
                        classes.add(className);
                    }
                    return null;
                }

                @Override
                public FieldVisitor visitField(int access, String name, String descriptor,
                                               String signature, Object value) {
                    if (classes.contains(className)) {
                        return null;
                    }
                    return new FieldVisitor(Opcodes.ASM9) {
                        @Override
                        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                            if (visible && FIELD_ANNOTATIONS.contains(descriptor)) {
                                classes.add(className);
                            }
                            return null;
                        }
                    };
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (IOException | IllegalArgumentException error) {
            LOGGER.warn("Could not read registration annotations from {}", file, error);
        }
    }
}
