package com.bdl.config;

import com.google.common.base.Predicate;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Benjamin Leitner
 */
@RunWith(JUnit4.class)
public class ConfigurationTest {

  @After
  public void after() throws Exception {
    Configuration.enableConfigSetCheck();
  }

  @Test
  public void testConfigBuilder_simpleString() {
    Configurable<String> configurable = Configurable.value("foo");
    configurable.setValue("bar");
    assertThat(configurable.get()).isEqualTo("bar");
  }

  @Test
  public void testConfigBuilder_simpleInteger() {
    Configurable<Integer> configurable = Configurable.value(2);
    configurable.setFromString("25");
    assertThat(configurable.get()).isEqualTo(25);
  }

  @Test
  public void testConfigBuilder_simpleLong() {
    Configurable<Long> configurable = Configurable.value(2L);
    configurable.setFromString("25");
    assertThat(configurable.get()).isEqualTo(25L);
  }

  @Test
  public void testConfigBuilder_simpleBoolean() {
    Configurable<Boolean> configurable = Configurable.noDefault(Boolean.class);
    Configuration.disableConfigSetCheck();
    configurable.setFromString("yes");
    assertThat(configurable.get()).isTrue();
    configurable.setFromString("n");
    assertThat(configurable.get()).isFalse();
  }

  @Test
  public void testConfigBuilder_simpleBooleanParsing() {
    Configuration.disableConfigSetCheck();
    Configurable<Boolean> configurable = Configurable.value(true);
    assertThat(configurable.get()).isTrue();
    configurable.setFromString("n");
    assertThat(configurable.get()).isFalse();
  }

  @Test
  public void testConfigBuilder_simpleClass() {
    Configurable<Class> configurable = Configurable.value(Class.class, Boolean.class);
    Configuration.disableConfigSetCheck();
    configurable.setFromString(getClass().getName());
    assertThat(configurable.get()).isEqualTo(getClass());
  }

  @Test
  public void testPredicates() {
    Configurable<Integer> configurable = Configurable.<Integer>builder()
        .withDefaultValue(2)
        .withPredicate(new Predicate<Integer>() {
          @Override
          public boolean apply(Integer input) {
            return input >= 0;
          }
        })
        .build();
    try {
      configurable.setFromString("-25");
      Assert.fail();
    } catch (ConfigRuntimeException.InvalidConfigValueException e) {
      // expected
    }
  }
}
