package com.bdl.config.annotation.processor;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A representation of a Dagger Module file that can write itself out, given a Function that provides
 * writers for strings.
 *
 * @author Ben Leitner
 */
class DaggerModuleFile {

  private final String packageName;
  private final String fullyQualifiedFileName;
  private final List<ConfigMetadata> configs;
  private final Set<DaggerModuleFile> subpackageFiles;

  DaggerModuleFile(
      String fullyQualifiedFileName,
      Set<ConfigMetadata> configs,
      Set<DaggerModuleFile> subpackageFiles) {
    this.fullyQualifiedFileName = fullyQualifiedFileName;
    int lastDot = fullyQualifiedFileName.lastIndexOf('.');
    packageName = lastDot < 0 ? "<empty>" : fullyQualifiedFileName.substring(0, lastDot);
    this.configs = Lists.newArrayList();
    this.configs.addAll(configs);
    Collections.sort(this.configs);
    this.subpackageFiles = subpackageFiles;
  }

  private Set<String> getNonemptyImmediateChildFilenames() {
    ImmutableSet.Builder<String> names = ImmutableSet.builder();
    for (DaggerModuleFile file : subpackageFiles) {
      if (file.configs.isEmpty()) {
        names.addAll(file.getNonemptyImmediateChildFilenames());
      } else {
        names.add(file.fullyQualifiedFileName);
      }
    }
    return names.build();
  }

  void write(Function<String, Writer> writerFunction) throws IOException {
    for (DaggerModuleFile subpackageFile : subpackageFiles) {
      subpackageFile.write(writerFunction);
    }
    if (configs.isEmpty()) {
      return;
    }
    Writer writer = writerFunction.apply(fullyQualifiedFileName);
    writeClassOpening(writer);
    for (ConfigMetadata config : configs) {
      writeConfigSupplierBinding(writer, config);
      writeConfigValueBinding(writer, config);
    }
    writeClassClosing(writer);

    writer.close();
  }

  private void writeClassOpening(Writer writer) throws IOException {
    writeLine(writer, "package %s;", packageName);
    writeLine(writer, "");
    writeLine(writer, "import com.bdl.config.Configuration;");
    writeLine(writer, "import com.bdl.config.ConfigDescription;");
    writeLine(writer, "import com.bdl.config.ConfigSupplier;");
    writeLine(writer, "import com.bdl.config.ConfigValue;");
    writeLine(writer, "");
    writeLine(writer, "import dagger.Module;");
    writeLine(writer, "import dagger.Provides;");
    writeLine(writer, "import dagger.multibindings.IntoSet;");
    writeLine(writer, "");
    writeLine(writer, "/** Dagger module for binding configs in the %s package. */", packageName);

    Set<String> includes = getNonemptyImmediateChildFilenames();
    if (includes.isEmpty()) {
      writeLine(writer, "@Module");
    } else {
      writeLine(writer, "@Module(includes = {%s})", Joiner.on(", ").join(includes));
    }
    writeLine(writer, "public class ConfigDaggerModule {");
  }

  private void writeConfigSupplierBinding(Writer writer, ConfigMetadata config) throws IOException {
    writeLine(writer, "");
    writeLine(writer, "  /** Adds a ConfigSupplier for config %s (%s) to the set multibinder. */",
        config.name(), config.fullyQualifiedPathName());
    writeLine(writer, "  @Provides");
    writeLine(writer, "  @IntoSet");
    writeLine(writer, "  public static ConfigSupplier provideConfigSupplier_%s() {", config.name());
    writeLine(writer, "    ConfigDescription description = ConfigDescription.builder()");
    writeLine(writer, "        .className(\"%s\")", config.className());
    writeLine(writer, "        .fieldName(\"%s\")", config.fieldName());
    writeLine(writer, "        .type(\"%s\")", config.type());
    if (config.specifiedName().isPresent()) {
      writeLine(writer, "        .specifiedName(\"%s\")", config.specifiedName().get());
    }
    if (config.description().isPresent()) {
      writeLine(writer, "        .description(\"%s\")", config.description());
    }
    writeLine(writer, "        .build();");

    if (config.visibility() == ConfigMetadata.Visibility.PRIVATE) {
      // Must use a reflective supplier
      writeLine(writer, "    return ConfigSupplier.reflective(description);");
    } else {
      // Either public or protected/package-local and we are in the same package, so direct access ok.
      writeLine(writer, "    return ConfigSupplier.simple(description, %s);", config.fullyQualifiedPathName());
    }
    writeLine(writer, "  }");
  }

  private void writeConfigValueBinding(Writer writer, ConfigMetadata config) throws IOException {
    writeLine(writer, "");
    writeLine(writer, "  /** Binds the type of the config with a ConfigValue annotation to the Configurable's value. */");
    writeLine(writer, "  @Provides");
    Optional<String> qualifier = config.qualifier();
    if (qualifier.isPresent()) {
      writeLine(writer, "  %s", qualifier.get());
    } else {
      writeLine(writer, "  @ConfigValue(\"%s\")", config.name());
    }

    writeLine(writer, "  public static %s provideConfigValue_%s(Configuration configuration) {",
        config.type(), config.name());
    writeLine(writer, "    return configuration.get(\"%s\");", config.fullyQualifiedPathName());
    writeLine(writer, "  }");
  }

  private void writeClassClosing(Writer writer) throws IOException {
    writeLine(writer, "}");
  }

  private void writeLine(Writer writer, String template, Object... params) throws IOException {
    writer.write(String.format(template, params));
    writer.write("\n");
  }

}
