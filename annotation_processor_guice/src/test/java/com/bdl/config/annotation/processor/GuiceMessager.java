package com.bdl.config.annotation.processor;

import com.bdl.auto.impl.AutoImpl;
import com.bdl.auto.impl.ImplOption;

/**
 * Dummy Messager implementation that does nothing, for use in tests.
 *
 * @author Ben Leitner
 */
@AutoImpl(ImplOption.RETURN_DEFAULT_VALUE)
interface GuiceMessager extends javax.annotation.processing.Messager {}
