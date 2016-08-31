package com.bdl.config;

/**
 * A listener for configuration changes
 *
 * @param <T> the type of Configurable to be listened to
 * @author Benjamin Leitner
 */
public interface ConfigChangeListener<T> {

  /**
   * Called before the configuration change goes into effect.
   *
   * @return {@code true} if the configuration change is allowed to proceed, {@code false} otherwise.
   */
  boolean beforeConfigurationChange(T oldValue);

  /** Notifies the listener that a change has occurred */
  void onConfigurationChange(T newValue);

  /** Notifies the listener that a change that was going to happen was canceled. */
  void onChangeCancelled(T oldValue, T cancelledValue);

  /** Adapter class with no-op implementations of the listener methods, allowing for overriding only needed methods */
  class Adapter<T> implements ConfigChangeListener<T> {

    @Override
    public boolean beforeConfigurationChange(T oldValue) {
      return true;
    }

    @Override
    public void onConfigurationChange(T newValue) {
      // No-op
    }

    @Override
    public void onChangeCancelled(T oldValue, T cancelledValue) {
      // No-op
    }
  }

  /** Interface for a handle to a Listener that can remove the listener from the Configurable. */
  interface ListenerRegistration {
    void unregister();
  }
}
