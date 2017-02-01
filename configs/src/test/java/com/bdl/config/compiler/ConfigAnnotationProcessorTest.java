package com.bdl.config.compiler;

import static com.google.common.truth.Truth.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;

import com.bdl.config.ConfigValue;
import com.bdl.config.Configuration;
import com.bdl.config.MainConfigDaggerModule;
import com.bdl.config.MainConfigGuiceModule;

import dagger.Component;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Tests the output of the {@code ConfigAnnotationProcessor}.
 *
 * @author Ben Leitner
 */
@RunWith(JUnit4.class)
public class ConfigAnnotationProcessorTest {

  @Test
  public void testDaggerInjection() throws Exception {
    ConfigComponent configComponent =
        DaggerConfigAnnotationProcessorTest_ConfigComponent.builder()
            .mainConfigDaggerModule(MainConfigDaggerModule.forArguments("--here_is_a_config=foo"))
            .build();
    Configuration configs = configComponent.getConfigs();
    assertThat(configs).isNotNull();
    InjectionTarget target = configComponent.getTarget();
    assertThat(target.configValue).isEqualTo("foo");
    assertThat(target.nullConfig).isNull();
  }

  @Test
  public void testGuiceInjection() throws Exception {
    Injector injector =
        Guice.createInjector(
            MainConfigGuiceModule.forArguments("--here_is_a_config=foo"), new ConfigGuiceModule());
    Configuration configs = injector.getInstance(Configuration.class);
    assertThat(configs).isNotNull();
    InjectionTarget target = injector.getInstance(InjectionTarget.class);
    assertThat(target.configValue).isEqualTo("foo");
    assertThat(target.nullConfig).isNull();
  }

  @Test
  public void testNestedSubpackageInjection_dagger() throws Exception {
    ConfigComponent configComponent =
        DaggerConfigAnnotationProcessorTest_ConfigComponent.builder()
            .mainConfigDaggerModule(MainConfigDaggerModule.forArguments("--here_is_a_config=foo"))
            .build();
    Configuration configs = configComponent.getConfigs();
    assertThat(configs.get("deep_nested_private")).isEqualTo("foo");
    assertThat(configs.get("deep_nested_package")).isEqualTo("foo");
    assertThat(configs.get("deep_nested_public")).isEqualTo("foo");
  }

  @Test
  public void testNestedSubpackageInjection_guice() throws Exception {
    Injector injector =
        Guice.createInjector(
            MainConfigGuiceModule.forArguments("--here_is_a_config=foo"), new ConfigGuiceModule());
    Configuration configs = injector.getInstance(Configuration.class);
    assertThat(configs.get("deep_nested_private")).isEqualTo("foo");
    assertThat(configs.get("deep_nested_package")).isEqualTo("foo");
    assertThat(configs.get("deep_nested_public")).isEqualTo("foo");
  }

  static class InjectionTarget {
    private final String configValue;
    private final String nullConfig;

    @Inject
    InjectionTarget(
        @ConfigValue("here_is_a_config") String configValue,
        @Nullable @ConfigValue("here_is_a_null_config") String nullConfig) {
      this.configValue = configValue;
      this.nullConfig = nullConfig;
    }
  }

  @Component(modules = {MainConfigDaggerModule.class, ConfigDaggerModule.class})
  @Singleton
  interface ConfigComponent {
    Configuration getConfigs();

    InjectionTarget getTarget();
  }
}
