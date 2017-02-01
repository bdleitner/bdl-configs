package com.bdl.config.annotation.processor;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;

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
 * Implementation of {@link ConfigPackageTree.Visitor} that writes out Guice Module files.
 *
 * @author Ben Leitner
 */
class GuiceModuleFileWriterVisitor implements ConfigPackageTree.Visitor<String> {

  private final Messager messager;
  private final Function<String, Writer> writerFunction;

  GuiceModuleFileWriterVisitor(Messager messager, Function<String, Writer> writerFunction) {
    this.messager = messager;
    this.writerFunction = writerFunction;
  }

  @Override
  public Set<String> visit(
      Set<String> childOutputs, String packageName, Set<ConfigMetadata> configs) {
    if (configs.isEmpty() && childOutputs.size() < 2) {
      return childOutputs;
    }

    List<String> orderedChildPackages = childOutputs.stream().sorted().collect(Collectors.toList());
    List<ConfigMetadata> orderedConfigs = configs.stream().sorted().collect(Collectors.toList());

    ImmutableSet.Builder<TypeMetadata> referencedTypes =
        ImmutableSet.<TypeMetadata>builder()
            .add(TypeMetadata.from(ConfigDescription.class))
            .add(TypeMetadata.from(ConfigException.class))
            .add(TypeMetadata.from(ConfigSupplier.class))
            .add(TypeMetadata.from(ConfigValue.class))
            .add(TypeMetadata.from(Configuration.class))
            .add(TypeMetadata.from(AbstractModule.class))
            .add(TypeMetadata.from(Provides.class))
            .add(TypeMetadata.from(Multibinder.class));
    configs.forEach(config -> referencedTypes.addAll(config.getAllTypes()));

    Imports imports = Imports.create(packageName, referencedTypes.build());

    try {
      Writer writer =
          writerFunction.apply(PackageNameUtil.append(packageName, "ConfigGuiceModule"));
      writeClassOpening(writer, imports, packageName);
      writeConfigureMethod(writer, orderedChildPackages, orderedConfigs);
      for (ConfigMetadata config : orderedConfigs) {
        writeConfigSupplierBinding(writer, imports, config);
        writeConfigValueBinding(writer, imports, config);
      }
      writeClassClosing(writer);

      writer.close();
    } catch (IOException ex) {
      messager.printMessage(Diagnostic.Kind.ERROR, Throwables.getStackTraceAsString(ex));
    }

    return ImmutableSet.of(packageName);
  }

  private void writeClassOpening(Writer writer, Imports imports, String packageName)
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
    writeLine(writer, "/** Guice module for binding configs in the %s package. */", packageName);

    writeLine(writer, "public class ConfigGuiceModule extends AbstractModule {");
  }

  private void writeConfigureMethod(
      Writer writer, Iterable<String> childPackages, Collection<ConfigMetadata> configs)
      throws IOException {
    writeLine(writer, "");
    writeLine(writer, "  @Override");
    writeLine(writer, "  protected void configure() {");
    boolean needsNewLine = false;
    for (String childPackage : childPackages) {
      needsNewLine = true;
      writeLine(writer, "    install(new %s.ConfigGuiceModule());", childPackage);
    }
    if (!configs.isEmpty()) {
      if (needsNewLine) {
        writeLine(writer, "");
      }
      writeLine(
          writer,
          "    Multibinder<ConfigSupplier> supplierBinder = Multibinder.newSetBinder(binder(), ConfigSupplier.class);");
      for (ConfigMetadata config : configs) {
        writeLine(writer, "    bindConfigSupplier_%s(supplierBinder);", config.name());
      }
    }
    writeLine(writer, "  }");
  }

  private void writeConfigSupplierBinding(Writer writer, Imports imports, ConfigMetadata config)
      throws IOException {
    writeLine(writer, "");
    writeLine(
        writer,
        "  /** Binds a ConfigSupplier for config %s (%s) to the set multibinder. */",
        config.name(),
        config.fieldReference(imports));
    TypeMetadata containingType = config.field().containingClass();
    writeLine(
        writer,
        "  private void bindConfigSupplier_%s(Multibinder<ConfigSupplier> binder) {",
        config.name());
    writeLine(writer, "    ConfigDescription description = ConfigDescription.builder()");
    writeLine(writer, "        .packageName(\"%s\")", containingType.packageName());
    writeLine(
        writer,
        "        .className(\"%s%s\")",
        containingType.nestingPrefix(),
        containingType.name());
    writeLine(writer, "        .fieldName(\"%s\")", config.field().name());
    // TODO: pass in imports
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
    writeLine(writer, "    binder.addBinding().toInstance(");

    if (config.field().visibility() == Visibility.PRIVATE) {
      // Must use a reflective supplier
      writeLine(writer, "        ConfigSupplier.reflective(description));");
    } else {
      // Either public or protected/package-local and we are in the same package, so direct access ok.
      writeLine(
          writer,
          "        ConfigSupplier.simple(description, %s));",
          config.fieldReference(imports));
    }
    writeLine(writer, "  }");
  }

  private void writeConfigValueBinding(Writer writer, Imports imports, ConfigMetadata config)
      throws IOException {
    writeLine(writer, "");
    Optional<AnnotationMetadata> bindingAnnotation = config.bindingAnnotation();
    writeLine(
        writer,
        "  /** Binds the type of the config with a %s annotation to the Configurable's value. */",
        bindingAnnotation.isPresent() ? bindingAnnotation.get().type().name() : "ConfigValue");
    writeLine(writer, "  @Provides");
    if (bindingAnnotation.isPresent()) {
      // TODO: imports and references
      writeLine(writer, "  %s", bindingAnnotation.get().toString(imports));
    } else {
      writeLine(writer, "  @ConfigValue(\"%s\")", config.name());
    }

    writeLine(
        writer,
        "  %s provideConfigValue_%s(Configuration configuration) {",
        config.type().toString(imports),
        config.name());
    writeLine(writer, "    try {");
    writeLine(
        writer,
        "      return (%s) configuration.get(\"%s\");",
        config.type().toString(imports),
        config.fullyQualifiedPathName());
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
