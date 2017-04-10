package com.bdl.config.annotation.processor;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * Annotation processor for generating Dagger Modules for Configuration.
 *
 * @author Ben Leitner
 */
@SupportedAnnotationTypes("com.bdl.config.Config")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class DaggerConfigAnnotationProcessor extends ConfigAnnotationProcessor {

  public DaggerConfigAnnotationProcessor() {
    super(new VisitorFactory() {
      @Override
      public ConfigPackageTree.Visitor<String> get(ProcessingEnvironment env, Messager messager) {
        return new DaggerModuleFileWriterVisitor(
            messager, new JavaFileObjectWriterFunction(env));
      }
    });
  }
}
