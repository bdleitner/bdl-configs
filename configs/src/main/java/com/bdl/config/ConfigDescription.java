
package com.bdl.config;

import com.google.auto.value.AutoValue;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;


/**
 * Data class for Configurable metadata.
 *
 * @author Benjaming Leitner
 */
@AutoValue
abstract class ConfigDescription {

  public abstract String packageName();
  public abstract String className();
  public abstract String fieldName();
  public abstract String type();
  public abstract Optional<String> specifiedName();
  public abstract Optional<String> description();

  /** Returns the name of the config.  This is equal to the field name unless a specified name is given. */
  public String name() {
    return MoreObjects.firstNonNull(specifiedName().orNull(), fieldName());
  }

  public String fullyQualifiedName() {
    return String.format("%s.%s.%s", packageName(), className(), fieldName());
  }

  public static Builder builder() {
    return new AutoValue_ConfigDescription.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder packageName(String packageName);
    public abstract Builder className(String className);
    public abstract Builder fieldName(String fieldName);
    public abstract Builder type(String type);
    public abstract Builder specifiedName(String specifiedName);
    public abstract Builder description(String description);
    public abstract ConfigDescription build();
  }
}
