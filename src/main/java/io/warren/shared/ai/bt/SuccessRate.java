package io.warren.shared.ai.bt;

import java.util.Random;
import java.util.function.Supplier;

public class SuccessRate extends Decorator {
  private final Supplier<Float> successRateSupplier;
  private final Random random;

  // Constructor with static success rate (0-100)
  public SuccessRate(Node child, float successRate) {
    this(child, () -> successRate);
  }

  // Constructor with supplier for dynamic success rate (0-100)
  public SuccessRate(Node child, Supplier<Float> successRateSupplier) {
    super(child);
    this.successRateSupplier = successRateSupplier;
    this.random = new Random();
  }

  @Override
  public Status tick() {
    if (child == null) {
      return Status.FAILURE;
    }

    // Get current success rate and clamp between 0-100
    float currentSuccessRate = Math.max(0f, Math.min(100f, successRateSupplier.get()));

    // Convert to 0-1 range for random comparison
    float normalizedRate = currentSuccessRate / 100f;

    // Roll the dice
    if (random.nextFloat() > normalizedRate) {
      return Status.FAILURE;
    }

    // Execute child if we passed the success check
    return child.tick();
  }
}