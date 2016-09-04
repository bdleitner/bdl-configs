package com.bdl.config;

/**
 * Base exception class for runtime exceptions that may arise when working with configs.
 *
 * @author Benjamin Leitner
 */
public class ConfigRuntimeException extends RuntimeException {

  /** For serialization, derived by casting the start of the class name into numbers. */
  private static final long serialVersionUID = 6121752435L;

  ConfigRuntimeException(ConfigException ex) {
    super(ex);
  }

  /** Returns the underlying {@link ConfigException}. */
  public ConfigException unwrap() {
    return ((ConfigException) getCause());
  }

}
