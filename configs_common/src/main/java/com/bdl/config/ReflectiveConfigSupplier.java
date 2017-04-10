package com.bdl.config;

import java.lang.reflect.Field;

/**
 * Implementation of {@link ConfigSupplier} that uses reflection to access the Configurable's
 * fields.
 *
 * @author Ben Leitner
 */
class ReflectiveConfigSupplier extends ConfigSupplier {

  private final ConfigDescription description;
  private Configurable<?> config;

  ReflectiveConfigSupplier(ConfigDescription description) {
    this.description = description;
  }

  /**
   * Loads a class with the specified type name. This correctly handles type names representing
   * inner classes.
   *
   * @param name The type name to load. Must not be null.
   * @return The loaded class
   * @throws ClassNotFoundException if no class is found.
   */
  private Class<?> loadClass(String name) throws ClassNotFoundException {
    while (true) {
      try {
        return Class.forName(name);
      } catch (ClassNotFoundException ex) {
        int idx = name.lastIndexOf('.');
        if (idx < 0) {
          throw ex;
        }
        // Maybe it's an inner class? Replace last '.' with '$' to generate an inner class name.
        name = String.format("%s$%s", name.substring(0, idx), name.substring(idx + 1));
      }
    }
  }

  @Override
  ConfigDescription getDescription() {
    return description;
  }

  @Override
  Configurable<?> getConfigurable() {
    if (config == null) {
      try {
        Class<?> clazz = loadClass(description.fullyQualifiedClassName());
        Field field = clazz.getDeclaredField(description.fieldName());
        field.setAccessible(true);
        config = (Configurable<?>) field.get(null);
      } catch (ClassNotFoundException ex) {
        throw new LinkageError(
            String.format(
                "Class for config field %s.%s is present in manifest but not found at runtime.",
                description.className(), description.fieldName()));
      } catch (NoSuchFieldException ex) {
        throw new LinkageError(
            String.format(
                "Configurable field %s.%s is present in manifest but not found at runtime.",
                description.className(), description.fieldName()));
      } catch (IllegalAccessException ex) {
        throw new LinkageError(
            String.format(
                "Unable to get Configurable field %s.%s",
                description.className(), description.fieldName()));
      } catch (NullPointerException ex) {
        throw new LinkageError(
            String.format(
                "Cannot get config %s.%s, did you forget to make it static?",
                description.className(), description.fieldName()));
      } catch (ClassCastException ex) {
        throw new LinkageError(
            String.format(
                "Cannot convert field %s.%s to a Configurable in order to resolve.",
                description.className(), description.fieldName()));
      }
    }
    return config;
  }
}
