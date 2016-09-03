package com.bdl.config.annotation.processor;

import com.google.auto.value.AutoValue;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.BindingAnnotation;

import com.bdl.config.Config;

import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Qualifier;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * A class to hold the metadata for a Configurable object.
 *
 * @author Ben Leitner
 */
@AutoValue
public abstract class ConfigMetadata implements Comparable<ConfigMetadata> {

  public enum Visibility {
    PRIVATE,
    PACKAGE, // Package local or protected
    PUBLIC;
  }
  public abstract String packageName();
  public abstract String className();
  public abstract String fieldName();
  public abstract Visibility visibility();
  public abstract String type();
  public abstract Optional<String> specifiedName();
  public abstract Optional<String> description();
  public abstract Optional<String> qualifier();
  public abstract Optional<String> bindingAnnotation();

  public String fullyQualifiedPathName() {
    return String.format("%s.%s.%s", packageName(), className(), fieldName());
  }

  /** Returns the name of the config.  This is equal to the field name unless a specified name is given. */
  public String name() {
    return MoreObjects.firstNonNull(specifiedName().orNull(), fieldName());
  }

  @Override
  public int compareTo(ConfigMetadata that) {
    return String.CASE_INSENSITIVE_ORDER.compare(fullyQualifiedPathName(), that.fullyQualifiedPathName());
  }

  public static ConfigMetadata fromField(Element field) {
    Preconditions.checkState(field.getKind() == ElementKind.FIELD,
        "Element %s is not a field.", field);
    ConfigMetadata.Builder metadata = ConfigMetadata.builder();
    Element enclosingClass = field.getEnclosingElement();
    String qualifiedName = ((TypeElement) enclosingClass).getQualifiedName().toString();
    int lastDot = qualifiedName.lastIndexOf('.');
    metadata.packageName(qualifiedName.substring(0, lastDot));
    metadata.className(qualifiedName.substring(lastDot + 1));
    metadata.fieldName(field.getSimpleName().toString());

    TypeMirror type = field.asType();
    String configType = ((DeclaredType) type).getTypeArguments().get(0).toString();
    metadata.type(configType);

    metadata.visibility(getVisibility(field, enclosingClass));

    Config annotation = field.getAnnotation(Config.class);
    if (!annotation.desc().isEmpty()) {
      metadata.description(annotation.desc());
    }
    if (!annotation.name().isEmpty()) {
      metadata.specifiedName(annotation.name());
    }
    String qualifier = getQualifierOrNull(field);
    if (qualifier != null) {
      metadata.qualifier(qualifier);
    }
    String bindingAnnotation = getBindingAnnotationOrNull(field);
    if (bindingAnnotation != null) {
      metadata.bindingAnnotation(bindingAnnotation);
    }
    return metadata.build();
  }

  private static Visibility getVisibility(Element field, Element enclosingClass) {
    Set<Modifier> fieldModifiers = field.getModifiers();
    Set<Modifier> classModifiers = field.getModifiers();

    if (fieldModifiers.contains(Modifier.PRIVATE)) {
      return Visibility.PRIVATE;
    }
    if (fieldModifiers.contains(Modifier.PUBLIC)
        && classModifiers.contains(Modifier.PUBLIC)) {
      return Visibility.PUBLIC;
    }
    return Visibility.PACKAGE;
  }

  @Nullable
  private static String getQualifierOrNull(Element field) {
    for (AnnotationMirror annotation : field.getAnnotationMirrors()) {
      if (annotation.getAnnotationType().asElement().getAnnotation(Qualifier.class) != null) {
        return annotation.toString();
      }
    }
    return null;
  }

  @Nullable
  private static String getBindingAnnotationOrNull(Element field) {
    for (AnnotationMirror annotation : field.getAnnotationMirrors()) {
      if (annotation.getAnnotationType().asElement().getAnnotation(BindingAnnotation.class) != null) {
        return annotation.toString();
      }
    }
    return null;
  }

  public static Builder builder() {
    return new AutoValue_ConfigMetadata.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder packageName(String packageName);
    public abstract Builder className(String className);
    public abstract Builder fieldName(String fieldName);
    public abstract Builder visibility(Visibility visibility);
    public abstract Builder type(String type);
    public abstract Builder specifiedName(String specifiedName);
    public abstract Builder description(String description);
    public abstract Builder qualifier(String qualifier);
    public abstract Builder bindingAnnotation(String bindingAnnotation);
    public abstract ConfigMetadata build();
  }
}
