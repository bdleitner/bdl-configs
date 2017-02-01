package com.bdl.config.annotation.processor;

import com.bdl.config.Config;
import com.bdl.config.Configurable;

/**
 * Container class with some @Config fields for testing.
 *
 * @author Ben Leitner
 */
class ConfigContainer {

  @Config private static final Configurable<String> privateString = Configurable.flag("foo");

  @Config(name = "specified_name")
  @Annotations.DummyQualifier("foo")
  static final Configurable<Integer> packageInteger = Configurable.flag(5);

  @Config
  @Annotations.DummyQualifier
  @Annotations.DummyBindingAnnotation("blah")
  public static final Configurable<Boolean> publicBoolean = Configurable.flag(true);

  @Config
  private static final Configurable<String> noDefaultString =
      Configurable.noDefaultFlag(String.class);
}
