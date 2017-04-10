package com.bdl.config.annotation.processor;

import com.sun.source.util.Trees;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.util.Elements;

/**
 * Annotation processor for generating Guice Modules for Configuration.
 *
 * @author Ben Leitner
 */
@SupportedAnnotationTypes("com.bdl.config.Config")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class GuiceConfigAnnotationProcessor extends ConfigAnnotationProcessor {

  private Messager messager;
  private Elements elements;
  private Trees trees;

  public GuiceConfigAnnotationProcessor() {
    super(new VisitorFactory() {
      @Override
      public ConfigPackageTree.Visitor<String> get(ProcessingEnvironment env, Messager messager) {
        return new GuiceModuleFileWriterVisitor(
            messager, new JavaFileObjectWriterFunction(env));
      }
    });
  }
}
