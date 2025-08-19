package io.warren.shared.ai.bt;

import java.util.function.Supplier;

/** Re-evaluates which subtree to run on every tick. */
public class DynamicSubTree implements Node {
  private final String name;
  private final Supplier<Node> nodeSupplier;
  private Node current;

  public DynamicSubTree(String name, Supplier<BehaviorTree> treeSupplier) {
    this.name = name;
    this.nodeSupplier = () -> {
      BehaviorTree t = treeSupplier.get();
      return (t == null) ? null : t.getRoot();
    };
  }

  @Override
  public Status tick() {
    Node next = nodeSupplier.get();
    if (next == null) return Status.FAILURE;

    if (current != next) {
      if (current != null) {
        current.onEnd();
        current.reset();
      }
      current = next;
      current.onStart();
    }
    return current.tick();
  }

  @Override
  public void reset() {
    if (current != null) current.reset();
  }

  @Override
  public void onStart() {
    if (current != null) current.onStart();
  }

  @Override
  public void onEnd() {
    if (current != null) current.onEnd();
  }

  @Override
  public String toString() {
    return "DynamicSubTree[" + name + "]";
  }
}
