package com.bdl.config.annotation.processor;

import com.bdl.auto.impl.AutoImpl;
import com.bdl.auto.impl.ImplOption;

/**
 * TODO: JavaDoc this class.
 *
 * @author Ben Leitner
 */
@AutoImpl(ImplOption.RETURN_DEFAULT_VALUE)
interface Messager extends javax.annotation.processing.Messager {
}
