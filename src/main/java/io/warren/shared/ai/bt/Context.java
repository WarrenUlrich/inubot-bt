package io.warren.shared.ai.bt;

import java.util.HashMap;
import java.util.Map;

public class Context {
  private final Map<String, Object> data = new HashMap<>();

  public void set(String key, Object value) {
    data.put(key, value);
  }

  @SuppressWarnings("unchecked")
  public <T> T get(String key, Class<T> type) {
    Object value = data.get(key);
    if (value != null && type.isInstance(value)) {
      return (T) value;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public <T> T getOrDefault(String key, T defaultValue) {
    Object value = data.get(key);
    if (value != null && defaultValue.getClass().isInstance(value)) {
      return (T) value;
    }
    return defaultValue;
  }

  public boolean has(String key) {
    return data.containsKey(key);
  }

  public void remove(String key) {
    data.remove(key);
  }

  public void clear() {
    data.clear();
  }
}