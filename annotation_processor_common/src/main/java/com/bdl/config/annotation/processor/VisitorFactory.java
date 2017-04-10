package com.bdl.config.annotation.processor;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;

/**
 * Interface for classes that create a visitor.
 *
 * @author Ben Leitner
 */
interface VisitorFactory {

  ConfigPackageTree.Visitor<String> get(ProcessingEnvironment env, Messager messager);
}
