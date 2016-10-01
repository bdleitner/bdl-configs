package com.bdl.config.annotation.processor;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;

import com.bdl.annotation.processing.model.AnnotationMetadata;
import com.bdl.annotation.processing.model.Imports;
import com.bdl.annotation.processing.model.TypeMetadata;
import com.bdl.annotation.processing.model.ValueMetadata;
import com.bdl.annotation.processing.model.Visibility;
import com.bdl.config.ConfigDescription;
import com.bdl.config.ConfigException;
import com.bdl.config.ConfigSupplier;
import com.bdl.config.ConfigValue;
import com.bdl.config.Configuration;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
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

    List<String> orderedChildPackages = childOutputs.stream().sorted().collect(Collectors.toList());
    List<ConfigMetadata> orderedConfigs = configs.stream().sorted().collect(Collectors.toList());

    ImmutableSet.Builder<TypeMetadata> referencedTypes = ImmutableSet.<TypeMetadata>builder()
        .add(TypeMetadata.from(ConfigDescription.class))
        .add(TypeMetadata.from(ConfigException.class))
        .add(TypeMetadata.from(ConfigSupplier.class))
        .add(TypeMetadata.from(ConfigValue.class))
        .add(TypeMetadata.from(Configuration.class))
        .add(TypeMetadata.from(Module.class))
        .add(TypeMetadata.from(Provides.class))
        .add(TypeMetadata.from(IntoSet.class));
    configs.forEach(config -> referencedTypes.addAll(config.getAllTypes()));

    Imports imports = Imports.create(packageName, referencedTypes.build());

    try {
      Writer writer = writerFunction.apply(PackageNameUtil.append(packageName, "ConfigDaggerModule"));
      writeClassOpening(writer, imports, packageName, orderedChildPackages);
      for (ConfigMetadata config : orderedConfigs) {
        writeConfigSupplierBinding(writer, imports, config);
        writeConfigValueBinding(writer, imports, config);
      }
      writeClassClosing(writer);

      writer.close();
    } catch (IOException ex) {
      messager.printMessage(Diagnostic.Kind.ERROR,
          Throwables.getStackTraceAsString(ex));
    }

    return ImmutableSet.of(packageName);
  }

  private void writeClassOpening(Writer writer, Imports imports, String packageName, Collection<String> childPackages)
      throws IOException {
    writeLine(writer, "package %s;", packageName);
    String previous = null;
    for (String importString : imports.getImports()) {
      if (ImportUtil.needsNewLine(previous, importString)) {
        writeLine(writer, "");
      }
      previous = importString;
      writeLine(writer, "import %s;", importString);
    }
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

  private void writeConfigSupplierBinding(Writer writer, Imports imports, ConfigMetadata config) throws IOException {
    writeLine(writer, "");
    writeLine(writer, "  /** Adds a ConfigSupplier for config %s (%s) to the set multibinder. */",
        config.name(), config.fieldReference(imports));
    writeLine(writer, "  @Provides");
    writeLine(writer, "  @IntoSet");
    writeLine(writer, "  public static ConfigSupplier provideConfigSupplier_%s() {", config.name());
    writeLine(writer, "    ConfigDescription description = ConfigDescription.builder()");
    TypeMetadata containingType = config.field().containingClass();
    writeLine(writer, "        .packageName(\"%s\")", containingType.packageName());
    writeLine(writer, "        .className(\"%s%s\")",  containingType.nestingPrefix(), containingType.name());
    writeLine(writer, "        .fieldName(\"%s\")", config.field().name());
    writeLine(writer, "        .type(\"%s\")", config.type().toString(imports));
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
      writeLine(writer, "    return ConfigSupplier.simple(description, %s);", config.fieldReference(imports));
    }
    writeLine(writer, "  }");
  }

  private void writeConfigValueBinding(Writer writer, Imports imports, ConfigMetadata config) throws IOException {
    writeLine(writer, "");
    Optional<AnnotationMetadata> qualifier = config.qualifier();
    writeLine(writer, "  /** Binds the type of the config with a %s annotation to the Configurable's value. */",
        qualifier.isPresent() ? qualifier.get().type().name() : "ConfigValue");
    writeLine(writer, "  @Provides");
    if (qualifier.isPresent()) {
      writeLine(writer, "  %s", qualifier.get().toString(imports));
    } else {
      writeLine(writer, "  @ConfigValue(\"%s\")", config.name());
    }

    writeLine(writer, "  public static %s provideConfigValue_%s(Configuration configuration) {",
        config.type().toString(imports), config.name());
    writeLine(writer, "    try {");
    writeLine(writer, "      return (%s) configuration.get(\"%s\");",
        config.type().toString(imports), config.fullyQualifiedPathName());
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
