package com.bdl.config.annotation.processor;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.inject.BindingAnnotation;

import com.bdl.annotation.processing.model.AnnotationMetadata;
import com.bdl.annotation.processing.model.ClassMetadata;
import com.bdl.annotation.processing.model.FieldMetadata;
import com.bdl.annotation.processing.model.Imports;
import com.bdl.annotation.processing.model.TypeMetadata;
import com.bdl.annotation.processing.model.UsesTypes;
import com.bdl.annotation.processing.model.ValueMetadata;
import com.bdl.config.Config;
import com.bdl.config.Configurable;

import java.util.Optional;
import java.util.Set;

import javax.inject.Qualifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * A class to hold the metadata for a Configurable object.
 *
 * @author Ben Leitner
 */
@AutoValue
abstract class ConfigMetadata implements Comparable<ConfigMetadata>, UsesTypes {

  private static final TypeMetadata CONFIG_TYPE = TypeMetadata.from(Config.class);
  private static final TypeMetadata CONFIGURABLE_TYPE = TypeMetadata.from(Configurable.class);
  private static final TypeMetadata QUALIFIER_TYPE = TypeMetadata.from(Qualifier.class);
  private static final TypeMetadata BINDING_ANNOTATION_TYPE =
      TypeMetadata.from(BindingAnnotation.class);

  abstract FieldMetadata field();

  abstract AnnotationMetadata configAnnotation();

  abstract Optional<AnnotationMetadata> bindingAnnotation();

  abstract Optional<AnnotationMetadata> qualifier();

  abstract boolean hasDefault();

  TypeMetadata type() {
    return field().type().params().get(0);
  }

  @Override
  public Set<TypeMetadata> getAllTypes() {
    ImmutableSet.Builder<TypeMetadata> allTypes = ImmutableSet.builder();
    allTypes.addAll(field().type().params().get(0).getAllTypes());
    allTypes.add(field().containingClass());
    if (qualifier().isPresent()) {
      allTypes.addAll(qualifier().get().getAllTypes());
    }
    if (bindingAnnotation().isPresent()) {
      allTypes.addAll(bindingAnnotation().get().getAllTypes());
    }
    return allTypes.build();
  }

  public String fullyQualifiedPathName() {
    return fieldReference(Imports.empty());
  }

  public String fieldReference(Imports imports) {
    return String.format(
        "%s.%s", field().containingClass().rawType().toString(imports), field().name());
  }

  /**
   * Returns the name of the config. This is equal to the field name unless a specified name is
   * given.
   */
  public String name() {
    ValueMetadata value = configAnnotation().value("name");
    return value != null ? value.value() : field().name();
  }

  @Override
  public int compareTo(ConfigMetadata that) {
    return String.CASE_INSENSITIVE_ORDER.compare(
        fullyQualifiedPathName(), that.fullyQualifiedPathName());
  }

  static ConfigMetadata from(Elements elements, FieldMetadata field, FieldInitializationGrabber grabber) {
    Builder config = ConfigMetadata.builder().field(field);
    boolean foundQualifier = false;
    boolean foundBindingAnnotation = false;

    String initializer = grabber.findInitializer(field);
    config.hasDefault(!initializer.contains("noDefault"));

    for (AnnotationMetadata annotation : field.annotations()) {
      TypeMetadata annotationType = annotation.type();
      if (annotationType.equals(CONFIG_TYPE)) {
        config.configAnnotation(annotation);
        continue;
      }

      String annotationTypeName =
          String.format(
              "%s%s%s",
              annotationType.packagePrefix(),
              annotationType.nestingPrefix(),
              annotationType.name());
      System.out.printf("Annotation Type Name: %s\n", annotationTypeName);
      TypeElement annotationTypeElement = elements.getTypeElement(annotationTypeName);
      System.out.printf("Annotation Type Element: %s\n", annotationTypeElement);
      ClassMetadata annotationClazz = ClassMetadata.fromElement(annotationTypeElement);
      for (AnnotationMetadata metaAnnotation : annotationClazz.annotations()) {
        if (metaAnnotation.type().equals(QUALIFIER_TYPE)) {
          Preconditions.checkArgument(
              !foundQualifier,
              "Field %s has more than one Qualifier annotation (not including @Config).",
              config.build().fullyQualifiedPathName());
          foundQualifier = true;
          config.qualifier(annotation);
        }
        if (metaAnnotation.type().equals(BINDING_ANNOTATION_TYPE)) {
          Preconditions.checkArgument(
              !foundBindingAnnotation,
              "Field %s has more than one BindingAnnotation annotation (not including @Config).",
              config.build().fullyQualifiedPathName());
          foundBindingAnnotation = true;
          config.bindingAnnotation(annotation);
        }
      }
    }
    return config.build();
  }

  static Builder builder() {
    return new AutoValue_ConfigMetadata.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {
    abstract Builder field(FieldMetadata field);

    abstract Builder configAnnotation(AnnotationMetadata annotation);

    abstract Builder bindingAnnotation(AnnotationMetadata annotation);

    abstract Builder qualifier(AnnotationMetadata annotation);

    abstract Builder hasDefault(boolean hasDefault);

    abstract ConfigMetadata autoBuild();

    ConfigMetadata build() {
      ConfigMetadata config = autoBuild();
      Preconditions.checkArgument(
          config.field().type().rawType().equals(CONFIGURABLE_TYPE.rawType()),
          "The given field is not a Configurable type, was %s",
          config.field().type().toString(Imports.empty()));
      return config;
    }
  }
}
