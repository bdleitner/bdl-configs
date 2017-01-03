package com.bdl.config;

import com.google.auto.value.AutoAnnotation;

/**
 * Utility class for working with configurations.
 *
 * @author Ben Leitner
 */
public class Configs {

  @AutoAnnotation
  public static ConfigValue configValue(String value) {
    return new AutoAnnotation_Configs_configValue(value);
  }
}
