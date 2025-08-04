package io.warren.shared.positioning;

import org.rspeer.game.position.Position;

@FunctionalInterface
public interface PositionScorer {
  public abstract double score(Position position);
}
