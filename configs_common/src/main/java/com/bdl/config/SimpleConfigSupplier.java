package com.bdl.config;

import com.google.common.base.Supplier;

/**
 * A simple implementation of {@link Supplier}
 *
 * @author Ben Leitner
 */
class SimpleConfigSupplier extends ConfigSupplier {

  private final ConfigDescription description;
  private final Configurable<?> config;

  SimpleConfigSupplier(ConfigDescription description, Configurable<?> config) {
    this.description = description;
    this.config = config;
  }

  @Override
  ConfigDescription getDescription() {
    return description;
  }

  @Override
  Configurable<?> getConfigurable() {
    return config;
  }
}
