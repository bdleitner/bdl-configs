package com.bdl.config.compiler.subpkg;

import com.bdl.config.ConfigDescription;
import com.bdl.config.ConfigException;
import com.bdl.config.ConfigSupplier;
import com.bdl.config.ConfigValue;
import com.bdl.config.Configuration;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;

/** Dagger module for binding configs in the com.bdl.config.compiler.subpkg package. */
@Module
public class ConfigDaggerModule {

  /** Adds a ConfigSupplier for config deep_nested_package (Outer.Middle.Inner.DEEP_NESTED_PACKAGE) to the set multibinder. */
  @Provides
  @IntoSet
  public static ConfigSupplier provideConfigSupplier_deep_nested_package() {
    ConfigDescription description = ConfigDescription.builder()
        .packageName("com.bdl.config.compiler.subpkg")
        .className("Outer.Middle.Inner")
        .fieldName("DEEP_NESTED_PACKAGE")
        .type("String")
        .specifiedName("deep_nested_package")
        .build();
    return ConfigSupplier.simple(description, Outer.Middle.Inner.DEEP_NESTED_PACKAGE);
  }

  /** Binds the type of the config with a ConfigValue annotation to the Configurable's value. */
  @Provides
  @ConfigValue("deep_nested_package")
  public static String provideConfigValue_deep_nested_package(Configuration configuration) {
    try {
      return (String) configuration.get("com.bdl.config.compiler.subpkg.Outer.Middle.Inner.DEEP_NESTED_PACKAGE");
    } catch (ConfigException ex) {
      throw ex.wrap();
    }
  }

  /** Adds a ConfigSupplier for config deep_nested_private (Outer.Middle.Inner.DEEP_NESTED_PRIVATE) to the set multibinder. */
  @Provides
  @IntoSet
  public static ConfigSupplier provideConfigSupplier_deep_nested_private() {
    ConfigDescription description = ConfigDescription.builder()
        .packageName("com.bdl.config.compiler.subpkg")
        .className("Outer.Middle.Inner")
        .fieldName("DEEP_NESTED_PRIVATE")
        .type("String")
        .specifiedName("deep_nested_private")
        .build();
    return ConfigSupplier.reflective(description);
  }

  /** Binds the type of the config with a ConfigValue annotation to the Configurable's value. */
  @Provides
  @ConfigValue("deep_nested_private")
  public static String provideConfigValue_deep_nested_private(Configuration configuration) {
    try {
      return (String) configuration.get("com.bdl.config.compiler.subpkg.Outer.Middle.Inner.DEEP_NESTED_PRIVATE");
    } catch (ConfigException ex) {
      throw ex.wrap();
    }
  }

  /** Adds a ConfigSupplier for config deep_nested_public (Outer.Middle.Inner.DEEP_NESTED_PUBLIC) to the set multibinder. */
  @Provides
  @IntoSet
  public static ConfigSupplier provideConfigSupplier_deep_nested_public() {
    ConfigDescription description = ConfigDescription.builder()
        .packageName("com.bdl.config.compiler.subpkg")
        .className("Outer.Middle.Inner")
        .fieldName("DEEP_NESTED_PUBLIC")
        .type("String")
        .specifiedName("deep_nested_public")
        .build();
    return ConfigSupplier.simple(description, Outer.Middle.Inner.DEEP_NESTED_PUBLIC);
  }

  /** Binds the type of the config with a ConfigValue annotation to the Configurable's value. */
  @Provides
  @ConfigValue("deep_nested_public")
  public static String provideConfigValue_deep_nested_public(Configuration configuration) {
    try {
      return (String) configuration.get("com.bdl.config.compiler.subpkg.Outer.Middle.Inner.DEEP_NESTED_PUBLIC");
    } catch (ConfigException ex) {
      throw ex.wrap();
    }
  }
}
