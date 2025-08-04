package io.warren.shared.ai.bt;

public class Inverter extends Decorator {
  public Inverter(Node child) {
    super(child);
  }

  public Inverter() {
    super();
  }

  @Override
  public Status tick() {
    if (child == null) {
      return Status.FAILURE;
    }

    Status status = child.tick();

    switch (status) {
      case SUCCESS:
        return Status.FAILURE;
      case FAILURE:
        return Status.SUCCESS;
      default:
        return Status.RUNNING;
    }
  }
}