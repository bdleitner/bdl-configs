package com.bdl.config.annotation.processor;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import com.bdl.annotation.processing.model.AnnotationMetadata;
import com.bdl.annotation.processing.model.Imports;
import com.bdl.annotation.processing.model.TypeMetadata;
import com.bdl.annotation.processing.model.ValueMetadata;
import com.bdl.annotation.processing.model.Visibility;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    if (configs.isEmpty() && childOutputs.size() < 2) {
      return childOutputs;
    }

    List<String> orderedChildPackages = Lists.newArrayList(childOutputs);
    Collections.sort(orderedChildPackages);
    List<ConfigMetadata> orderedConfigs = Lists.newArrayList(configs);
    Collections.sort(orderedConfigs);

    try {
      Writer writer = writerFunction.apply(PackageNameUtil.append(packageName, "ConfigDaggerModule"));
      writeClassOpening(writer, packageName, orderedChildPackages);
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

  private void writeClassOpening(Writer writer, String packageName, Collection<String> childPackages) throws IOException {
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
      writeLine(writer, "@Module(includes = {%s})",
          childPackages.stream()
              .map((input) -> String.format("%s.ConfigDaggerModule", input))
              .sorted()
              .collect(Collectors.joining(", ")));
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
    TypeMetadata containingType = config.field().containingClass();
    writeLine(writer, "        .packageName(\"%s\")", containingType.packageName());
    writeLine(writer, "        .className(\"%s%s\")",  containingType.nestingPrefix(), containingType.name());
    writeLine(writer, "        .fieldName(\"%s\")", config.field().name());
    // TODO: pass in imports
    writeLine(writer, "        .type(\"%s\")", config.type().toString(Imports.empty()));
    ValueMetadata nameValue = config.configAnnotation().value("name");
    if (nameValue != null) {
      writeLine(writer, "        .specifiedName(\"%s\")", nameValue.value());
    }
    ValueMetadata descriptionValue = config.configAnnotation().value("desc");
    if (descriptionValue != null) {
      writeLine(writer, "        .description(\"%s\")", descriptionValue.value());
    }
    writeLine(writer, "        .build();");

    if (config.field().visibility() == Visibility.PRIVATE) {
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
    Optional<AnnotationMetadata> qualifier = config.qualifier();
    writeLine(writer, "  /** Binds the type of the config with a %s annotation to the Configurable's value. */",
        qualifier.isPresent() ? qualifier.get().type().name() : "ConfigValue");
    writeLine(writer, "  @Provides");
    // TODO: imports and references
    if (qualifier.isPresent()) {
      writeLine(writer, "  %s", qualifier.get().toString(Imports.empty()));
    } else {
      writeLine(writer, "  @ConfigValue(\"%s\")", config.name());
    }

    // TODO: pass in imports.
    writeLine(writer, "  public static %s provideConfigValue_%s(Configuration configuration) {",
        config.type().toString(Imports.empty()), config.name());
    writeLine(writer, "    try {");
    writeLine(writer, "      return (%s) configuration.get(\"%s\");",
        config.type().toString(Imports.empty()), config.fullyQualifiedPathName());
    writeLine(writer, "    } catch (ConfigException ex) {");
    writeLine(writer, "      throw ex.wrap();");
    writeLine(writer, "    }");
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
