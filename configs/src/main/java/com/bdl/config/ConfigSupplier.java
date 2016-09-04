package com.bdl.config;

/**
 * Base class for classes that can supply configurables to the configuration.
 *
 * @author Ben Leitner
 */
abstract class ConfigSupplier {

  /** Obtain the description of the Configurable. */
  abstract ConfigDescription getDescription();

  /** Obtain the Configurable instance. */
  abstract Configurable<?> getConfigurable();

  /** Creates a simple supplier. */
  public static ConfigSupplier simple(ConfigDescription description, Configurable<?> config) {
    return new SimpleConfigSupplier(description, config);
  }

  /** Creates a reflective supplier. */
  public static ConfigSupplier reflective(ConfigDescription description) {
    return new ReflectiveConfigSupplier(description);
  }
}
