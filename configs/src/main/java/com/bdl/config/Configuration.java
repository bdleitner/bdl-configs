package com.bdl.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

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

  private final Map<String, ConfigDescription> configs;

  @Inject
  public Configuration(Map<String, ConfigDescription> configs) {
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
      } catch (ConfigException.UnrecognizedConfigException ex) {
        unrecognizedConfigs.add(configEntry.getKey());
      }
    }

    if (!unrecognizedConfigs.isEmpty()) {
      throw new ConfigException.UnrecognizedConfigException(
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
      throws ConfigRuntimeException.InvalidConfigValueException,
      ConfigException.UnrecognizedConfigException,
      ConfigException.AmbiguousConfigException, ConfigRuntimeException.IllegalConfigStateException {

    ConfigDescription desc = configs.get(configName);

    // If we don't find the config, check and see if the name is no[config_name].
    if (desc == null && valueString == null && configName.startsWith(FALSE_BOOLEAN_CONFIG_PREFIX)) {
      configName = configName.substring(FALSE_BOOLEAN_CONFIG_PREFIX.length());
      valueString = FALSE_BOOLEAN_CONFIG_VALUE;
      desc = configs.get(configName);
    }

    if (desc == null) {
      throw new ConfigException.UnrecognizedConfigException(configName);
    }

    if (valueString == null && desc.type().equals("java.lang.Boolean")) {
      valueString = "true";
    }

    try {
      config(desc).setFromString(valueString);
    } catch (ConfigRuntimeException.InvalidConfigValueException ex) {
      ex.setConfigName(configName);
      throw ex;
    } catch (ConfigRuntimeException.IllegalConfigStateException ex) {
      ex.setConfigName(configName);
      throw ex;
    }
  }

  /** Return the config object for the given description */
  private static Configurable<?> config(ConfigDescription description) {
    try {
      Class<?> clazz = loadClass(description.className());
      Field field = clazz.getDeclaredField(description.fieldName());
      field.setAccessible(true);
      return (Configurable<?>) field.get(null);
    } catch (ClassNotFoundException ex) {
      throw new LinkageError(String.format(
          "Class for config field %s.%s is present in manifest but not found at runtime.",
          description.className(), description.fieldName()));
    } catch (NoSuchFieldException ex) {
      throw new LinkageError(String.format(
          "Configurable field %s.%s is present in manifest but not found at runtime.",
          description.className(), description.fieldName()));
    } catch (IllegalAccessException ex) {
      throw new LinkageError(String.format(
          "Unable to get Configurable field %s.%s",
          description.className(), description.fieldName()));
    } catch (NullPointerException ex) {
      throw new LinkageError(String.format(
          "Cannot get config %s.%s, did you forget to make it static?",
          description.className(), description.fieldName()));
    } catch (ClassCastException ex) {
      throw new LinkageError(String.format(
          "Cannot convert field %s.%s to a Configurable in order to resolve.",
          description.className(), description.fieldName()));
    }
  }

  /**
   * Loads a class with the specified type name.
   * This correctly handles type names representing inner classes.
   *
   * @param name The type name to load. Must not be null.
   * @return The loaded class
   * @throws ClassNotFoundException if no class is found.
   */
  private static Class<?> loadClass(String name) throws ClassNotFoundException {
    while (true) {
      try {
        return Class.forName(name);
      } catch (ClassNotFoundException ex) {
        int idx = name.lastIndexOf('.');
        if (idx < 0) {
          throw ex;
        }
        // Maybe it's an inner class? Replace last '.' with '$' to generate an inner class name.
        name = String.format("%s$%s", name.substring(0, idx), name.substring(idx + 1));
      }
    }
  }
}
