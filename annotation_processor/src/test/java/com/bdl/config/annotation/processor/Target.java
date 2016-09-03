package com.bdl.config.annotation.processor;

import com.bdl.config.Config;
import com.bdl.config.Configurable;

/**
 * TODO: JavaDoc this class.
 *
 * @author Ben Leitner
 */
public class Target {

  @Config
  @Annotations.DummyQualifier("some_config")
  public static Configurable<String> stringFlag = Configurable.flag("default");
}
