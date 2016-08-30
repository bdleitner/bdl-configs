package com.bdl.config;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * Binding Annotation / Qualifier for use in binding config values for Dependency Injection.
 *
 * @author Ben Leitner
 */
@BindingAnnotation
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface ConfigValue {
  String value();
}
