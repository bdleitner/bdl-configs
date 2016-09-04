package com.bdl.config;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Predicate;

import com.bdl.config.ConfigException.InvalidConfigValueException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
  public void testConfigBuilder_simpleString() throws Exception {
    Configurable<String> configurable = Configurable.value("foo");
    configurable.setValue("bar");
    assertThat(configurable.get()).isEqualTo("bar");
  }

  @Test
  public void testConfigBuilder_simpleInteger() throws Exception {
    Configurable<Integer> configurable = Configurable.value(2);
    configurable.setFromString("25");
    assertThat(configurable.get()).isEqualTo(25);
  }

  @Test
  public void testConfigBuilder_simpleLong() throws Exception {
    Configurable<Long> configurable = Configurable.value(2L);
    configurable.setFromString("25");
    assertThat(configurable.get()).isEqualTo(25L);
  }

  @Test
  public void testConfigBuilder_simpleBoolean() throws Exception {
    Configurable<Boolean> configurable = Configurable.noDefault(Boolean.class);
    Configuration.disableConfigSetCheck();
    configurable.setFromString("yes");
    assertThat(configurable.get()).isTrue();
    configurable.setFromString("n");
    assertThat(configurable.get()).isFalse();
  }

  @Test
  public void testConfigBuilder_simpleBooleanParsing() throws Exception {
    Configuration.disableConfigSetCheck();
    Configurable<Boolean> configurable = Configurable.value(true);
    assertThat(configurable.get()).isTrue();
    configurable.setFromString("n");
    assertThat(configurable.get()).isFalse();
  }

  @Test
  public void testConfigBuilder_simpleClass() throws Exception {
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
    } catch (ConfigException ex) {
      // expected
      assertThat(ex).isInstanceOf(InvalidConfigValueException.class);
    }
  }
}
