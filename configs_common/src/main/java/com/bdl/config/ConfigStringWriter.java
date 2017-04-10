package com.bdl.config;

/**
 * An interface for classes that write out existing configuration information.
 *
 * @author Ben Leitner
 */
public interface ConfigStringWriter {

  /** Writes the config name-value pair. */
  void write(String key, String value);
}
