package io.warren.shared.script;

import java.util.List;

import org.rspeer.commons.ArrayUtils;
import org.rspeer.commons.logging.Log;
import org.rspeer.event.Subscribe;
import org.rspeer.game.Game;
import org.rspeer.game.event.AnimationEvent;
import org.rspeer.game.script.Task;
import org.rspeer.game.script.TaskDescriptor;
import org.rspeer.game.script.TaskScript;
import org.rspeer.game.script.event.ScriptConfigEvent;
import org.rspeer.game.script.tools.RestockTask;

import io.warren.shared.ai.bt.BehaviorTree;

public abstract class BehaviorScript extends TaskScript {
  private boolean started = false;

  private BehaviorTree behaviorTree;

  private static BehaviorScript currentInstance;

  public abstract BehaviorTree.Builder builder();

  @TaskDescriptor(name = "LoopTask")
  public static class LoopTask extends Task {
    private BehaviorScript script;

    public LoopTask() {
      this.script = currentInstance;
    }

    @Override
    public boolean execute() {
      if (script == null) {
        Log.debug("Script reference not set in LoopTask");
        return true;
      }

      if (!script.started)
        return true;

      if (script.getState() == State.PAUSED)
        return true;

      if (Game.getState() != Game.STATE_IN_GAME)
        return true;

      script.behaviorTree.run();
      return true;
    }
  }

  @Override
  public void initialize() {
    currentInstance = this;
    behaviorTree = builder().build();
  }

  @Override
  public final Class<? extends Task>[] tasks() {
    return ArrayUtils.getTypeSafeArray(
        LoopTask.class,
        RestockTask.class);
  }

  @Subscribe
  public void subscribe(ScriptConfigEvent event) {
    if (!started) {
      started = true;
    }
  }
}