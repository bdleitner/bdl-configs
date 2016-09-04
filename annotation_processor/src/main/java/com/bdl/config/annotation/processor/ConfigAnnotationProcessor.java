package com.bdl.config.annotation.processor;

import com.google.common.base.Function;

import com.bdl.config.Config;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
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

  private Messager messager;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    messager = processingEnv.getMessager();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    List<ConfigMetadata>  foundConfigs = new ArrayList<>();

    for (Element element : roundEnv.getElementsAnnotatedWith(Config.class)) {
      if (element.getKind() == ElementKind.FIELD) {
        foundConfigs.add(ConfigMetadata.fromField(element));
      }
    }
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
      DaggerModuleFileWriterVisitor visitor = new DaggerModuleFileWriterVisitor(
          messager, new JavaFileObjectWriterFunction(processingEnv, "ConfigDaggerModule"));
      tree.visit(visitor);

    } catch (Exception ex) {
      messager.printMessage(
          Diagnostic.Kind.ERROR,
          ex.getMessage());
    }
    return true;
  }

  private static class JavaFileObjectWriterFunction implements Function<String, Writer> {

    private final ProcessingEnvironment env;
    private final String className;

    private JavaFileObjectWriterFunction(ProcessingEnvironment env, String className) {
      this.env = env;
      this.className = className;
    }

    @Override
    public Writer apply(String input) {
      try {
        JavaFileObject jfo = env.getFiler().createSourceFile(String.format("%s.%s", input, className));
        return jfo.openWriter();
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
