package io.warren.shared.ai.bt;

import java.util.ArrayList;
import java.util.List;

public abstract class Composite implements Node {
  protected final List<Node> children = new ArrayList<>();
  protected int currentChild = 0;

  public Composite addChild(Node child) {
    children.add(child);
    return this;
  }

  public Composite addChildren(Node... nodes) {
    for (Node node : nodes) {
      children.add(node);
    }
    return this;
  }

  @Override
  public void reset() {
    currentChild = 0;
    for (Node child : children) {
      child.reset();
    }
  }

  protected boolean hasChildren() {
    return !children.isEmpty();
  }
}
