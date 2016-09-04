package com.bdl.config;

import com.google.common.base.Joiner;

/**
 * Base exception class for exceptions that may arise when working with configs.
 *
 * @author Benjamin Leitner
 */
public class ConfigException extends Exception {

  /** Interface for exceptions that can have a config name set on them. */
  interface ConfigNameSettable {
    ConfigException withConfigName(String name);
  }

  /** For serialization, derived by casting the start of the class name into numbers */
  private static final long serialVersionUID = 6121752435L;

  /** Creates a new {@link ConfigException} with the given message */
  ConfigException(String message) {
    super(message);
  }

  /** Creates a new {@link ConfigException} with the given message and cause */
  ConfigException(String message, Throwable cause) {
    super(message, cause);
  }

  /** Wraps this exception in a {@link ConfigRuntimeException}. */
  public ConfigRuntimeException wrap() {
    return new ConfigRuntimeException(this);
  }

  /** An exception thrown when an attempt to load configs from an external source fails */
  public static class ExternalConfigLoadException extends ConfigException {

    /** For serialization, derived by casting the start of the class name into numbers */
    private static final long serialVersionUID = 52420518141126121L;

    ExternalConfigLoadException(String arg) {
      super(arg);
    }

    ExternalConfigLoadException(String arg, Throwable cause) {
      super(arg, cause);
    }
  }

  /** Exception thrown to indicate that a config specification is malformed */
  public static class InvalidConfigSyntaxException extends ConfigException {

    /** For serialization, derived by casting the start of the class name into numbers. */
    private static final long serialVersionUID = 91422112946121719L;

    InvalidConfigSyntaxException(String arg) {
      super(String.format("Invalid config syntax: %s", arg));
    }
  }

  /** An exception thrown when a config is not recognized */
  public static class UnrecognizedConfigException extends ConfigException {

    /** For serialization, derived by casting the start of the class name into numbers */
    private static final long serialVersionUID = 21141853157149L;

    UnrecognizedConfigException(String... configNames) {
      super(String.format("The config(s) \"%s\" are not recognized",
          Joiner.on(", ").join(configNames)));
    }
  }

  /** An exception thrown when a requested config cannot be uniquely determined */
  public static class AmbiguousConfigException extends ConfigException {

    /** For serialization, derived by casting the start of the class name into numbers */
    private static final long serialVersionUID = 11329721152119L;

    AmbiguousConfigException(String name, Iterable<String> fullNames) {
      super(String.format(
          "Ambiguous config name \"%s\" matches configs: %s.  Try using the fully qualified name.",
          name,
          Joiner.on(", ").join(fullNames)));
    }
  }

  /** An exception that is thrown if the config cannot be set. */
  public static class IllegalConfigStateException extends ConfigException implements ConfigNameSettable {

    /** For serialization, derived by casting the start of the class name into numbers. */
    private static final long serialVersionUID = 9121257112612171920L;

    private final String configName;

    IllegalConfigStateException() {
      this(null);
    }

    IllegalConfigStateException(String configName) {
      super(null);
      this.configName = configName;
    }

    @Override
    public ConfigException withConfigName(String name) {
      return new IllegalConfigStateException(name);
    }

    @Override
    public String getMessage() {
      if (configName == null) {
        return "Attempted to set the value of a config after it was read.";
      }
      return String.format("Attempted to set the value of the config %s after it was read.", configName);
    }
  }

  /**
   * An exception that is thrown if the string value cannot be parsed to give a valid value for the
   * config.
   */
  public static class InvalidConfigValueException extends ConfigException implements ConfigNameSettable {

    /** For serialization, derived by casting the start of the class name into numbers. */
    private static final long serialVersionUID = 914221129461217221L;

    private final String value;
    private final String configName;

    InvalidConfigValueException(String value) {
      this(null, value);
    }

    InvalidConfigValueException(String configName, String value) {
      super(null);
      this.configName = configName;
      this.value = value;
    }

    @Override
    public ConfigException withConfigName(String name) {
      return new InvalidConfigValueException(name, value);
    }

    @Override
    public String getMessage() {
      if (configName == null) {
        return String.format("The value \"%s\" is not valid", value);
      }
      return String.format("The value \"%s\" is not valid for config \"%s\"", value, configName);
    }
  }

  /**
   * An exception thrown when an attempt is made to update a {@link Configurable} to an incompatible
   * value.
   */
  public static class TypeMismatchException extends ConfigException {

    TypeMismatchException(Object oldValue, Object newValue) {
      super(String.format("Attempted to set configurable to incompatible value %s (was %s).",
          newValue, oldValue));
    }

    TypeMismatchException(Throwable cause) {
      super("A configuration type mismatch has occurred.", cause);
    }
  }
}
