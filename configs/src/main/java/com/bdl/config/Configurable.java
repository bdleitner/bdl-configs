package com.bdl.config;

import static com.bdl.config.ConfigRuntimeException.IllegalConfigStateException;
import static com.bdl.config.ConfigRuntimeException.InvalidConfigValueException;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A class to hold a configurable value as from a command line argument.
 *
 * @param <T> The type of value contained in the config.
 * @author Benjamin Leitner
 */
public final class Configurable<T> {

  protected final T defaultValue;
  private final Predicate<? super T> predicate;
  private final Function<String, T> parser;
  private final boolean lockable;

  /**
   * The current value of the config.
   * {@code volatile} because there is no synchronization of processing for methods that might set the config
   */
  protected volatile T value;
  private boolean read;

  private Configurable(
      T defaultValue,
      Predicate<? super T> predicate,
      Function<String, T> parser,
      boolean lockable) {
    this.defaultValue = defaultValue;
    this.predicate = predicate;
    this.parser = parser;
    value = defaultValue;
    this.lockable = lockable;
    if (!predicate.apply(value)) {
      throw new InvalidConfigValueException(value.toString());
    }
    read = false;
  }

  /** Returns the value of this config. */
  public final T get() {
    read = true;
    return value;
  }

  /**
   * Returns a string representation of the value of this command line config.
   * This will retrieve the value from the {@link #get} method and thus mark
   * the config as having been accessed.
   */
  @Override
  public final String toString() {
    return value == null ? null : value.toString();
  }

  /**
   * Checks that the config can be set.  A config's value may not be set after it is read.
   * This check can be disabled for testing using {@link Configuration#disableConfigSetCheck()}
   *
   * @throws IllegalConfigStateException if this config has already been read
   */
  private void checkSetState() throws ConfigRuntimeException.IllegalConfigStateException {
    if (lockable && read && !Configuration.isConfigSetCheckDisabled()) {
      throw new IllegalConfigStateException();
    }
  }

  private T checkValue(T value) throws InvalidConfigValueException {
    if (!predicate.apply(value)) {
      throw new InvalidConfigValueException(value.toString());
    }
    return value;
  }

  /** Sets this config's value from the specified string. */
  final Configurable<T> setFromString(String valueString) throws InvalidConfigValueException,
      ConfigRuntimeException.IllegalConfigStateException {
    checkSetState();
    if (valueString == null) {
      if (Configuration.isConfigSetCheckDisabled()) {
        value = null;
      } else {
        throw new NullPointerException();
      }
    } else {
      value = checkValue(parser.apply(valueString));
    }
    return this;
  }

  /**
   * Sets the config to a new value.
   * <p>
   * If the configurable is lockable (a flag), this method will fail if the value has been read unless
   * state checking is disabled.
   *
   * @param value the new value to assign to this config
   * @see Configuration#disableConfigSetCheck()
   */
  public void setValue(T value)
      throws ConfigRuntimeException.InvalidConfigValueException, IllegalConfigStateException {
    checkSetState();
    this.value = checkValue(value);
  }

  /** Creates a new Configurable with the default value. */
  public static <T> Configurable<T> value(T defaultValue) {
    return Configurable.<T>builder().withDefaultValue(defaultValue).build();
  }

  /** Creates a new Configurable.Builder with the default value */
  public static <T> Configurable<T> flag(T defaultValue) {
    return Configurable.<T>builder().withDefaultValue(defaultValue).makeLockable().build();
  }

  /** Creates a new Configurable with the default value. */
  public static <T, S extends T> Configurable<T> value(Class<T> clazz, S defaultValue) {
    return Configurable.<T>builder().withClass(clazz).withDefaultValue(defaultValue).build();
  }

  /** Creates a new Configurable with the default value. */
  public static <T, S extends T> Configurable<T> flag(Class<T> clazz, S defaultValue) {
    return Configurable.<T>builder().withClass(clazz).withDefaultValue(defaultValue).makeLockable().build();
  }

