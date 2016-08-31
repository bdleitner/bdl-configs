package com.bdl.config;

/**
 * Abstract base class for an intermediate wrapper for plumbing access to Configurables.
 *
 * @author Ben Leitner
 */
public abstract class ConfigAccess {

  /** Sets the configurable value from a string. */
  abstract void setFromString(String value);
}
