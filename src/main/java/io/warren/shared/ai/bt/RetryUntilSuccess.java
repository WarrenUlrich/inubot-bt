package io.warren.shared.ai.bt;

public class RetryUntilSuccess extends Decorator {
  private final int maxAttempts;
  private int currentAttempts = 0;

  public RetryUntilSuccess(Node child, int maxAttempts) {
    super(child);
    this.maxAttempts = maxAttempts;
  }

  public RetryUntilSuccess(Node child) {
    this(child, -1);
  }

  @Override
  public Status tick() {
    if (child == null) {
      return Status.FAILURE;
    }

    Status status = child.tick();

    if (status == Status.SUCCESS) {
      reset();
      return Status.SUCCESS;
    }

    if (status == Status.RUNNING) {
      return Status.RUNNING;
    }

    // Child failed
    currentAttempts++;

    // Check if we've exceeded max attempts
    if (maxAttempts > 0 && currentAttempts >= maxAttempts) {
      reset();
      return Status.FAILURE;
    }

    // Reset child for retry
    child.reset();
    return Status.RUNNING;
  }

  @Override
  public void reset() {
    super.reset();
    currentAttempts = 0;
  }
}
