package com.bdl.config.annotation.processor;

import com.bdl.config.Config;
import com.google.auto.value.AutoValue;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Set;

/**
 * A class to hold the metadata for a Configurable object.
 *
 * @author Ben Leitner
 */
@AutoValue
public abstract class ConfigMetadata implements Comparable<ConfigMetadata> {
  public abstract String packageName();
  public abstract String className();
  public abstract String fieldName();
  public abstract Set<Modifier> fieldModifiers();
  public abstract String type();
  public abstract Optional<String> specifiedName();
  public abstract Optional<String> description();

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

  public static ConfigMetadata fromElement(Element element) {
    ConfigMetadata.Builder metadata = ConfigMetadata.builder();
    Element enclosingClass = element.getEnclosingElement();
    String qualifiedName = ((TypeElement) enclosingClass).getQualifiedName().toString();
    int lastDot = qualifiedName.lastIndexOf('.');
    metadata.packageName(qualifiedName.substring(0, lastDot));
    metadata.className(qualifiedName.substring(lastDot + 1));
    metadata.fieldName(element.getSimpleName().toString());

    TypeMirror type = element.asType();
    String configType = ((DeclaredType) type).getTypeArguments().get(0).toString();
    metadata.type(configType);

    metadata.fieldModifiers(element.getModifiers());

    Config annotation = element.getAnnotation(Config.class);
    if (!annotation.desc().isEmpty()) {
      metadata.description(annotation.desc());
    }
    if (!annotation.name().isEmpty()) {
      metadata.specifiedName(annotation.name());
    }
    return metadata.build();
  }

  public static Builder builder() {
    return new AutoValue_ConfigMetadata.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder packageName(String packageName);
    public abstract Builder className(String className);
    public abstract Builder fieldName(String fieldName);
    public abstract Builder fieldModifiers(Set<Modifier> modifiers);
    public abstract Builder type(String type);
    public abstract Builder specifiedName(String specifiedName);
    public abstract Builder description(String description);
    public abstract ConfigMetadata build();
  }
}
