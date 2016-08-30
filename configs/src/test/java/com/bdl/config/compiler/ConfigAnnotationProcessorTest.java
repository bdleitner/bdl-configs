package com.bdl.config.compiler;

import com.bdl.ConfigDaggerModule;
import com.bdl.config.ConfigValue;
import com.bdl.config.Configuration;
import dagger.Component;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests the output of the {@code ConfigAnnotationProcessor}.
 *
 * @author Ben Leitner
 */
@RunWith(JUnit4.class)
public class ConfigAnnotationProcessorTest {

  @Test
  public void testConfigInjection() throws Exception {
    ConfigComponent configComponent = DaggerConfigAnnotationProcessorTest_ConfigComponent.create();
    Configuration configs = configComponent.getConfigs();
    configs.parseConfigs("--here_is_a_config=foo");

    InjectingComponent injectingComponent = DaggerConfigAnnotationProcessorTest_InjectingComponent.create();
    InjectionTarget target = injectingComponent.getTarget();
    assertThat(target.configValue).isEqualTo("foo");
  }

  static class InjectionTarget {
    private final String configValue;

    @Inject
    InjectionTarget(@ConfigValue("here_is_a_config") String configValue) {

      this.configValue = configValue;
    }
  }

  @Component(modules = ConfigDaggerModule.class)
  @Singleton
  interface ConfigComponent {
    Configuration getConfigs();
  }

  @Component(modules = ConfigDaggerModule.class)
  interface InjectingComponent {
    InjectionTarget getTarget();
  }
}
