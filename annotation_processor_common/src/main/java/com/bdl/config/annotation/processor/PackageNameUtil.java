package com.bdl.config.annotation.processor;

import javax.annotation.Nullable;

/**
 * Utility class for working with package names.
 *
 * @author Ben Leitner
 */
class PackageNameUtil {

  private PackageNameUtil() {
    // Utility class, no instantiation.
  }

  public static String append(@Nullable String packageName, String nextPart) {
    if (packageName == null || packageName.isEmpty()) {
      return nextPart;
    }
    return packageName + "." + nextPart;
  }
}
