package com.bdl.config.annotation.processor;

import com.bdl.config.Config;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Annotation processor for generating Dagger Modules for Configuration.
 *
 * @author Ben Leitner
 */
@SupportedAnnotationTypes("com.bdl.config.Config")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ConfigAnnotationProcessor extends AbstractProcessor {

  private static final String PACKAGE_NAME = "com.bdl";
  private Messager messager;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    messager = processingEnv.getMessager();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    List<Map<String, String>>  foundConfigs = new ArrayList<>();

    for (Element e : roundEnv.getElementsAnnotatedWith(Config.class)) {
      if (e.getKind() == ElementKind.FIELD) {
        foundConfigs.add(getMapFor(e));
      }
    }
    if (foundConfigs.isEmpty()) {
      return true;
    }

    try {
      messager.printMessage(Diagnostic.Kind.NOTE,
          String.format("Found %s configs, want to write.", foundConfigs.size()));
      JavaFileObject jfo = processingEnv.getFiler().createSourceFile("com.bdl.ConfigDaggerModule");
      Writer writer = jfo.openWriter();
      writeClassOpening(writer);
      for (Map<String, String> config : foundConfigs) {
        writeMethodsForConfig(writer, config);
      }
      writeClassClosing(writer);
      writer.close();
    } catch (IOException ex) {
      messager.printMessage(
          Diagnostic.Kind.ERROR,
          ex.getMessage());
    }
    return true;
  }

  private void writeClassOpening(Writer writer) throws IOException {
    writeLine(writer, "package com.bdl;");
    writeLine(writer, "");
    writeLine(writer, "import dagger.Module;");
    writeLine(writer, "import dagger.Provides;");
    writeLine(writer, "import dagger.multibindings.StringKey;");
    writeLine(writer, "import dagger.multibindings.IntoMap;");
    writeLine(writer, "import com.bdl.config.ConfigValue;");
    writeLine(writer, "import com.bdl.config.ConfigDescription;");
    writeLine(writer, "");
    writeLine(writer, "/** Dagger module for binding configs in the com.bdl package. */");
    writeLine(writer, "@Module");
    writeLine(writer, "public class ConfigDaggerModule {");
  }

  private void writeMethodsForConfig(Writer writer, Map<String, String> config) throws IOException {
    writeConfigDescriptionBinding(writer, config);
    writeConfigValueBinding(writer, config);
  }

  private void writeConfigDescriptionBinding(Writer writer, Map<String, String> config) throws IOException {
    writeLine(writer, "");
    writeLine(writer,
        String.format("  /** Adds a ConfigDescription to the map multibinder for the %s key. */",
        config.get("name")));
    writeLine(writer, "  @Provides");
    writeLine(writer, "  @IntoMap");
    writeLine(writer, String.format("  @StringKey(\"%s\")", config.get("name")));
    writeLine(writer,
        String.format("  public static ConfigDescription provideConfigDescription_%s() {",
        config.get("name")));
    writeLine(writer, "    return ConfigDescription.builder()");
    writeLine(writer, String.format("        .className(\"%s\")", config.get("className")));
    writeLine(writer, String.format("        .fieldName(\"%s\")", config.get("fieldName")));
    writeLine(writer, String.format("        .type(\"%s\")", config.get("type")));
    if (!config.get("specifiedName").isEmpty()) {
      writeLine(writer, String.format("        .specifiedName(\"%s\")", config.get("specifiedName")));
    }
    if (!config.get("description").isEmpty()) {
      writeLine(writer, String.format("        .description(\"%s\")", config.get("description")));
    }
    writeLine(writer, "        .build();");
    writeLine(writer, "  }");
  }

  private void writeConfigValueBinding(Writer writer, Map<String, String> config) throws IOException {
    writeLine(writer, "");
    writeLine(writer, "  /** Binds the type of the config with a ConfigValue annotation to the Configurable's value. */");
    writeLine(writer, "  @Provides");
    writeLine(writer, String.format("  @ConfigValue(\"%s\")", config.get("name")));
    writeLine(writer,
        String.format("  public static %s provideConfigValue_%s() {",
        config.get("type"), config.get("name")));
    writeLine(writer, String.format("    return %s.%s.get();", config.get("className"), config.get("fieldName")));
    writeLine(writer, "  }");
  }

  private void writeClassClosing(Writer writer) throws IOException {
    writeLine(writer, "}");
  }

  private void writeLine(Writer writer, String line) throws IOException {
    writer.write(line);
    writer.write("\n");
  }

  private Map<String, String> getMapFor(Element element) {
    Map<String, String> map = new HashMap<>();
    Element enclosingClass = element.getEnclosingElement();
    map.put("className", ((TypeElement) enclosingClass).getQualifiedName().toString());
    map.put("fieldName", element.getSimpleName().toString());

    TypeMirror type = element.asType();
    String configType = ((DeclaredType) type).getTypeArguments().get(0).toString();
    map.put("type", configType);

    Config annotation = element.getAnnotation(Config.class);
    map.put("description", annotation.desc());
    map.put("specifiedName", annotation.name());

    map.put(
        "name",
        annotation.name().isEmpty()
            ? map.get("fieldName")
            : map.get("specifiedName"));
    return map;
  }
}
