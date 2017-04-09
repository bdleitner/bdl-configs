package com.bdl.config;

import com.google.common.collect.ImmutableList;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.Multibinds;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Set;

import javax.inject.Qualifier;
import javax.inject.Singleton;

/**
 * Root dagger module to produce a {@link Configuration}.
 *
 * @author Ben Leitner
 */
@Module(includes = {MainConfigDaggerModule.SetModule.class})
public class MainConfigDaggerModule {

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.PARAMETER})
  @interface ForConfigArguments {}

  private final List<String> arguments;

  private MainConfigDaggerModule(List<String> arguments) {
    this.arguments = arguments;
  }

  /** Creates a new {@link MainConfigDaggerModule}. */
  public static MainConfigDaggerModule create() {
    return forArguments(ImmutableList.<String>of());
  }

  /** Creates a new {@link MainConfigDaggerModule} using the given strings as inputs. */
  public static MainConfigDaggerModule forArguments(String... arguments) {
    return forArguments(ImmutableList.copyOf(arguments));
  }

  /** Creates a new {@link MainConfigDaggerModule} using the given strings as inputs. */
  public static MainConfigDaggerModule forArguments(Iterable<String> arguments) {
    return new MainConfigDaggerModule(ImmutableList.copyOf(arguments));
  }

  @Provides
  @MainConfigDaggerModule.ForConfigArguments
  public List<String> provideConfigArguments() {
    return arguments;
  }

  @Provides
  @Singleton
  public static ConfigMap provideConfigMap(ConfigProcessor processor) {
    return processor.getConfigMap();
  }

  @Provides
  static ConfigProcessor provideConfigProcessor(
      @ForConfigArguments List<String> arguments, Set<ConfigSupplier> suppliers) {
    return new ConfigProcessor(arguments, suppliers);
  }

  @Module
  public abstract static class SetModule {
    @Multibinds
    abstract Set<ConfigSupplier> declaresConfigSupplierSet();
  }
}
