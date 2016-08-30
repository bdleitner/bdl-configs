package com.bdl.config;

import com.bdl.config.ConfigRuntimeException.IllegalConfigStateException;
import com.google.common.base.Predicate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Benjamin Leitner
 */
@RunWith(JUnit4.class)
public class ConfigurableTest {

  private static final Predicate<Integer> POSITIVE_INTEGER = new Predicate<Integer>() {
    @Override
    public boolean apply(Integer input) {
      return input > 0;
    }
  };

  private enum TestEnum {
    FIRST,
    SECOND,
    THIRD
  }

  @Before
  public void before() throws Exception {
    Configuration.enableConfigSetCheck();
  }

  @After
  public void after() throws Exception {
    Configuration.enableConfigSetCheck();
  }

  @Test
  public void testConfigSetAndGet() throws Exception {
    Configurable<String> configurable = Configurable.value("foo");
    configurable.setFromString("bar");
    assertThat(configurable.get()).isEqualTo("bar");
    configurable.setFromString("baz");
    assertThat(configurable.get()).isEqualTo("baz");
  }

  @Test
  public void testFlagGetAndSet() throws Exception {
    Configurable<String> configurable = Configurable.flag("foo");
    configurable.setFromString("bar");
    assertThat(configurable.get()).isEqualTo("bar");
    try {
      configurable.setFromString("baz");
    } catch (IllegalConfigStateException ex) {
      // expected
    }
  }

  @Test
  public void testErrorOnConfigSetAfterGet() throws Exception {
    Configurable<String> configurable = Configurable.flag("foo");
    assertThat(configurable.get()).isEqualTo("foo");
    try {
      configurable.setFromString("bar");
      fail();
    } catch (IllegalConfigStateException ex) {
      // expected
    }
    assertThat(configurable.get()).isEqualTo("foo");
  }

  @Test
  public void testErrorOnConfigSetAfterGetOkWithDisable() throws Exception {
    Configuration.disableConfigSetCheck();
    Configurable<String> configurable = Configurable.flag("foo");
    assertThat(configurable.get()).isEqualTo("foo");
    configurable.setFromString("bar");
    assertThat(configurable.get()).isEqualTo("bar");
  }

  @Test
  public void testNullDefaultFlag() throws Exception {
    Configurable<String> configurable = Configurable.noDefaultFlag(String.class);
    assertThat(configurable.get()).isNull();
    try {
      configurable.setFromString("bar");
      fail();
    } catch (IllegalConfigStateException ex) {
      // expected
    }
  }

  @Test
  public void testErrorOnNullSet() throws Exception {
    Configurable<String> configurable = Configurable.value("foo");
    try {
      configurable.setFromString(null);
      fail();
    } catch (NullPointerException ex) {
      // expected
    }
  }

  @Test
  public void testParsing() throws Exception {
    Configurable<Integer> configurable = Configurable.value(1);
    assertThat(configurable.get()).isEqualTo(1);
    configurable.setFromString("2");
    assertThat(configurable.get()).isEqualTo(2);
    try {
      configurable.setFromString("blah");
      fail();
    } catch (NumberFormatException ex) {
      // expected
    }
  }

  @Test
  public void testEnumConfig() {
    Configurable<TestEnum> value = Configurable.value(TestEnum.SECOND);
    value.setFromString("THIRD");
    assertThat(value.get()).isEqualTo(TestEnum.THIRD);
  }

  @Test
  public void testPredicates() throws Exception {
    Configurable<Integer> configurable = Configurable.<Integer>builder()
        .withDefaultValue(1)
        .withPredicate(POSITIVE_INTEGER)
        .build();
    Configuration.disableConfigSetCheck();
    assertThat(configurable.get()).isEqualTo(1);
    try {
      configurable.setFromString("-22");
      fail();
    } catch (ConfigRuntimeException.InvalidConfigValueException e) {
      // expected
    }
    assertThat(configurable.get()).isEqualTo(1);
  }

  @Test
  public void testPredicates_badDefault() throws Exception {
    try {
      Configurable.<Integer>builder().withDefaultValue(-10).withPredicate(POSITIVE_INTEGER).build();
      fail();
    } catch (ConfigRuntimeException.InvalidConfigValueException ex) {
      // expected
    }
  }
}
