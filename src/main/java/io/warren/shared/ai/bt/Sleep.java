package io.warren.shared.ai.bt;

import org.rspeer.game.Game;

public class Sleep implements Node {
  private final long duration;
  private long startTick = -1;

  public Sleep(long duration) {
    this.duration = duration;
  }

  @Override
  public Status tick() {
    if (startTick < 0) {
      startTick = Game.getTick();
    }

    long elapsed = Game.getTick() - startTick;
    if (elapsed >= duration) {
      reset();
      return Status.SUCCESS;
    }

    return Status.SLEEPING;
  }

  @Override
  public void reset() {
    startTick = -1;
  }
}
