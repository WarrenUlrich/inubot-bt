package io.warren.shared.ai.bt;

import org.rspeer.game.Game;
import java.util.function.Supplier;

public class Cooldown extends Decorator {
  private final Supplier<Integer> cooldownDurationSupplier;
  private int lastExecutionTick = -1;

  // Constructor with static cooldown duration
  public Cooldown(Node child, int cooldownDuration) {
    this(child, () -> cooldownDuration);
  }

  // Constructor with supplier for dynamic cooldown duration
  public Cooldown(Node child, Supplier<Integer> cooldownDurationSupplier) {
    super(child);
    this.cooldownDurationSupplier = cooldownDurationSupplier;
  }

  @Override
  public Status tick() {
    if (child == null) {
      return Status.FAILURE;
    }

    int currentTick = Game.getTick();
    int cooldownDuration = Math.max(0, cooldownDurationSupplier.get());

    // Check if we're still in cooldown (including first execution)
    if (lastExecutionTick >= 0) {
      int elapsed = currentTick - lastExecutionTick;
      if (elapsed < cooldownDuration) {
        return Status.FAILURE; // or Status.RUNNING if you prefer
      }
    }

    // Execute child
    Status status = child.tick();

    // Update last execution time immediately when we start execution
    if (lastExecutionTick < 0 || status != Status.RUNNING) {
      lastExecutionTick = currentTick;
    }

    return status;
  }

  @Override
  public void reset() {
    super.reset();
    lastExecutionTick = -1;
  }
}