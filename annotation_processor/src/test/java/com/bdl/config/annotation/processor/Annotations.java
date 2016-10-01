package com.bdl.config.annotation.processor;

import com.google.auto.value.AutoAnnotation;
import com.google.inject.BindingAnnotation;

import com.bdl.annotation.processing.model.AnnotationMetadata;
import com.bdl.annotation.processing.model.TypeMetadata;
import com.bdl.annotation.processing.model.ValueMetadata;
import com.bdl.config.Config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.Nullable;
import javax.inject.Qualifier;

/**
 * Container class for annotations used in testing.
 *
 * @author Ben Leitner
 */
public class Annotations {
  private Annotations() {
    // Container class, no instantiation.
  }

  /** Dummy Qualifier annotation for use in tests. */
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
  public @interface DummyQualifier {
    String value() default "";
  }

  /** Dummy BindingAnnotation annotation for use in tests. */
  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
  public @interface DummyBindingAnnotation {
    String value() default "";
  }

  @AutoAnnotation
  public static DummyQualifier qualifier(String value) {
    return new AutoAnnotation_Annotations_qualifier(value);
  }

  @AutoAnnotation
  public static DummyBindingAnnotation bindingAnnotation(String value) {
    return new AutoAnnotation_Annotations_bindingAnnotation(value);
  }

  public static AnnotationMetadata configMetadata() {
    return configMetadata(null);
  }

  public static AnnotationMetadata configMetadata(@Nullable String name) {
    return configMetadata(name, null);
  }

  public static AnnotationMetadata configMetadata(@Nullable String name, @Nullable String desc) {
    AnnotationMetadata.Builder annotation = AnnotationMetadata.builder()
        .setType(TypeMetadata.from(Config.class));
    if (name != null) {
      annotation.putValue("name", ValueMetadata.create(name));
    }
    if (desc != null) {
      annotation.putValue("desc", ValueMetadata.create(desc));
    }
    return annotation.build();
  }

  public static AnnotationMetadata qualifierMetadata(@Nullable String value) {
    AnnotationMetadata.Builder annotation = AnnotationMetadata.builder()
        .setType(TypeMetadata.from(DummyQualifier.class));
    if (value != null) {
      annotation.putValue("value", ValueMetadata.create(value));
    }
    return annotation.build();
  }

  public static AnnotationMetadata bindingAnnotationMetadata(@Nullable String value) {
    AnnotationMetadata.Builder annotation = AnnotationMetadata.builder()
        .setType(TypeMetadata.from(DummyBindingAnnotation.class));
    if (value != null) {
      annotation.putValue("value", ValueMetadata.create(value));
    }
    return annotation.build();
  }
}
