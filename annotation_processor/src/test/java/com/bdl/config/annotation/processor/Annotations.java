package com.bdl.config.annotation.processor;

import com.google.auto.value.AutoAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
  public static @interface DummyQualifier {
    String value() default "";
  }

  @AutoAnnotation
  public static DummyQualifier qualifier(String value) {
    return new AutoAnnotation_Annotations_qualifier(value);
  }
}