package com.bdl.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation marking a field as a config, for use with config processing.
 *
 * @author Benjamin Leitner
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Config {

  /**
   * The (short) name of the config on the command line if it should be different from the name of
   * the field.
   */
  String name() default "";

  /** A description of the config for use in reporting problems or missing config values */
  String desc() default "";
}
