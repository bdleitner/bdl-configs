package com.bdl.config.compiler;

import com.bdl.config.Configurable;
import com.bdl.config.Config;

/**
 * TODO: JavaDoc this class.
 *
 * @author Ben Leitner
 */
public class ProcessorTarget {

  @Config(name = "here_is_a_config", desc = "A dummy config for the annotation processor")
  public static final Configurable<String> HERE_IS_A_CONFIGURABLE = Configurable.value("foo");

}
