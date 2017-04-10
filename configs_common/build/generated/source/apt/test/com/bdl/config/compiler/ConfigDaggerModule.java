package com.bdl.config.compiler;

import com.bdl.config.ConfigDescription;
import com.bdl.config.ConfigException;
import com.bdl.config.ConfigSupplier;
import com.bdl.config.ConfigValue;
import com.bdl.config.Configuration;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;

import javax.annotation.Nullable;

/** Dagger module for binding configs in the com.bdl.config.compiler package. */
@Module(includes = {com.bdl.config.compiler.subpkg.ConfigDaggerModule.class})
public class ConfigDaggerModule {

  /** Adds a ConfigSupplier for config here_is_a_config (ProcessorTarget.HERE_IS_A_CONFIGURABLE) to the set multibinder. */
  @Provides
  @IntoSet
  public static ConfigSupplier provideConfigSupplier_here_is_a_config() {
    ConfigDescription description = ConfigDescription.builder()
        .packageName("com.bdl.config.compiler")
        .className("ProcessorTarget")
        .fieldName("HERE_IS_A_CONFIGURABLE")
        .type("String")
        .specifiedName("here_is_a_config")
        .description("A dummy config for the annotation processor")
        .build();
    return ConfigSupplier.simple(description, ProcessorTarget.HERE_IS_A_CONFIGURABLE);
  }

  /** Binds the type of the config with a ConfigValue annotation to the Configurable's value. */
  @Provides
  @ConfigValue("here_is_a_config")
  public static String provideConfigValue_here_is_a_config(Configuration configuration) {
    try {
      return (String) configuration.get("com.bdl.config.compiler.ProcessorTarget.HERE_IS_A_CONFIGURABLE");
    } catch (ConfigException ex) {
      throw ex.wrap();
    }
  }

  /** Adds a ConfigSupplier for config here_is_a_null_config (ProcessorTarget.HERE_IS_A_NULL_CONFIGURABLE) to the set multibinder. */
  @Provides
  @IntoSet
  public static ConfigSupplier provideConfigSupplier_here_is_a_null_config() {
    ConfigDescription description = ConfigDescription.builder()
        .packageName("com.bdl.config.compiler")
        .className("ProcessorTarget")
        .fieldName("HERE_IS_A_NULL_CONFIGURABLE")
        .type("String")
        .specifiedName("here_is_a_null_config")
        .description("A dummy config for the annotation processor")
        .build();
    return ConfigSupplier.simple(description, ProcessorTarget.HERE_IS_A_NULL_CONFIGURABLE);
  }

  /** Binds the type of the config with a ConfigValue annotation to the Configurable's value. */
  @Provides
  @ConfigValue("here_is_a_null_config")
  @Nullable
  public static String provideConfigValue_here_is_a_null_config(Configuration configuration) {
    try {
      return (String) configuration.get("com.bdl.config.compiler.ProcessorTarget.HERE_IS_A_NULL_CONFIGURABLE");
    } catch (ConfigException ex) {
      throw ex.wrap();
    }
  }
}
