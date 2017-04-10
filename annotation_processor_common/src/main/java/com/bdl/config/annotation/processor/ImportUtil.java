package com.bdl.config.annotation.processor;

import javax.annotation.Nullable;

/**
 * Utils for import spacing.
 *
 * @author Ben Leitner
 */
class ImportUtil {

  private ImportUtil() {
    // No instantiation
  }

  static boolean needsNewLine(@Nullable String priorImport, String currentImport) {
    if (priorImport == null) {
      return true;
    }
    String[] priorParts = priorImport.split("\\.");
    String[] currentParts = currentImport.split("\\.");
    int fewerParts = Math.min(priorParts.length, currentParts.length);
    int lastAgreeingIndex = lastAgreeingIndex(priorParts, currentParts);
    return lastAgreeingIndex < Math.min(2, fewerParts - 2);
  }

  private static int lastAgreeingIndex(String[] first, String[] second) {
    int fewerParts = Math.min(first.length, second.length);

    for (int i = 0; i < fewerParts; i++) {
      if (!first[i].equals(second[i])) {
        return i - 1;
      }
    }
    return fewerParts;
  }
}
