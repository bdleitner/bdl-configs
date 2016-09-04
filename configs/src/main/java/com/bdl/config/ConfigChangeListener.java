package com.bdl.config;

/**
 * A listener for configuration changes
 *
 * @param <T> the type of Configurable to be listened to
 * @author Benjamin Leitner
 */
public interface ConfigChangeListener<T> {

  /** Notifies the listener that a change has occurred */
  void onConfigurationChange(T newValue);

  /** Interface for a handle to a Listener that can remove the listener from the Configurable. */
  interface ListenerRegistration {
    void unregister();
  }
}
