package com.bdl.config.annotation.processor;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;

/**
 * Implementation of {@link ConfigPackageTree.Visitor} that writes out Dagger Module files.
 *
 * @author Ben Leitner
 */
class DaggerModuleFileWriterVisitor implements ConfigPackageTree.Visitor<String> {

  private final Messager messager;
  private final Function<String, Writer> writerFunction;

  DaggerModuleFileWriterVisitor(Messager messager, Function<String, Writer> writerFunction) {
    this.messager = messager;
    this.writerFunction = writerFunction;
  }

  @Override
  public Set<String> visit(Set<String> childOutputs, String packageName, Set<ConfigMetadata> configs) {
    if (configs.isEmpty()) {
      return childOutputs;
    }

    List<ConfigMetadata> orderedConfigs = Lists.newArrayList(configs);
    Collections.sort(orderedConfigs);

    try {
      Writer writer = writerFunction.apply(PackageNameUtil.append(packageName, "ConfigDaggerModule"));
      writeClassOpening(writer, packageName, childOutputs);
      for (ConfigMetadata config : orderedConfigs) {
        writeConfigSupplierBinding(writer, config);
        writeConfigValueBinding(writer, config);
      }
      writeClassClosing(writer);

      writer.close();
    } catch (IOException ex) {
      messager.printMessage(Diagnostic.Kind.ERROR,
          Throwables.getStackTraceAsString(ex));
    }

    return ImmutableSet.of(packageName);
  }

  private void writeClassOpening(Writer writer, String packageName, Set<String> childPackages) throws IOException {
    writeLine(writer, "package %s;", packageName);
    writeLine(writer, "");
    writeLine(writer, "import com.bdl.config.ConfigDescription;");
    writeLine(writer, "import com.bdl.config.ConfigException;");
    writeLine(writer, "import com.bdl.config.ConfigSupplier;");
    writeLine(writer, "import com.bdl.config.ConfigValue;");
    writeLine(writer, "import com.bdl.config.Configuration;");
    writeLine(writer, "");
    writeLine(writer, "import dagger.Module;");
    writeLine(writer, "import dagger.Provides;");
    writeLine(writer, "import dagger.multibindings.IntoSet;");
    writeLine(writer, "");
    writeLine(writer, "/** Dagger module for binding configs in the %s package. */", packageName);

    if (childPackages.isEmpty()) {
      writeLine(writer, "@Module");
    } else {
      writeLine(writer, "@Module(includes = {%s})", Joiner.on(", ").join(
          Iterables.transform(childPackages, new Function<String, String>() {
            @Override
            public String apply(String input) {
              return String.format("%s.ConfigDaggerModule", input);
            }
          })));
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
    writeLine(writer, "        .packageName(\"%s\")", config.packageName());
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
    Optional<String> qualifier = config.qualifier();
    writeLine(writer, "  /** Binds the type of the config with a %s annotation to the Configurable's value. */",
        qualifier.isPresent() ? simpleQualifierName(qualifier.get()) : "ConfigValue");
    writeLine(writer, "  @Provides");
    if (qualifier.isPresent()) {
      writeLine(writer, "  %s", qualifier.get());
    } else {
      writeLine(writer, "  @ConfigValue(\"%s\")", config.name());
    }

    writeLine(writer, "  public static %s provideConfigValue_%s(Configuration configuration) {",
        config.type(), config.name());
    writeLine(writer, "    try {");
    writeLine(writer, "      return (%s) configuration.get(\"%s\");",
        config.type(), config.fullyQualifiedPathName());
    writeLine(writer, "    } catch (ConfigException ex) {");
    writeLine(writer, "      throw ex.wrap();");
    writeLine(writer, "    }");
    writeLine(writer, "  }");
  }

  private String simpleQualifierName(String fullName) {
    int lastDot = fullName.lastIndexOf('.');
    int openParen = fullName.indexOf('(');
    return fullName.substring(lastDot + 1, openParen);
  }

  private void writeClassClosing(Writer writer) throws IOException {
    writeLine(writer, "}");
  }

  private void writeLine(Writer writer, String template, Object... params) throws IOException {
    writer.write(String.format(template, params));
    writer.write("\n");
  }

}
