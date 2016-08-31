package com.bdl.config;

import com.bdl.config.ConfigRuntimeException.AmbiguousConfigException;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.Multibinds;

import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;

/**
 * Root dagger module to produce a {@link Configuration}.
 *
 * @author Ben Leitner
 */
@Module
public abstract class ConfigDaggerModule {

  @Multibinds abstract Set<ConfigSupplier> declaresConfigSupplierSet();

  @Provides
  @Singleton
  public static Configuration provideConfiguration(Set<ConfigSupplier> configSuppliers) {
    Map<String, ConfigSupplier> supplierMap = Maps.newHashMap();
    for (ConfigSupplier supplier : configSuppliers) {
      ConfigDescription description = supplier.getDescription();
      String name = description.name();
      if (supplierMap.containsKey(name)) {
        throw new AmbiguousConfigException(
            supplierMap.get(name).getDescription(),
            supplier.getDescription());
      }
    }

    return new Configuration(
        ImmutableMap.copyOf(
            Maps.transformValues(supplierMap, new Function<ConfigSupplier, Configurable<?>>() {
              @Override
              public Configurable<?> apply(ConfigSupplier input) {
                return input.getConfigurable();
              }
            })));
  }
}
