package io.warren.shared.ai.bt;

import java.util.function.BooleanSupplier;
import org.rspeer.game.Game;

public class SleepUntil implements Node {
  private final BooleanSupplier condition;
  private final long maxTicks;
  private long startTick = -1;

  public SleepUntil(BooleanSupplier condition, long maxTicks) {
    this.condition = condition;
    this.maxTicks = maxTicks;
  }

  @Override
  public Status tick() {
    if (startTick < 0) {
      startTick = Game.getTick();
    }

    if (condition.getAsBoolean()) {
      reset();
      return Status.SUCCESS;
    }

    long elapsed = Game.getTick() - startTick;
    if (elapsed >= maxTicks) {
      reset();
      return Status.FAILURE;
    }

    return Status.SLEEPING;
  }

  @Override
  public void reset() {
    startTick = -1;
  }

  /**
   * Decorator that sleeps until its child node returns SUCCESS.
   * The child node acts as the condition.
   */
  public static class Decorator extends io.warren.shared.ai.bt.Decorator {
    private final long maxTicks;
    private long startTick = -1;

    public Decorator(long maxTicks) {
      super();
      this.maxTicks = maxTicks;
    }

    public Decorator(long maxTicks, Node child) {
      super(child);
      this.maxTicks = maxTicks;
    }

    @Override
    public Status tick() {
      if (child == null) {
        throw new IllegalStateException("SleepUntil.Decorator requires a child node");
      }

      // Initialize start time
      if (startTick < 0) {
        startTick = Game.getTick();
      }

      // Execute the child node (which acts as our condition)
      Status childStatus = child.tick();

      // If child succeeded, we're done
      if (childStatus == Status.SUCCESS) {
        reset();
        return Status.SUCCESS;
      }

      // Check if we've exceeded max wait time
      long elapsed = Game.getTick() - startTick;
      if (elapsed >= maxTicks) {
        reset();
        return Status.FAILURE;
      }

      // Child didn't succeed yet, keep sleeping
      // Reset the child so it can be re-evaluated next tick
      if (childStatus == Status.FAILURE) {
        child.reset();
      }

      return Status.SLEEPING;
    }

    @Override
    public void reset() {
      startTick = -1;
      super.reset();
    }
  }
}