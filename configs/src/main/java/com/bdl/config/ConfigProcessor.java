package com.bdl.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.bdl.config.ConfigException.ConfigNameSettable;
import com.bdl.config.ConfigException.ExternalConfigLoadException;
import com.bdl.config.ConfigException.InvalidConfigSyntaxException;
import com.bdl.config.ConfigException.UnrecognizedConfigException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class to process configuration settings from a list of strings and apply them to configs.
 *
 * @author Ben Leitner
 */
class ConfigProcessor {

  private static final Pattern CONFIG_PATTERN = Pattern.compile("-+([a-zA-Z_]\\w*)(?:=(.*))?");
  private static final int CONFIG_NAME_GROUP = 1;
  private static final int CONFIG_VALUE_GROUP = 2;

  private static final String FALSE_BOOLEAN_CONFIG_PREFIX = "no";
  private static final String FALSE_BOOLEAN_CONFIG_VALUE = "false";

  private final List<String> arguments;
  private final Set<ConfigSupplier> configSuppliers;

  private ConfigMap configs;

  ConfigProcessor(List<String> arguments, Set<ConfigSupplier> configSuppliers) {
    this.arguments = ImmutableList.copyOf(arguments);
    this.configSuppliers = configSuppliers;
  }

  ConfigMap getConfigMap() {
    if (configs == null) {
      Map<String, String> nameToValueMap = getNameToValueMap();
      configs = suppliersToConfigMap();
      processConfigValues(nameToValueMap, configs);
    }
    return configs;
  }

  private ConfigMap suppliersToConfigMap() {
    ConfigMap.Builder configs = ConfigMap.builder();
    for (ConfigSupplier supplier : configSuppliers) {
      configs.addConfigurable(supplier);
    }
    return configs.build();
  }

  private Map<String, String> getNameToValueMap() {
    Map<String, String> configNamesToValues = Maps.newHashMap();
    List<String> nonConfigArgs = Lists.newArrayList();
    addConfigsToMap(configNamesToValues, nonConfigArgs, arguments);

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
            throw new ExternalConfigLoadException("Unable to load configs from external source.", ex)
                .wrap();
          }
        }
      }
    }
    return configNamesToValues;
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
      Iterable<String> configArgs) {
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
        throw new InvalidConfigSyntaxException(arg).wrap();
      }
      String configName = matcher.group(CONFIG_NAME_GROUP);
      String valueString = matcher.group(CONFIG_VALUE_GROUP);

      configNamesToValues.put(configName, valueString);
    }
  }

  private void processConfigValues(Map<String, String> namesToValues, ConfigMap configMap) {
    Set<String> unrecognizedConfigs = Sets.newHashSet();

    for (Map.Entry<String, String> configEntry : namesToValues.entrySet()) {
      try {
        processConfig(configMap, configEntry.getKey(), configEntry.getValue());
      } catch (UnrecognizedConfigException ex) {
        unrecognizedConfigs.add(configEntry.getKey());
      }
    }

    if (!unrecognizedConfigs.isEmpty()) {
      throw new UnrecognizedConfigException(
          unrecognizedConfigs.toArray(new String[unrecognizedConfigs.size()]))
          .wrap();
    }
  }

  private void processConfig(ConfigMap configMap, String configName, String valueString)
      throws UnrecognizedConfigException {

    Configurable<?> config = configMap.getOrNull(configName);

    // If we don't find the config, check and see if the name is no[config_name].
    if (config == null && valueString == null && configName.startsWith(FALSE_BOOLEAN_CONFIG_PREFIX)) {
      configName = configName.substring(FALSE_BOOLEAN_CONFIG_PREFIX.length());
      valueString = FALSE_BOOLEAN_CONFIG_VALUE;
      config = configMap.getOrNull(configName);
    }

    if (config == null) {
      throw new UnrecognizedConfigException(configName);
    }

    if (valueString == null && config.getType().equals(Boolean.class)) {
      valueString = "true";
    }

    try {
      config.setFromString(valueString);
    } catch (ConfigException ex) {
      if (ex instanceof ConfigNameSettable) {
        ex = ((ConfigNameSettable) ex).withConfigName(configName);
      }
      throw ex.wrap();
    }
  }
}
