package com.bdl.config;

import static com.bdl.config.ConfigException.IllegalConfigStateException;
import static com.bdl.config.ConfigException.InvalidConfigValueException;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import com.bdl.config.ConfigChangeListener.ListenerRegistration;
import com.bdl.config.ConfigException.TypeMismatchException;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A class to hold a configurable value as from a command line argument.
 *
 * @param <T> The type of value contained in the config.
 * @author Benjamin Leitner
 */
public final class Configurable<T> {

  private final Class<T> type;
  private final T defaultValue;
  private final Predicate<? super T> predicate;
  private final Function<String, T> parser;
  private final boolean readOnlyAfterRead;
  private final Set<ConfigChangeListener<T>> listeners;

  /**
   * The current value of the config.
   * {@code volatile} because there is no synchronization of processing for methods that might set the config
   */
  protected volatile T value;
  private boolean read;

  private Configurable(
      Class<T> type,
      T defaultValue,
      Predicate<? super T> predicate,
      Function<String, T> parser,
      boolean readOnlyAfterRead) {
    this.type = type;
    this.defaultValue = defaultValue;
    this.predicate = predicate;
    this.parser = parser;
    value = defaultValue;
    this.readOnlyAfterRead = readOnlyAfterRead;
    if (!predicate.apply(value)) {
      throw new InvalidConfigValueException(value.toString()).wrap();
    }
    read = false;
    listeners = Sets.newHashSet();
  }

  /** Returns the value of this config. */
  public T get() {
    read = true;
    return value;
  }

  /** Returns the type of the configurable. */
  Class<T> getType() {
    return type;
  }

  boolean isReadOnly() {
    return readOnlyAfterRead && read && !Configuration.isConfigSetCheckDisabled();
  }

  /**
   * Checks that the config can be set.  A config's value may not be set after it is read.
   * This check can be disabled for testing using {@link Configuration#disableConfigSetCheck()}
   *
   * @throws IllegalConfigStateException if this config has already been read
   */
  private void checkSetState() throws IllegalConfigStateException {
    if (isReadOnly()) {
      throw new IllegalConfigStateException();
    }
  }

  private T checkValue(T value) throws InvalidConfigValueException {
    if (value != null && !type.equals(value.getClass())) {
      throw new TypeMismatchException(this.value, value).wrap();
    }
    if (!predicate.apply(value)) {
      throw new InvalidConfigValueException(value == null ? "<null>" : value.toString());
    }
    return value;
  }

  /** Sets this config's value from the specified string. */
  T setFromString(String valueString) throws ConfigException {
    return setValue(parser.apply(Preconditions.checkNotNull(valueString, "New config string value cannot be null.")));
  }

  /** Registers a listener to the configurable. */
  public ListenerRegistration registerListener(final ConfigChangeListener<T> listener) {
    return registerListener(listener, false);
  }

  /** Registers a listener to the configurable, including firing an initial event to sent the current value. */
  public ListenerRegistration registerListener(final ConfigChangeListener<T> listener, boolean listen) {
    listeners.add(listener);
    if (listen) {
      listener.onConfigurationChange(value);
    }
    return new ListenerRegistration() {
      @Override
      public void unregister() {
        listeners.remove(listener);
      }
    };
  }

  /**
   * Sets the config to a new value.
   * <p>
   * If the configurable is read-only-after-read (a flag), this method will fail if the value has been read unless
   * state checking is disabled.
   *
   * @param value the new value to assign to this config.
   * @return the prior value held by the config.
   * @throws ConfigException if an error occurs:
   * <ul>
   *   <li> {@link ConfigException.IllegalConfigStateException} if the configurable is not writable.</li>
   *   <li> {@link ConfigException.InvalidConfigValueException} if the given value is not valid for the config.</li>
   * </ul>
   * @see Configuration#disableConfigSetCheck()
   */
  public T setValue(T value) throws ConfigException {
    checkSetState();
    T oldValue = this.value;
    synchronized (this) {
      this.value = checkValue(value);
    }
    fireOnChange();
    return oldValue;
  }

  /**
   * Resets the config to its default value.
   *
   * @return the prior value held by the config.
   * @throws ConfigException if an error occurs:
   * <ul>
   *   <li> {@link ConfigException.IllegalConfigStateException} if the configurable is not writable.</li>
   * </ul>
   */
  public T reset() throws ConfigException {
    return setValue(defaultValue);
  }

  private void fireOnChange() {
    T newValue = get();
    for (ConfigChangeListener<T> listener : listeners) {
      listener.onConfigurationChange(newValue);
    }
  }

  /**
   * Returns a string representation of the value of this command line config.
   * This will retrieve the value from the {@link #get} method and thus mark
   * the config as having been accessed.
   */
  @Override
  public String toString() {
    return value == null ? "null" : value.toString();
  }

  /** Creates a new Configurable with the default value. */
  public static <T> Configurable<T> value(T defaultValue) {
    return Configurable.<T>builder().withDefaultValue(defaultValue).build();
  }

  /** Creates a new Configurable.Builder with the default value */
  public static <T> Configurable<T> flag(T defaultValue) {
    return Configurable.<T>builder().withDefaultValue(defaultValue).makeReadOnlyAfterRead().build();
  }

  /** Creates a new Configurable with the default value. */
  public static <T, S extends T> Configurable<T> value(Class<T> clazz, S defaultValue) {
    return Configurable.<T>builder().withClass(clazz).withDefaultValue(defaultValue).build();
  }

  /** Creates a new Configurable with the default value. */
  public static <T, S extends T> Configurable<T> flag(Class<T> clazz, S defaultValue) {
    return Configurable.<T>builder().withClass(clazz).withDefaultValue(defaultValue).makeReadOnlyAfterRead().build();
  }

  /** Creates a new Configurable with no default value but of the given class. */
  public static <T> Configurable<T> noDefault(Class<T> clazz) {
    return Configurable.<T>builder().withClass(clazz).build();
  }

  /** Creates a new Configurable with no default value but of the given class. */
  public static <T> Configurable<T> noDefaultFlag(Class<T> clazz) {
    return Configurable.<T>builder().withClass(clazz).makeReadOnlyAfterRead().build();
  }

  /** Returns a config that gives a list of strings */
  public static Configurable<List<String>> stringList(String... defaultValues) {
    return stringListConfigBuilder(defaultValues).build();
  }

  /** Returns a config that gives a list of strings */
  public static Configurable<List<String>> stringListFlag(String... defaultValues) {
    Builder<List<String>> listBuilder = stringListConfigBuilder(defaultValues);
    return listBuilder.makeReadOnlyAfterRead().build();
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
    private boolean readOnlyAfterRead;

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

    /** Indicates that the Configurable should not be writable once its value has been read. */
    public Builder<T> makeReadOnlyAfterRead() {
      this.readOnlyAfterRead = true;
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


      return new Configurable<>(clazz, defaultValue, predicate, getParser(), readOnlyAfterRead);
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
