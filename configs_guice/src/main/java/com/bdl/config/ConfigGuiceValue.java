package com.bdl.config;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binding Annotation / Qualifier for use in binding config values for Dependency Injection.
 *
 * @author Ben Leitner
 */
@BindingAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface ConfigGuiceValue {
  String value();
}
