package io.warren.shared.ai.bt;

import java.util.function.BooleanSupplier;

public class Condition implements Node {
  private final BooleanSupplier condition;
  private final String name;

  public Condition(String name, BooleanSupplier condition) {
    this.name = name;
    this.condition = condition;
  }

  @Override
  public Status tick() {
    try {
      return condition.getAsBoolean() ? Status.SUCCESS : Status.FAILURE;
    } catch (Exception e) {
      System.err.println("Condition '" + name + "' failed: " + e.getMessage());
      return Status.FAILURE;
    }
  }

  @Override
  public String toString() {
    return "Condition[" + name + "]";
  }
}
