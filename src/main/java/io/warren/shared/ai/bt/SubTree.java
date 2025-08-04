package io.warren.shared.ai.bt;

import java.util.function.Supplier;

public class SubTree implements Node {
  private final String name;
  private final Supplier<Node> treeSupplier;
  private Node tree;

  public SubTree(String name, Supplier<Node> treeSupplier) {
    this.name = name;
    this.treeSupplier = treeSupplier;
  }

  @Override
  public Status tick() {
    if (tree == null) {
      tree = treeSupplier.get();
      tree.onStart();
    }

    return tree.tick();
  }

  @Override
  public void reset() {
    if (tree != null) {
      tree.reset();
    }
  }

  @Override
  public void onStart() {
    if (tree != null) {
      tree.onStart();
    }
  }

  @Override
  public void onEnd() {
    if (tree != null) {
      tree.onEnd();
    }
  }

  @Override
  public String toString() {
    return "SubTree[" + name + "]";
  }
}