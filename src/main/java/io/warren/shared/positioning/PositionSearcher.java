package io.warren.shared.positioning;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.rspeer.commons.logging.Log;
import org.rspeer.game.Game;
import org.rspeer.game.movement.pathfinding.Collisions;
import org.rspeer.game.position.Position;
import org.rspeer.game.position.Region;
import org.rspeer.game.scene.Players;
import org.rspeer.game.scene.Scene;

public class PositionSearcher {
  private Map<Position, Double> positionScores = new HashMap<>();
  private List<PositionScorer> scorers;
  private static final Random random = new Random();

  private PositionSearcher(Builder builder) {
    scorers = builder.scorers;
  }

  public double getScore(Position pos) {
    return positionScores.getOrDefault(pos, 0.0);
  }

  public Map<Position, Double> getAllScores() {
    return new HashMap<>(positionScores);
  }

  public void forEach(BiConsumer<Position, Double> action) {
    positionScores.forEach(action);
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the position with the highest score.
   * 
   * @return Optional containing the best position, or empty if no positions are
   *         scored
   */
  public Optional<Position> getBestPosition() {
    return positionScores.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey);
  }

  /**
   * Gets the position with the lowest score.
   * 
   * @return Optional containing the worst position, or empty if no positions are
   *         scored
   */
  public Optional<Position> getWorstPosition() {
    return positionScores.entrySet().stream()
        .min(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey);
  }

  /**
   * Gets the top N positions by score.
   * 
   * @param n Number of positions to return
   * @return List of positions sorted by score (highest first)
   */
  public List<Position> getTopPositions(int n) {
    return positionScores.entrySet().stream()
        .sorted(Map.Entry.<Position, Double>comparingByValue().reversed())
        .limit(n)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  /**
   * Gets all positions with a score above the threshold.
   * 
   * @param threshold Minimum score threshold
   * @return List of positions with scores above threshold
   */
  public List<Position> getPositionsAboveScore(double threshold) {
    return positionScores.entrySet().stream()
        .filter(e -> e.getValue() > threshold)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  /**
   * Gets all positions with a score below the threshold.
   * 
   * @param threshold Maximum score threshold
   * @return List of positions with scores below threshold
   */
  public List<Position> getPositionsBelowScore(double threshold) {
    return positionScores.entrySet().stream()
        .filter(e -> e.getValue() < threshold)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  /**
   * Gets the best position that matches the given predicate.
   * 
   * @param predicate Condition that the position must satisfy
   * @return Optional containing the best matching position, or empty if none
   *         match
   */
  public Optional<Position> getBestPositionMatching(Predicate<Position> predicate) {
    return positionScores.entrySet().stream()
        .filter(e -> predicate.test(e.getKey()))
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey);
  }

  /**
   * Gets the best reachable position.
   * 
   * @return Optional containing the best reachable position, or empty if none are
   *         reachable
   */
  public Optional<Position> getBestReachablePosition() {
    return getBestPositionMatching(Collisions::canReach);
  }

  /**
   * Gets positions sorted by score.
   * 
   * @param ascending If true, sorts from lowest to highest score
   * @return List of all positions sorted by score
   */
  public List<Position> getPositionsSortedByScore(boolean ascending) {
    Comparator<Map.Entry<Position, Double>> comparator = Map.Entry.comparingByValue();
    if (!ascending) {
      comparator = comparator.reversed();
    }
    return positionScores.entrySet().stream()
        .sorted(comparator)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  /**
   * Gets positions within a score range.
   * 
   * @param minScore Minimum score (inclusive)
   * @param maxScore Maximum score (inclusive)
   * @return List of positions within the score range
   */
  public List<Position> getPositionsInScoreRange(double minScore, double maxScore) {
    return positionScores.entrySet().stream()
        .filter(e -> e.getValue() >= minScore && e.getValue() <= maxScore)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  /**
   * Checks if a position has been scored.
   * 
   * @param position Position to check
   * @return true if the position has a score
   */
  public boolean hasScore(Position position) {
    return positionScores.containsKey(position);
  }

  /**
   * Gets the number of scored positions.
   * 
   * @return Count of positions with scores
   */
  public int size() {
    return positionScores.size();
  }

  /**
   * Clears all position scores.
   */
  public void clear() {
    positionScores.clear();
  }

  /**
   * Gets the average score of all positions.
   * 
   * @return Average score, or 0 if no positions are scored
   */
  public double getAverageScore() {
    if (positionScores.isEmpty()) {
      return 0.0;
    }
    return positionScores.values().stream()
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0.0);
  }

  /**
   * Gets the closest position to a target with a score above threshold.
   * 
   * @param target   Target position
   * @param minScore Minimum score threshold
   * @return Optional containing the closest qualifying position
   */
  public Optional<Position> getClosestPositionAboveScore(Position target, double minScore) {
    return positionScores.entrySet().stream()
        .filter(e -> e.getValue() > minScore)
        .min(Comparator.comparingDouble(e -> e.getKey().distance(target)))
        .map(Map.Entry::getKey);
  }

  public void update() {
    var playerPosition = Players.self().getPosition();
    var positions = new ArrayList<Position>();

    for (int x = -14; x < 14; x++) {
      for (int y = -14; y < 14; y++) {
        positions.add(playerPosition.translate(x, y));
      }
    }

    for (var position : positions) {
      double score = 0.0;
      for (var s : scorers)
        score += s.score(position);
      positionScores.put(position, score);
    }
  }

  public static class Builder {
    private List<PositionScorer> scorers = new ArrayList<>();

    public Builder withScorer(PositionScorer scorer) {
      scorers.add(scorer);
      return this;
    }

    public Builder reachableScorer(double positive, double negative) {
      return withScorer(p -> {
        return Collisions.canReach(p) ? positive : negative;
      });
    }

    public Builder randomScorer(double min, double max) {
      return withScorer(p -> {
        return min + (max - min) * random.nextDouble();
      });
    }

    public Builder randomScorer(double range) {
      return randomScorer(-range, range);
    }

    /**
     * Adds a scorer based on distance from a target position.
     * Closer positions get higher scores.
     * 
     * @param target    Target position
     * @param maxScore  Score for positions at distance 0
     * @param decayRate How quickly score decreases with distance
     */
    public Builder distanceScorer(Position target, double maxScore, double decayRate) {
      return withScorer(p -> {
        double distance = p.distance(target);
        return maxScore * Math.exp(-decayRate * distance);
      });
    }

    /**
     * Adds a scorer that gives bonus points for positions near walls.
     * 
     * @param wallBonus Score bonus for each adjacent wall
     */
    public Builder wallProximityScorer(double wallBonus) {
      return withScorer(p -> {
        int wallCount = 0;
        for (int dx = -1; dx <= 1; dx++) {
          for (int dy = -1; dy <= 1; dy++) {
            if (dx == 0 && dy == 0)
              continue;
            if (!Collisions.canReach(p.translate(dx, dy))) {
              wallCount++;
            }
          }
        }
        return wallCount * wallBonus;
      });
    }

    public PositionSearcher build() {
      return new PositionSearcher(this);
    }
  }
}