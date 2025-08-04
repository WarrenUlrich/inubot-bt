package io.warren.shared.ai.bt;

import java.util.function.BooleanSupplier;
import org.rspeer.game.Game;

public class SleepWhile implements Node {
  private final BooleanSupplier condition;
  private final long maxTicks;
  private long startTick = -1;

  public SleepWhile(BooleanSupplier condition, long maxTicks) {
    this.condition = condition;
    this.maxTicks = maxTicks;
  }

  @Override
  public Status tick() {
    if (startTick < 0) {
      startTick = Game.getTick();
    }

    if (!condition.getAsBoolean()) {
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
}
