package com.bdl.config.compiler.subpkg;

import com.bdl.config.Config;
import com.bdl.config.Configurable;

/**
 * Outer class for testing subpackage configs.
 *
 * @author Ben Leitner
 */
public class Outer {

  public static class Middle {

    public static class Inner {

      @Config(name = "deep_nested_private")
      private static final Configurable<String> DEEP_NESTED_PRIVATE = Configurable.value("foo");

      @Config(name = "deep_nested_package")
      static final Configurable<String> DEEP_NESTED_PACKAGE = Configurable.value("foo");

      @Config(name = "deep_nested_public")
      public static final Configurable<String> DEEP_NESTED_PUBLIC = Configurable.value("foo");
    }
  }
}
