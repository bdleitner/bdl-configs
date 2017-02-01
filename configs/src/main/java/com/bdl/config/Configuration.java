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

  /**
   * Disables config state checking when attempting to set a config value. Useful for writing tests.
   */
  public static void disableConfigSetCheck() {
    STATE_CHECKING_DISABLED.set(true);
  }

  /** Enables config state checking when attempting to set a config value. */
  public static void enableConfigSetCheck() {
    STATE_CHECKING_DISABLED.set(false);
  }

  /** Returns {@code true} if config write checking is disabled, {@code false} otherwise. */
  static boolean isConfigSetCheckDisabled() {
    return STATE_CHECKING_DISABLED.get();
  }

  /**
   * Obtain the current configurable value by name.
   *
   * @throws ConfigException if an error occurs:
   *     <ul>
   *     <li> {@link ConfigException.UnrecognizedConfigException} if no configurable matches the
   *         given name.
   *     <li> {@link ConfigException.AmbiguousConfigException} if more than one configurable matches
   *         the given name.
   *     </ul>
   */
  public Object get(String name) throws ConfigException {
    return configs.getOrThrow(name).get();
  }

  /**
   * Updates the configurable identified with the given name to the given value.
   *
   * @throws ConfigException if an error occurs:
   *     <ul>
   *     <li> {@link ConfigException.UnrecognizedConfigException} if no configurable matches the
   *         given name.
   *     <li> {@link ConfigException.AmbiguousConfigException} if more than one configurable matches
   *         the given name.
   *     <li> {@link ConfigException.TypeMismatchException} if the given value does not have the
   *         correct type.
   *     <li> {@link ConfigException.IllegalConfigStateException} if the configurable is not in a
   *         writable state.
   *     </ul>
   */
  @SuppressWarnings("unchecked") // If types don't match, a ConfigException is thrown.
  public <T, S extends T> T update(String name, S newValue) throws ConfigException {
    // Must cast to raw type, since we can't pass an Object to a ?.
    return ((Configurable<T>) configs.getOrThrow(name)).setValue(newValue);
  }

  /**
   * Updates the configurable identified with the given name to the given value.
   *
   * @return the old value
   * @throws ConfigException if an error occurs:
   *     <ul>
   *     <li> {@link ConfigException.UnrecognizedConfigException} if no configurable matches the
   *         given name.
   *     <li> {@link ConfigException.AmbiguousConfigException} if more than one configurable matches
   *         the given name.
   *     <li> {@link ConfigException.TypeMismatchException} if the given value does not have the
   *         correct type.
   *     <li> {@link ConfigException.IllegalConfigStateException} if the configurable is not in a
   *         writable state.
   *     </ul>
   */
  public Object updateAsString(String name, String newValueAsString) throws ConfigException {
    return configs.getOrThrow(name).setFromString(newValueAsString);
  }

  /**
   * Resets the configurable with the given name to its default value.
   *
   * @throws ConfigException if an error occurs:
   *     <ul>
   *     <li> {@link ConfigException.UnrecognizedConfigException} if no configurable matches the
   *         given name.
   *     <li> {@link ConfigException.AmbiguousConfigException} if more than one configurable matches
   *         the given name.
   *     <li> {@link ConfigException.IllegalConfigStateException} if the configurable is not in a
   *         writable state.
   *     </ul>
   */
  public Object reset(String name) throws ConfigException {
    return configs.getOrThrow(name).reset();
  }

  /** Resets all registered configurables that are not currently read-only. */
  public void resetAllWritable() {
    for (Configurable<?> configurable : configs.allConfigurables()) {
      if (!configurable.isReadOnly()) {
        try {
          configurable.reset();
        } catch (ConfigException ex) {
          // Should never happen, since we checked for writability.
          throw ex.wrap();
        }
      }
    }
  }

  /**
   * Registers a listener to the configuration with the given name.
   *
   * @param name the name of the config to which to attach the listener.
   * @param listener the listener to attach.
   * @throws ConfigException if an error occurs:
   *     <ul>
   *     <li>{@link ConfigException.UnrecognizedConfigException} if no configurable matches the
   *         given name.
   *     <li>{@link ConfigException.AmbiguousConfigException} if more than one configurable matches
   *         the given name.
   *     <li>{@link ConfigException.TypeMismatchException} if there is a mismatch between
   *         configurable and listener types.
   *     </ul>
   */
  public <T> ConfigChangeListener.ListenerRegistration registerListener(
      String name, ConfigChangeListener<T> listener) throws ConfigException {
    return registerListener(name, listener, false);
  }

  /**
   * Registers a listener to the configuration with the given name.
   *
   * <p>Note, no type-checking is performed here, so it is possible to register a listener to a
   * configurable of a non-matching type. This could cause exceptions down the road.
   *
   * @param name the name of the config to which to attach the listener.
   * @param listener the listener to attach.
   * @param listen if {@code true} an {@link ConfigChangeListener#onConfigurationChange(Object)}
   *     message is sent immediately with the configuration's current value.
   * @throws ConfigException if an error occurs:
   *     <ul>
   *     <li>{@link ConfigException.UnrecognizedConfigException} if no configurable matches the
   *         given name.
   *     <li>{@link ConfigException.AmbiguousConfigException} if more than one configurable matches
   *         the given name.
   *     </ul>
   */
  @SuppressWarnings("unchecked") // Can't stop it here, but may cause exceptions elsewhere.
  public <T> ConfigChangeListener.ListenerRegistration registerListener(
      String name, ConfigChangeListener<T> listener, boolean listen) throws ConfigException {
    return ((Configurable<T>) configs.getOrThrow(name)).registerListener(listener, listen);
  }

  /** Writes config name-value pairs to the given writer. */
  public void writeTo(ConfigObjectWriter writer) {
    for (String key : configs.allKeys()) {
      try {
        writer.write(key, get(key));
      } catch (ConfigException ex) {
        // Should never happen, since the key comes from the ConfigMap.
        throw ex.wrap();
      }
    }
  }

  /** Writes config name-value pairs to the given writer. */
  public void writeTo(ConfigStringWriter writer) {
    for (String key : configs.allKeys()) {
      try {
        writer.write(key, get(key).toString());
      } catch (ConfigException ex) {
        // Should never happen, since the key comes from the ConfigMap.
        throw ex.wrap();
      }
    }
  }
}
