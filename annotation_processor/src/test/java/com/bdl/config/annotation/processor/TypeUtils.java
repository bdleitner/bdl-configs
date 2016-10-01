package com.bdl.config.annotation.processor;

import com.bdl.annotation.processing.model.TypeMetadata;

/**
 * TODO: JavaDoc this class.
 *
 * @author Ben Leitner
 */
class TypeUtils {

  static TypeMetadata configOf(TypeMetadata type) {
    return TypeMetadata.builder()
        .setPackageName("com.bdl.config")
        .setName("Configurable")
        .addParam(type)
        .build();
  }
}
