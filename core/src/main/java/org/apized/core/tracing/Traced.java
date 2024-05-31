package org.apized.core.tracing;

import io.micronaut.aop.Around;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Around
@Inherited
public @interface Traced {
  String value() default "";

  TraceKind kind() default TraceKind.INTERNAL;

  Attribute[] attributes() default {};

  @interface Attribute {
    String key();
    String value() default "";
    String arg() default "";
  }
}
