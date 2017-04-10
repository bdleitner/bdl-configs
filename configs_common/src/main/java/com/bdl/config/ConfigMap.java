package com.bdl.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import com.bdl.config.ConfigException.AmbiguousConfigException;
import com.bdl.config.ConfigException.UnrecognizedConfigException;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Class to allow for multi-lookup of Configurables, by short name if unique or by fully qualified
 * name.
 *
 * @author Ben Leitner
 */
class ConfigMap {

  private final Map<String, Configurable<?>> configs;
  private final Multimap<String, String> names;

  @VisibleForTesting
  ConfigMap(Map<String, Configurable<?>> configs, Multimap<String, String> names) {
    this.configs = configs;
    this.names = names;
  }

  Configurable<?> getOrThrow(String key) {
    Configurable<?> config = getOrNull(key);
    if (config == null) {
      throw new UnrecognizedConfigException(key).wrap();
    }
    return config;
  }

  Configurable<?> getOrNull(String key) {
    Collection<String> fullNames = names.get(key);
    if (fullNames != null && fullNames.size() == 1) {
      return configs.get(Iterables.getOnlyElement(fullNames));
    }
    if (fullNames != null && fullNames.size() > 1) {
      throw new AmbiguousConfigException(key, fullNames).wrap();
    }
    // null or empty.
    return configs.get(key);
  }

  /** Returns a set of all registered {@link Configurable}s. */
  Set<Configurable<?>> allConfigurables() {
    return ImmutableSet.copyOf(configs.values());
  }

  /**
   * Returns a set of all keys for the configurables. The "short" names are used whenever
   * unambiguous.
   */
  Set<String> allKeys() {
    ImmutableSet.Builder<String> keys = ImmutableSet.builder();
    Set<String> unneeded = Sets.newHashSet();
    for (Map.Entry<String, Collection<String>> entry : names.asMap().entrySet()) {
      if (entry.getValue().size() == 1) {
        keys.add(entry.getKey());
        unneeded.add(Iterables.getOnlyElement(entry.getValue()));
      }
    }
    for (String key : configs.keySet()) {
      if (!unneeded.contains(key)) {
        keys.add(key);
      }
    }
    return keys.build();
  }

  static Builder builder() {
    return new Builder();
  }

  /** Builder class for the config map. */
  static class Builder {
    private ImmutableMap.Builder<String, Configurable<?>> configs;
    private ImmutableMultimap.Builder<String, String> names;

    private Builder() {
      this.configs = ImmutableMap.builder();
      this.names = ImmutableListMultimap.builder();
    }

    Builder addConfigurable(ConfigSupplier configSupplier) {
      ConfigDescription description = configSupplier.getDescription();
      configs.put(description.fullyQualifiedFieldName(), configSupplier.getConfigurable());
      names.put(description.name(), description.fullyQualifiedFieldName());
      return this;
    }

    ConfigMap build() {
      return new ConfigMap(configs.build(), names.build());
    }
  }
}
