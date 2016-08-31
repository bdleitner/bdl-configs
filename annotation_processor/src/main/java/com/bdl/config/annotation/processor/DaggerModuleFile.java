package com.bdl.config.annotation.processor;

import com.google.common.base.Function;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * A representation of a Dagger Module file that can write itself out, given a Function that provides
 * writers for strings.
 *
 * @author Ben Leitner
 */
class DaggerModuleFile {

  private final String fullyQualifiedName;
  private final Set<ConfigMetadata> configs;
  private final Set<DaggerModuleFile> subpackageFiles;

  DaggerModuleFile(
      String fullyQualifiedName,
      Set<ConfigMetadata> configs,
      Set<DaggerModuleFile> subpackageFiles) {
    this.fullyQualifiedName = fullyQualifiedName;
    this.configs = configs;
    this.subpackageFiles = subpackageFiles;
  }

  void write(Function<String, Writer> writerFunction) throws IOException {
    for (DaggerModuleFile subpackageFile : subpackageFiles) {
      subpackageFile.write(writerFunction);
    }
    Writer writer = writerFunction.apply(fullyQualifiedName);
    writeClassOpening(writer);
    for (ConfigMetadata config : configs) {
      writeConfigSupplierBinding(writer, config);
      writeConfigValueBinding(writer, config);
    }

    writer.close();
  }

  private void writeClassOpening(Writer writer) throws IOException {
    String packageName = fullyQualifiedName.substring(0, fullyQualifiedName.lastIndexOf('.'));
    writeLine(writer, "package %s", packageName);
    writeLine(writer, "");
    writeLine(writer, "import com.bdl.config.ConfigDescription;");
    writeLine(writer, "import com.bdl.config.ConfigSupplier;");
    writeLine(writer, "import com.bdl.config.ConfigValue;");
    writeLine(writer, "");
    writeLine(writer, "import dagger.Module;");
    writeLine(writer, "import dagger.Provides;");
    writeLine(writer, "import dagger.multibindings.IntoSet;");
    writeLine(writer, "");
    writeLine(writer, "/** Dagger module for binding configs in the %s package. */", packageName);
    writeLine(writer, "@Module");
    writeLine(writer, "public class ConfigDaggerModule {");
  }

  private void writeMethodsForConfig(Writer writer, ConfigMetadata config) throws IOException {
    writeConfigSupplierBinding(writer, config);
    writeConfigValueBinding(writer, config);
  }

  private void writeConfigSupplierBinding(Writer writer, ConfigMetadata config) throws IOException {
    writeLine(writer, "");
    writeLine(writer, "  /** Adds a ConfigSupplier for config %s (%s.%s) to the set multibinder. */",
        config.name(), config.className(), config.fieldName());
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

    if (config.fieldModifiers().contains(Modifier.PRIVATE)) {
      // Must use a reflective supplier
      writeLine(writer, "    return ConfigSupplier.reflective(description);");
    } else {
      // Either public or protected/package-local and we are in the same package, so direct access ok.
      writeLine(writer, "    return ConfigSupplier.simple(description, %s.%s);",
          config.className(), config.fieldName());
    }
    writeLine(writer, "  }");
  }

  private void writeConfigValueBinding(Writer writer, ConfigMetadata config) throws IOException {
    writeLine(writer, "");
    writeLine(writer, "  /** Binds the type of the config with a ConfigValue annotation to the Configurable's value. */");
    writeLine(writer, "  @Provides");
    writeLine(writer, "  @ConfigValue(\"%s\")", config.name());
    writeLine(writer, "  public static %s provideConfigValue_%s(Configuration configuration) {",
        config.type(), config.name());
    writeLine(writer, "    throw new UnsupportedOperationException(\"Not Implemented Yet!\");");
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
