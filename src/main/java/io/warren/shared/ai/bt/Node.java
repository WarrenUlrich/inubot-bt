package io.warren.shared.ai.bt;

public interface Node {
  Status tick();

  default void onStart() {}
  
  default void onEnd() {}

  default void reset() {}
}