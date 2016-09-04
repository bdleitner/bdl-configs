package com.bdl.config;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Interface for classes that load config values from external sources
 *
 * @author Benjamin Leitner
 */
abstract class ExternalConfigLoader {

  static final List<ExternalConfigLoader> LOADERS = Lists.newArrayList(
      fromFile(),
      fromResource(),
      fromSystemProperties());
  private final String sourceConfig;

  protected ExternalConfigLoader(String sourceConfig) {
    this.sourceConfig = sourceConfig;
  }

  /** Returns an {@link ExternalConfigLoader} that reads from files */
  public static ExternalConfigLoader fromFile() {
    return new FileExternalConfigLoader();
  }

  /** Returns an {@link ExternalConfigLoader} that reads from a Resource */
  public static ExternalConfigLoader fromResource() {
    return new ResourceExternalConfigLoader();
  }

  /** Returns an {@link ExternalConfigLoader} that reads from System Properties */
  public static ExternalConfigLoader fromSystemProperties() {
    return new SystemPropertiesExternalConfigLoader();
  }

  /** Returns the name of the config to which this external loader is keyed */
  public String getExternalConfigName() {
    return sourceConfig;
  }

  /**
   * Obtains a list of config arguments from an external source based on the given sourcing config
   * value
   */
  public abstract List<String> getConfigArgs(String externalConfigValue) throws Exception;

  /**
   * An base extension of {@link ExternalConfigLoader} for classes that obtain their configs via a
   * BufferedReader
   */
  private abstract static class ReaderExternalConfigLoader extends ExternalConfigLoader {

    private static final Pattern COMMENT_LINE_PATTERN = Pattern.compile("\\s*(?:#|//).*");

    protected ReaderExternalConfigLoader(String sourceConfig) {
      super(sourceConfig);
    }

    @Override
    public List<String> getConfigArgs(String externalSourceConfig) throws Exception {
      BufferedReader reader = null;
      ImmutableList.Builder<String> configArgs = ImmutableList.builder();
      try {
        reader = getReader(externalSourceConfig);
        String line;
        while ((line = reader.readLine()) != null) {
          line = line.trim();
          if (!line.isEmpty() && !COMMENT_LINE_PATTERN.matcher(line).matches()) {
            configArgs.add(line);
          }
        }
        return configArgs.build();
      } catch (FileNotFoundException ex) {
        throw new ConfigException.ExternalConfigLoadException(externalSourceConfig);
      } finally {
        if (reader != null) {
          reader.close();
        }
      }
    }

    protected abstract BufferedReader getReader(String externalSourceConfig) throws Exception;
  }

  /** An implementation of {@link ExternalConfigLoader} that reads configs from a file */
  private static class FileExternalConfigLoader extends ReaderExternalConfigLoader {

    private static final String CONFIG_FILE_NAME = "config_file";

    protected FileExternalConfigLoader() {
      super(CONFIG_FILE_NAME);
    }

    @Override
    protected BufferedReader getReader(String filename) throws Exception {
      return new BufferedReader(new FileReader(filename));
    }
  }

  /** An implementation of {@link ExternalConfigLoader} that reads configs from a resource. */
  private static class ResourceExternalConfigLoader extends ReaderExternalConfigLoader {

    private static final String CONFIG_RESOURCE_NAME = "config_resource";

    protected ResourceExternalConfigLoader() {
      super(CONFIG_RESOURCE_NAME);
    }

    @Override
    protected BufferedReader getReader(String resource) throws Exception {
      InputStream resourceStream = Configuration.class.getResourceAsStream(resource);
      if (resourceStream == null) {
        // Couldn't find the resource with this class's ClassLoader, so
        // try this thread's ContextClassLoader (if it was set).
        ClassLoader contextClassLoader =
            Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
          resourceStream = contextClassLoader.getResourceAsStream(resource);
        }
      }
      if (resourceStream == null) {
        throw new ConfigException.ExternalConfigLoadException(resource);
      }
      return new BufferedReader(new InputStreamReader(resourceStream));
    }
  }

  /** An extension of {@link ExternalConfigLoader} that reads config values from system properties */
  private static class SystemPropertiesExternalConfigLoader extends ExternalConfigLoader {

    private static final String SYSTEM_PROPERTY_CONFIG_NAME = "system_configs";
    private static final String SYSTEM_PROPERTY_CONFIG_TEMPLATE = "--%s=%s";

    protected SystemPropertiesExternalConfigLoader() {
      super(SYSTEM_PROPERTY_CONFIG_NAME);
    }

    @Override
    public List<String> getConfigArgs(String externalConfigValue) throws Exception {
      ImmutableList.Builder<String> configArgs = ImmutableList.builder();
      for (String systemConfig : Splitter.on(',').split(externalConfigValue)) {
        String value = System.getProperty(systemConfig);
        if (value != null) {
          configArgs.add(String.format(SYSTEM_PROPERTY_CONFIG_TEMPLATE, systemConfig, value));
        }
      }
      return configArgs.build();
    }
  }
}