  /** Creates a new Configurable with no default value but of the given class. */
  public static <T> Configurable<T> noDefault(Class<T> clazz) {
    return Configurable.<T>builder().withClass(clazz).build();
  }

  /** Creates a new Configurable with no default value but of the given class. */
  public static <T> Configurable<T> noDefaultFlag(Class<T> clazz) {
    return Configurable.<T>builder().withClass(clazz).makeLockable().build();
  }

  /** Returns a config that gives a list of strings */
  public static Configurable<List<String>> stringList(String... defaultValues) {
    return stringListConfigBuilder(defaultValues).build();
  }

  /** Returns a config that gives a list of strings */
  public static Configurable<List<String>> stringListFlag(String... defaultValues) {
    Builder<List<String>> listBuilder = stringListConfigBuilder(defaultValues);
    return listBuilder.makeLockable().build();
  }

  private static Builder<List<String>> stringListConfigBuilder(String[] defaultValues) {
    return Configurable.<List<String>>builder()
          .withDefaultValue(ImmutableList.copyOf(defaultValues))
          .withParser(new Function<String, List<String>>() {
            @Override
            public ImmutableList<String> apply(String input) {
              return ImmutableList.copyOf(Splitter.on(Pattern.compile(",\\s*")).split(input));
            }
          });
  }

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  /** Builder class for configs */
  public static class Builder<T> {
    private Class<T> clazz;
    private T defaultValue;
    private Function<String, T> parser;
    private ImmutableList.Builder<Predicate<? super T>> predicates;
    private boolean lockable;

    private Builder() {
      predicates = ImmutableList.builder();
    }

    public Builder<T> withClass(Class<T> clazz) {
      Preconditions.checkState(this.clazz == null,
          "Class already interpreted from default value %s."
              + " If the default is a subclass of the Configurable type, call withClass prior to withDefaultValue.",
          defaultValue);
      this.clazz = Preconditions.checkNotNull(clazz, "Class cannot be null.");
      return this;
    }

    @SuppressWarnings("unchecked") // Will throw if wrong.
    public Builder<T> withDefaultValue(T defaultValue) {
      Preconditions.checkState(this.defaultValue == null, "Default value already set to %s", this.defaultValue);
      this.defaultValue = Preconditions.checkNotNull(defaultValue, "Default value cannot be null");
      if (this.clazz == null) {
        // If this throws ClassCastException, it indicates that the default value s a subtype of the config type.
        // Call withClass() prior to setting the default value.
        this.clazz = (Class<T>) defaultValue.getClass();
      }
      return this;
    }

    public Builder<T> withPredicate(Predicate<? super T> predicate) {
      predicates.add(predicate);
      return this;
    }

    public Builder<T> withParser(Function<String, T> parser) {
      Preconditions.checkState(this.parser == null, "Parser already set to %s", this.parser);
      this.parser = Preconditions.checkNotNull(parser, "Parser cannot be null.");
      return this;
    }

    public Builder<T> makeLockable() {
      this.lockable = true;
      return this;
    }

    public Configurable<T> build() {
      ImmutableList<Predicate<? super T>> built = predicates.build();
      Predicate<? super T> predicate = Predicates.alwaysTrue();
      if (built.size() == 1) {
        predicate = Iterables.getOnlyElement(built);
      }
      if (built.size() > 1) {
        predicate = Predicates.and(built);
      }

      return new Configurable<>(defaultValue, predicate, getParser(), lockable);
    }

    private Function<String,T> getParser() {
      if (parser != null) {
        return parser;
      }
      Function<String, T> parserForClass = Parsers.forClass(Preconditions.checkNotNull(clazz,
          "No parser specified and no class was determined."));
      return Preconditions.checkNotNull(parserForClass,
          "No parser specified and none could be determined for class %s", clazz);
    }
  }
}
