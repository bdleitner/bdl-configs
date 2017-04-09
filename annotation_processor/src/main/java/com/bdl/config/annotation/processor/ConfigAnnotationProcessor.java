package com.bdl.config.annotation.processor;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;

import com.bdl.annotation.processing.model.FieldMetadata;
import com.bdl.config.Config;

import com.sun.source.util.Trees;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
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
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ConfigAnnotationProcessor extends AbstractProcessor {

  private Messager messager;
  private Elements elements;
  private Trees trees;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    messager = processingEnv.getMessager();
    elements = processingEnv.getElementUtils();
    trees = Trees.instance(processingEnv);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    List<ConfigMetadata> foundConfigs = FluentIterable.from(roundEnv.getElementsAnnotatedWith(Config.class))
    .filter(new Predicate<Element>() {
      @Override
      public boolean apply(@Nullable Element input) {
        return isStaticField(input);
      }
    }).transform(new Function<Element, ConfigMetadata>() {
          @Nullable
          @Override
          public ConfigMetadata apply(@Nullable Element input) {
            return ConfigMetadata.from(
                elements,
                FieldMetadata.from(input),
                new FieldInitializationGrabber(trees, elements));
          }
        }).toList();

    if (foundConfigs.isEmpty()) {
      return true;
    }

    ConfigPackageTree tree = new ConfigPackageTree();
    for (ConfigMetadata config : foundConfigs) {
      tree.addConfig(messager, config);
    }
    tree.pullPublicAndPrivateConfigsUp();

    try {
      messager.printMessage(
          Diagnostic.Kind.NOTE, String.format("Found %s configs.", foundConfigs.size()));

      DaggerModuleFileWriterVisitor daggerVisitor =
          new DaggerModuleFileWriterVisitor(
              messager, new JavaFileObjectWriterFunction(processingEnv));
      tree.visit(daggerVisitor);

      GuiceModuleFileWriterVisitor guiceVisitor =
          new GuiceModuleFileWriterVisitor(
              messager, new JavaFileObjectWriterFunction(processingEnv));
      tree.visit(guiceVisitor);

    } catch (Exception ex) {
      messager.printMessage(
          Diagnostic.Kind.ERROR,
          "Error in Config Processor\n"
              + ex.getMessage()
              + "\n"
              + Throwables.getStackTraceAsString(ex));
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
