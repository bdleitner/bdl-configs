package com.bdl.config;

/**
 * Base exception class for runtime exceptions that may arise when working with configs.
 *
 * @author Benjamin Leitner
 */
public class ConfigRuntimeException extends RuntimeException {

  /** For serialization, derived by casting the start of the class name into numbers */
  private static final long serialVersionUID = 6121752435L;

  /** Creates a new {@link ConfigException} with the given message */
  public ConfigRuntimeException(String message) {
    super(message);
  }

  /**
   * An exception that is thrown if the string value cannot be parsed to give a valid value for the
   * config.
   */
  public static class InvalidConfigValueException extends ConfigRuntimeException {

    /** For serialization, derived by casting the start of the class name into numbers. */
    private static final long serialVersionUID = 914221129461217221L;

    private final String value;
    private String configName;

    public InvalidConfigValueException(String value) {
      this(null, value);
    }

    public InvalidConfigValueException(String configName, String value) {
      super(null);
      this.configName = configName;
      this.value = value;
    }

    public void setConfigName(String configName) {
      this.configName = configName;
    }

    @Override
    public String getMessage() {
      if (configName == null) {
        return String.format("The value \"%s\" is not valid", value);
      }
      return String.format("The value \"%s\" is not valid for config \"%s\"", value, configName);
    }
  }

  /** An exception that is thrown if the config cannot be set. */
  public static class IllegalConfigStateException extends ConfigRuntimeException {

    /** For serialization, derived by casting the start of the class name into numbers. */
    private static final long serialVersionUID = 9121257112612171920L;

    private String configName;

    public IllegalConfigStateException() {
      this(null);
    }

    public IllegalConfigStateException(String configName) {
      super(null);
      this.configName = configName;
    }

    public void setConfigName(String configName) {
      this.configName = configName;
    }

    @Override
    public String getMessage() {
      if (configName == null) {
        return "Attempted to set the value of a config after it was read.";
      }
      return String.format("Attempted to set the value of the config %s after it was read.", configName);
    }
  }

  /** An exception thrown when a requested config cannot be uniquely determined */
  public static class AmbiguousConfigException extends ConfigRuntimeException {

    /** For serialization, derived by casting the start of the class name into numbers */
    private static final long serialVersionUID = 11329721152119L;

    public AmbiguousConfigException(ConfigDescription first, ConfigDescription second) {
      super(String.format(
          "Duplicate config name found.  The name %s refers to both %s.%s and %s.%s." +
              "  Inclusion of both via modules is not allowed.",
          first.name(),
          first.className(),
          first.fieldName(),
          second.className(),
          second.fieldName()));
    }
  }

  /**
   * An exception thrown when an attempt is made to update a {@link Configurable} to an incompatible
   * value.
   */
  public static class ConfigTypeMismatchException extends ConfigRuntimeException {

    public ConfigTypeMismatchException(Object oldValue, Object newValue) {
      super(String.format("Attempted to set configurable to incompatible value %s (was %s).",
          newValue, oldValue));
    }
  }
}
