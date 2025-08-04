package io.warren.shared.ai.bt;

public class Sequence extends Composite {
  @Override
  public Status tick() {
    if (!hasChildren()) {
      return Status.SUCCESS;
    }

    while (currentChild < children.size()) {
      Node child = children.get(currentChild);
      Status status = child.tick();
      if (status == Status.SLEEPING)
        return Status.SLEEPING;

      if (status != Status.SUCCESS) {
        // If running or failed, return that status
        if (status == Status.FAILURE) {
          reset();
        }
        return status;
      }

      // Move to next child
      currentChild++;
    }

    // All children succeeded
    reset();
    return Status.SUCCESS;
  }
}