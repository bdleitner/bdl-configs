package com.bdl.config.compiler;

import com.bdl.config.Config;
import com.bdl.config.Configurable;

/**
 * Dummy target class to create modules for the configs within.
 *
 * @author Ben Leitner
 */
public class GuiceProcessorTarget {

  @Config(name = "here_is_a_config", desc = "A dummy config for the annotation processor")
  public static final Configurable<String> HERE_IS_A_CONFIGURABLE = Configurable.value("foo");

  @Config(name = "here_is_a_null_config", desc = "A dummy config for the annotation processor")
  public static final Configurable<String> HERE_IS_A_NULL_CONFIGURABLE =
      Configurable.noDefault(String.class);
}
