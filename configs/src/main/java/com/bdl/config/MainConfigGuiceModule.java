package com.bdl.config;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

/**
 * Root Guice module to produce a {@link Configuration}.
 *
 * @author Ben Leitner
 */
public class MainConfigGuiceModule extends AbstractModule {

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.METHOD, ElementType.PARAMETER})
  @interface ForConfigArguments {}

  private final List<String> arguments;

  private MainConfigGuiceModule(List<String> arguments) {
    this.arguments = arguments;
  }

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), ConfigSupplier.class);
  }

  /** Creates a new {@link MainConfigGuiceModule} using the given strings as inputs. */
  public static MainConfigGuiceModule forArguments(String... arguments) {
    return forArguments(ImmutableList.copyOf(arguments));
  }

  /** Creates a new {@link MainConfigGuiceModule} using the given strings as inputs. */
  public static MainConfigGuiceModule forArguments(Iterable<String> arguments) {
    return new MainConfigGuiceModule(ImmutableList.copyOf(arguments));
  }

  @Provides
  @MainConfigGuiceModule.ForConfigArguments
  List<String> provideConfigArguments() {
    return arguments;
  }

  @Provides
  @Singleton
  ConfigMap provideConfigMap(ConfigProcessor processor) {
    return processor.getConfigMap();
  }

  @Provides
  ConfigProcessor provideConfigProcessor(
      @ForConfigArguments List<String> arguments, Set<ConfigSupplier> suppliers) {
    return new ConfigProcessor(arguments, suppliers);
  }
}
