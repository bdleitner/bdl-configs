package com.bdl.config;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Utility functions for working with {@link Configurable} objects
 *
 * @author Benjamin Leitner
 */
@Singleton
public class Configuration {

  private static final AtomicBoolean STATE_CHECKING_DISABLED = new AtomicBoolean(false);

  private final ConfigMap configs;

  @Inject
  Configuration(ConfigMap configs) {
    this.configs = configs;
  }

  /** Disables config state checking when attempting to set a config value. Useful for writing tests. */
  public static void disableConfigSetCheck() {
    STATE_CHECKING_DISABLED.set(true);
  }

  /** Enables config state checking when attempting to set a config value. */
  public static void enableConfigSetCheck() {
    STATE_CHECKING_DISABLED.set(false);
  }

  /** Returns {@code true} if config write checking is disabled, {@code false} otherwise. */
  public static boolean isConfigSetCheckDisabled() {
    return STATE_CHECKING_DISABLED.get();
  }

  /**
   * Obtain the current configurable value by name.
   *
   * @throws ConfigException if no such configurable exists.
   */
  public Object get(String name) throws ConfigException {
    return configs.getOrThrow(name).get();
  }
}
