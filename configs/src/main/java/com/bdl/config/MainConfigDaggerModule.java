package com.bdl.config;

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
  @Target({ ElementType.METHOD, ElementType.PARAMETER})
  @interface ForConfigArguments {}

  private final List<String> arguments;

  public MainConfigDaggerModule(List<String> arguments) {
    this.arguments = arguments;
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

  @Module
  public static abstract class SetModule {
    @Multibinds abstract Set<ConfigSupplier> declaresConfigSupplierSet();
  }
}
