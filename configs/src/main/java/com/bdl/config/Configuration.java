package com.bdl.config;

import com.bdl.config.ConfigException.UnrecognizedConfigException;
import com.bdl.config.ConfigRuntimeException.IllegalConfigStateException;
import com.bdl.config.ConfigRuntimeException.InvalidConfigValueException;
import com.google.common.collect.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility functions for working with {@link Configurable} objects
 *
 * @author Benjamin Leitner
 */
@Singleton
public class Configuration {

  private static final Pattern CONFIG_PATTERN = Pattern.compile("-+([a-zA-Z_]\\w*)(?:=(.*))?");
  private static final int CONFIG_NAME_GROUP = 1;
  private static final int CONFIG_VALUE_GROUP = 2;

  private static final String FALSE_BOOLEAN_CONFIG_PREFIX = "no";
  private static final String FALSE_BOOLEAN_CONFIG_VALUE = "false";

  private static final AtomicBoolean STATE_CHECKING_DISABLED = new AtomicBoolean(false);

  private static final AtomicBoolean PARSING_STARTED = new AtomicBoolean(false);

  private final Map<String, Configurable<?>> configs;

  @Inject
  public Configuration(Map<String, Configurable<?>> configs) {
    this.configs = ImmutableMap.copyOf(configs);
  }

  /** Disables config state checking when attempting to set a config value. Useful for writing tests. */
  public static void disableConfigSetCheck() {
    STATE_CHECKING_DISABLED.set(true);
  }

  /** Enables config state checking when attempting to set a config value. */
  public static void enableConfigSetCheck() {
    STATE_CHECKING_DISABLED.set(false);
  }

  /** Returns {@code true} if config write checking is disabled, {@code false} otherwise. */
  public static boolean isConfigSetCheckDisabled() {
    return STATE_CHECKING_DISABLED.get();
  }


  /**
   * Obtain the current configurable value by name.
   *
   * @throws ConfigException if no such configurable exists.
   */
  public Object get(String name) throws ConfigException {
    return getConfigurableOrThrow(name).get();
  }

  private Configurable<?> getConfigurableOrThrow(String name) throws ConfigException {
    Configurable<?> configurable = configs.get(name);
    if (configurable == null) {
      throw new UnrecognizedConfigException(name);
    }
    return configurable;
  }

  /** Parses the given arguments into configs, returns an array of the items that are not configs */
  public String[] parseConfigs(String... args) throws ConfigException {
    if (!PARSING_STARTED.compareAndSet(false, true)) {
      throw new IllegalStateException("Attempting to parse configs after they have already been parsed");
    }

    Map<String, String> configNamesToValues = Maps.newHashMap();
    List<String> nonConfigArgs = Lists.newArrayList();
    addConfigsToMap(configNamesToValues, nonConfigArgs, ImmutableList.copyOf(args));

    boolean loadedNewConfigs = true;
    while (loadedNewConfigs) {
      loadedNewConfigs = false;
      for (ExternalConfigLoader loader : ExternalConfigLoader.LOADERS) {
        String externalConfigValue = configNamesToValues.get(loader.getExternalConfigName());
        if (externalConfigValue != null) {
          loadedNewConfigs = true;
          configNamesToValues.remove(loader.getExternalConfigName());
          try {
            addConfigsToMap(configNamesToValues, nonConfigArgs, loader.getConfigArgs(externalConfigValue));
          } catch (Exception ex) {
            throw new ConfigException("Unable to load configs from external source.", ex);
          }
        }
      }
    }

    Set<String> unrecognizedConfigs = Sets.newHashSet();

    for (Map.Entry<String, String> configEntry : configNamesToValues.entrySet()) {
      try {
        processConfig(configEntry.getKey(), configEntry.getValue());
      } catch (UnrecognizedConfigException ex) {
        unrecognizedConfigs.add(configEntry.getKey());
      }
    }

    if (!unrecognizedConfigs.isEmpty()) {
      throw new UnrecognizedConfigException(
          unrecognizedConfigs.toArray(new String[unrecognizedConfigs.size()]));
    }
    return nonConfigArgs.toArray(new String[nonConfigArgs.size()]);
  }

  /**
   * Iterates through the args, building a map of config names to values. Any args that do not match the config pattern
   * are bypassed and added to the nonConfigArgs list.
   * <p>
   * If the argument "--" is encountered, all further arguments are assumed to be non-configs.
   */
  private void addConfigsToMap(
      Map<String, String> configNamesToValues,
      List<String> nonConfigArgs,
      Iterable<String> configArgs)
      throws ConfigException.InvalidConfigSyntaxException {
    boolean noMoreConfigs = false;

    for (String arg : configArgs) {
      if (noMoreConfigs || !arg.startsWith("-")) {
        nonConfigArgs.add(arg);
        continue;
      }

      if (arg.equals("--")) {
        noMoreConfigs = true;
        continue;
      }

      Matcher matcher = CONFIG_PATTERN.matcher(arg);
      if (!matcher.matches()) {
        throw new ConfigException.InvalidConfigSyntaxException(arg);
      }
      String configName = matcher.group(CONFIG_NAME_GROUP);
      String valueString = matcher.group(CONFIG_VALUE_GROUP);

      configNamesToValues.put(configName, valueString);
    }
  }

  private void processConfig(String configName, String valueString)
      throws InvalidConfigValueException,
      UnrecognizedConfigException,
      ConfigRuntimeException.AmbiguousConfigException, IllegalConfigStateException {

    Configurable<?> config = configs.get(configName);

    // If we don't find the config, check and see if the name is no[config_name].
    if (config == null && valueString == null && configName.startsWith(FALSE_BOOLEAN_CONFIG_PREFIX)) {
      configName = configName.substring(FALSE_BOOLEAN_CONFIG_PREFIX.length());
      valueString = FALSE_BOOLEAN_CONFIG_VALUE;
      config = configs.get(configName);
    }

    if (config == null) {
      throw new UnrecognizedConfigException(configName);
    }

    if (valueString == null && config.getType().equals(Boolean.class)) {
      valueString = "true";
    }

    try {
      config.setFromString(valueString);
    } catch (InvalidConfigValueException ex) {
      ex.setConfigName(configName);
      throw ex;
    } catch (IllegalConfigStateException ex) {
      ex.setConfigName(configName);
      throw ex;
    }
  }
}
