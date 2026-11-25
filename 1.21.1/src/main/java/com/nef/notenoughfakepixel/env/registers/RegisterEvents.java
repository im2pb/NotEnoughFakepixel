package com.nef.notenoughfakepixel.env.registers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Discovers a client initializer whose onInitializeClient method attaches Fabric event listeners. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterEvents {
}
