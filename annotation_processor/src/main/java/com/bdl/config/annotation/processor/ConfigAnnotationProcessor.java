package com.bdl.config.annotation.processor;

import com.bdl.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

/**
 * Annotation processor for generating Dagger Modules for Configuration.
 *
 * @author Ben Leitner
 */
@SupportedAnnotationTypes("com.bdl.config.Config")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ConfigAnnotationProcessor extends AbstractProcessor {

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
      tree.addConfig(config);
    }
    tree.pushPublicAndPrivateConfigsDown();

    // TODO: Finish this.
//    try {
//      messager.printMessage(Diagnostic.Kind.NOTE,
//          String.format("Found %s configs, want to write.", foundConfigs.size()));
//      JavaFileObject jfo = processingEnv.getFiler().createSourceFile("com.bdl.ConfigDaggerModule");
//      Writer writer = jfo.openWriter();
//      writeClassOpening(writer);
//      for (ConfigMetadata config : foundConfigs) {
//        writeMethodsForConfig(writer, config);
//      }
//      writeClassClosing(writer);
//      writer.close();
//    } catch (IOException ex) {
//      messager.printMessage(
//          Diagnostic.Kind.ERROR,
//          ex.getMessage());
//    }
    return true;
  }
}
