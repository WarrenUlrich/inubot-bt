package io.warren.shared.ai.bt;

public class Repeater extends Decorator {
  private final int maxRepeats;
  private int currentRepeats = 0;

  public Repeater(Node child, int maxRepeats) {
    super(child);
    this.maxRepeats = maxRepeats;
  }

  public Repeater(Node child) {
    this(child, -1);
  }

  @Override
  public Status tick() {
    if (child == null) {
      return Status.FAILURE;
    }

    // Infinite repeat
    if (maxRepeats < 0) {
      child.tick();
      return Status.RUNNING;
    }

    // Limited repeats
    Status status = child.tick();

    if (status == Status.RUNNING) {
      return Status.RUNNING;
    }

    if (status == Status.FAILURE) {
      reset();
      return Status.FAILURE;
    }

    // Child succeeded
    currentRepeats++;

    if (currentRepeats >= maxRepeats) {
      reset();
      return Status.SUCCESS;
    }

    // Reset child for next iteration
    child.reset();
    return Status.RUNNING;
  }

  @Override
  public void reset() {
    super.reset();
    currentRepeats = 0;
  }
}
