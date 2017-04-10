package com.bdl.config.compiler;

import com.bdl.config.ConfigDescription;
import com.bdl.config.ConfigException;
import com.bdl.config.ConfigSupplier;
import com.bdl.config.ConfigValue;
import com.bdl.config.Configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;

/** Guice module for binding configs in the com.bdl.config.compiler package. */
public class ConfigGuiceModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new com.bdl.config.compiler.subpkg.ConfigGuiceModule());

    Multibinder<ConfigSupplier> supplierBinder = Multibinder.newSetBinder(binder(), ConfigSupplier.class);
    bindConfigSupplier_here_is_a_config(supplierBinder);
    bindConfigSupplier_here_is_a_null_config(supplierBinder);
  }

  /** Binds a ConfigSupplier for config here_is_a_config (GuiceProcessorTarget.HERE_IS_A_CONFIGURABLE) to the set multibinder. */
  private void bindConfigSupplier_here_is_a_config(Multibinder<ConfigSupplier> binder) {
    ConfigDescription description = ConfigDescription.builder()
        .packageName("com.bdl.config.compiler")
        .className("GuiceProcessorTarget")
        .fieldName("HERE_IS_A_CONFIGURABLE")
        .type("String")
        .specifiedName("here_is_a_config")
        .description("A dummy config for the annotation processor")
        .build();
    binder.addBinding().toInstance(
        ConfigSupplier.simple(description, GuiceProcessorTarget.HERE_IS_A_CONFIGURABLE));
  }

  /** Binds the type of the config with a ConfigValue annotation to the Configurable's value. */
  @Provides
  @ConfigValue("here_is_a_config")
  String provideConfigValue_here_is_a_config(Configuration configuration) {
    try {
      return (String) configuration.get("com.bdl.config.compiler.GuiceProcessorTarget.HERE_IS_A_CONFIGURABLE");
    } catch (ConfigException ex) {
      throw ex.wrap();
    }
  }

  /** Binds a ConfigSupplier for config here_is_a_null_config (GuiceProcessorTarget.HERE_IS_A_NULL_CONFIGURABLE) to the set multibinder. */
  private void bindConfigSupplier_here_is_a_null_config(Multibinder<ConfigSupplier> binder) {
    ConfigDescription description = ConfigDescription.builder()
        .packageName("com.bdl.config.compiler")
        .className("GuiceProcessorTarget")
        .fieldName("HERE_IS_A_NULL_CONFIGURABLE")
        .type("String")
        .specifiedName("here_is_a_null_config")
        .description("A dummy config for the annotation processor")
        .build();
    binder.addBinding().toInstance(
        ConfigSupplier.simple(description, GuiceProcessorTarget.HERE_IS_A_NULL_CONFIGURABLE));
  }

  /** Binds the type of the config with a ConfigValue annotation to the Configurable's value. */
  @Provides
  @ConfigValue("here_is_a_null_config")
  String provideConfigValue_here_is_a_null_config(Configuration configuration) {
    try {
      return (String) configuration.get("com.bdl.config.compiler.GuiceProcessorTarget.HERE_IS_A_NULL_CONFIGURABLE");
    } catch (ConfigException ex) {
      throw ex.wrap();
    }
  }
}
