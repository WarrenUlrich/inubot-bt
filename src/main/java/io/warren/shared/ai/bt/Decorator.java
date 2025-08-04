package io.warren.shared.ai.bt;

public abstract class Decorator implements Node {
  protected Node child;

  public Decorator(Node child) {
    this.child = child;
  }

  public Decorator() {
    this(null);
  }

  public Decorator setChild(Node child) {
    this.child = child;
    return this;
  }

  @Override
  public void reset() {
    if (child != null) {
      child.reset();
    }
  }
}