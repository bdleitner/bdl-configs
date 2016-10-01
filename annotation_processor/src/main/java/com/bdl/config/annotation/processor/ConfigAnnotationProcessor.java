package com.bdl.config.annotation.processor;

import com.google.common.base.Throwables;

import com.bdl.annotation.processing.model.FieldMetadata;
import com.bdl.config.Config;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Annotation processor for generating Dagger Modules for Configuration.
 *
 * @author Ben Leitner
 */
@SupportedAnnotationTypes("com.bdl.config.Config")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ConfigAnnotationProcessor extends AbstractProcessor {

  private Messager messager;
  private Elements elements;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    messager = processingEnv.getMessager();
    elements = processingEnv.getElementUtils();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    List<ConfigMetadata>  foundConfigs = roundEnv.getElementsAnnotatedWith(Config.class)
        .stream()
        .filter(ConfigAnnotationProcessor::isStaticField)
        .map(element -> ConfigMetadata.from(elements, FieldMetadata.from(element)))
        .collect(Collectors.toList());

    if (foundConfigs.isEmpty()) {
      return true;
    }

    ConfigPackageTree tree = new ConfigPackageTree();
    for (ConfigMetadata config : foundConfigs) {
      tree.addConfig(messager, config);
    }
    tree.pullPublicAndPrivateConfigsUp();

    try {
      messager.printMessage(Diagnostic.Kind.NOTE,
          String.format("Found %s configs.", foundConfigs.size()));

      DaggerModuleFileWriterVisitor daggerVisitor = new DaggerModuleFileWriterVisitor(
          messager, new JavaFileObjectWriterFunction(processingEnv));
      tree.visit(daggerVisitor);

      GuiceModuleFileWriterVisitor guiceVisitor = new GuiceModuleFileWriterVisitor(
          messager, new JavaFileObjectWriterFunction(processingEnv));
      tree.visit(guiceVisitor);

    } catch (Exception ex) {
      messager.printMessage(
          Diagnostic.Kind.ERROR,
          "Error in Config Processor\n" + ex.getMessage() + "\n" + Throwables.getStackTraceAsString(ex));
    }
    return true;
  }

  static boolean isStaticField(Element element) {
    return element.getKind() == ElementKind.FIELD
        && element.getModifiers().contains(Modifier.STATIC);
  }

  static class JavaFileObjectWriterFunction implements Function<String, Writer> {

    private final ProcessingEnvironment env;

    private JavaFileObjectWriterFunction(ProcessingEnvironment env) {
      this.env = env;
    }

    @Override
    public Writer apply(String input) {
      try {
        JavaFileObject jfo = env.getFiler().createSourceFile(input);
        return jfo.openWriter();
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
