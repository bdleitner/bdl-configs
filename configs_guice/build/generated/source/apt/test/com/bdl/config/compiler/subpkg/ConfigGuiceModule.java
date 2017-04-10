package com.bdl.config.compiler.subpkg;

import com.bdl.config.ConfigDescription;
import com.bdl.config.ConfigException;
import com.bdl.config.ConfigSupplier;
import com.bdl.config.ConfigValue;
import com.bdl.config.Configuration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;

/** Guice module for binding configs in the com.bdl.config.compiler.subpkg package. */
public class ConfigGuiceModule extends AbstractModule {

  @Override
  protected void configure() {
    Multibinder<ConfigSupplier> supplierBinder = Multibinder.newSetBinder(binder(), ConfigSupplier.class);
    bindConfigSupplier_deep_nested_package(supplierBinder);
    bindConfigSupplier_deep_nested_private(supplierBinder);
    bindConfigSupplier_deep_nested_public(supplierBinder);
  }

  /** Binds a ConfigSupplier for config deep_nested_package (Outer.Middle.Inner.DEEP_NESTED_PACKAGE) to the set multibinder. */
  private void bindConfigSupplier_deep_nested_package(Multibinder<ConfigSupplier> binder) {
    ConfigDescription description = ConfigDescription.builder()
        .packageName("com.bdl.config.compiler.subpkg")
        .className("Outer.Middle.Inner")
        .fieldName("DEEP_NESTED_PACKAGE")
        .type("String")
        .specifiedName("deep_nested_package")
        .build();
    binder.addBinding().toInstance(
        ConfigSupplier.simple(description, Outer.Middle.Inner.DEEP_NESTED_PACKAGE));
  }

  /** Binds the type of the config with a ConfigValue annotation to the Configurable's value. */
  @Provides
  @ConfigValue("deep_nested_package")
  String provideConfigValue_deep_nested_package(Configuration configuration) {
    try {
      return (String) configuration.get("com.bdl.config.compiler.subpkg.Outer.Middle.Inner.DEEP_NESTED_PACKAGE");
    } catch (ConfigException ex) {
      throw ex.wrap();
    }
  }

  /** Binds a ConfigSupplier for config deep_nested_private (Outer.Middle.Inner.DEEP_NESTED_PRIVATE) to the set multibinder. */
  private void bindConfigSupplier_deep_nested_private(Multibinder<ConfigSupplier> binder) {
    ConfigDescription description = ConfigDescription.builder()
        .packageName("com.bdl.config.compiler.subpkg")
        .className("Outer.Middle.Inner")
        .fieldName("DEEP_NESTED_PRIVATE")
        .type("String")
        .specifiedName("deep_nested_private")
        .build();
    binder.addBinding().toInstance(
        ConfigSupplier.reflective(description));
  }

  /** Binds the type of the config with a ConfigValue annotation to the Configurable's value. */
  @Provides
  @ConfigValue("deep_nested_private")
  String provideConfigValue_deep_nested_private(Configuration configuration) {
    try {
      return (String) configuration.get("com.bdl.config.compiler.subpkg.Outer.Middle.Inner.DEEP_NESTED_PRIVATE");
    } catch (ConfigException ex) {
      throw ex.wrap();
    }
  }

  /** Binds a ConfigSupplier for config deep_nested_public (Outer.Middle.Inner.DEEP_NESTED_PUBLIC) to the set multibinder. */
  private void bindConfigSupplier_deep_nested_public(Multibinder<ConfigSupplier> binder) {
    ConfigDescription description = ConfigDescription.builder()
        .packageName("com.bdl.config.compiler.subpkg")
        .className("Outer.Middle.Inner")
        .fieldName("DEEP_NESTED_PUBLIC")
        .type("String")
        .specifiedName("deep_nested_public")
        .build();
    binder.addBinding().toInstance(
        ConfigSupplier.simple(description, Outer.Middle.Inner.DEEP_NESTED_PUBLIC));
  }

  /** Binds the type of the config with a ConfigValue annotation to the Configurable's value. */
  @Provides
  @ConfigValue("deep_nested_public")
  String provideConfigValue_deep_nested_public(Configuration configuration) {
    try {
      return (String) configuration.get("com.bdl.config.compiler.subpkg.Outer.Middle.Inner.DEEP_NESTED_PUBLIC");
    } catch (ConfigException ex) {
      throw ex.wrap();
    }
  }
}
