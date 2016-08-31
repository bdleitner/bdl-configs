package com.bdl.config;

import com.google.common.base.Joiner;

/**
 * Base exception class for exceptions that may arise when working with configs.
 *
 * @author Benjamin Leitner
 */
public class ConfigException extends Exception {

  /** For serialization, derived by casting the start of the class name into numbers */
  private static final long serialVersionUID = 6121752435L;

  /** Creates a new {@link ConfigException} with the given message */
  public ConfigException(String message) {
    super(message);
  }

  /** Creates a new {@link ConfigException} with the given message and cause */
  public ConfigException(String message, Throwable cause) {
    super(message, cause);
  }

  /** An exception thrown when a config is not recognized */
  public static class UnrecognizedConfigException extends ConfigException {

    /** For serialization, derived by casting the start of the class name into numbers */
    private static final long serialVersionUID = 21141853157149L;

    public UnrecognizedConfigException(String... configNames) {
      super(String.format("The config(s) \"%s\" are not recognized",
          Joiner.on(", ").join(configNames)));
    }
  }

  /** An exception thrown when an attempt to load configs from an external source fails */
  public static class ExternalConfigLoadException extends ConfigException {

    /** For serialization, derived by casting the start of the class name into numbers */
    private static final long serialVersionUID = 52420518141126121L;

    public ExternalConfigLoadException(String arg) {
      super(arg, null);
    }

  }

  /** Exception thrown to indicate that a config specification is malformed */
  public static class InvalidConfigSyntaxException extends ConfigException {

    /** For serialization, derived by casting the start of the class name into numbers. */
    private static final long serialVersionUID = 91422112946121719L;

    public InvalidConfigSyntaxException(String arg) {
      super(String.format("Invalid config syntax: %s", arg));
    }
  }


}
