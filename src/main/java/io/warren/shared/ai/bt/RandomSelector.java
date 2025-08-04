package io.warren.shared.ai.bt;

import java.util.Random;

public class RandomSelector extends Composite {
  private final Random random = new Random();
  private Node selectedChild = null;

  @Override
  public Status tick() {
    if (!hasChildren()) {
      return Status.FAILURE;
    }

    if (selectedChild == null) {
      int index = random.nextInt(children.size());
      selectedChild = children.get(index);
    }

    Status status = selectedChild.tick();

    if (status == Status.SUCCESS || status == Status.FAILURE) {
      reset();
    }

    return status;
  }

  @Override
  public void reset() {
    super.reset();
    selectedChild = null;
  }
}
