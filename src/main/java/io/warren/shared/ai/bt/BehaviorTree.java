package io.warren.shared.ai.bt;

import java.util.Stack;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.rspeer.commons.logging.Log;
import org.rspeer.game.ItemCategory;
import org.rspeer.game.Keyboard;
import org.rspeer.game.Wilderness;
import org.rspeer.game.adapter.component.World;
import org.rspeer.game.adapter.component.inventory.Backpack;
import org.rspeer.game.adapter.component.inventory.Bank;
import org.rspeer.game.adapter.component.inventory.Inventory;
import org.rspeer.game.adapter.scene.Npc;
import org.rspeer.game.adapter.scene.PathingEntity;
import org.rspeer.game.adapter.type.Interactable;
import org.rspeer.game.adapter.type.SceneNode;
import org.rspeer.game.combat.Combat;
import org.rspeer.game.component.Dialog;
import org.rspeer.game.component.EnterInput;
import org.rspeer.game.component.Interfaces;
import org.rspeer.game.component.Inventories;
import org.rspeer.game.component.Item;
import org.rspeer.game.component.Production;
import org.rspeer.game.component.Worlds;
import org.rspeer.game.component.tdi.Magic;
import org.rspeer.game.component.tdi.Prayer;
import org.rspeer.game.component.tdi.Prayers;
import org.rspeer.game.component.tdi.Quest;
import org.rspeer.game.component.tdi.Quests;
import org.rspeer.game.component.tdi.Skill;
import org.rspeer.game.component.tdi.Skills;
import org.rspeer.game.component.tdi.Spell;
import org.rspeer.game.component.tdi.Magic.Autocast.Mode;
import org.rspeer.game.config.item.entry.FuzzyItemEntry;
import org.rspeer.game.config.item.entry.InterchangeableItemEntry;
import org.rspeer.game.config.item.entry.ItemEntry;
import org.rspeer.game.config.item.loadout.BackpackLoadout;
import org.rspeer.game.config.item.loadout.EquipmentLoadout;
import org.rspeer.game.config.item.loadout.InventoryLoadout;
import org.rspeer.game.effect.Health;
import org.rspeer.game.movement.Movement;
import org.rspeer.game.movement.pathfinding.Collisions;
import org.rspeer.game.position.Position;
import org.rspeer.game.position.area.Area;
import org.rspeer.game.query.component.ComponentQuery;
import org.rspeer.game.query.component.ItemQuery;
import org.rspeer.game.query.component.WorldQuery;
import org.rspeer.game.query.results.ComponentQueryResults;
import org.rspeer.game.query.results.ItemQueryResults;
import org.rspeer.game.scene.Players;

import com.google.common.base.Predicate;

/**
 * A Behavior Tree implementation with a fluent builder API.
 */
public class BehaviorTree {
  private final Node root;

  private BehaviorTree(Node root) {
    this.root = root;
  }

  /**
   * Creates a new behavior tree builder.
   * 
   * @return A new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  public Node getRoot() {
    return root;
  }

  /**
   * Executes one tick of the behavior tree.
   * 
   * @return The status after this tick
   */
  public Status tick() {
    return root.tick();
  }

  /**
   * Runs the behavior tree until completion.
   * 
   * @param tickDelay Delay between ticks in milliseconds (0 for no delay)
   * @return The final status
   */
  public Status run() {
    Status status;
    do {
      status = tick();
    } while (status == Status.RUNNING);

    return status;
  }

  /**
   * Resets the behavior tree to its initial state.
   */
  public void reset() {
    root.reset();
  }

  /**
   * Builder for constructing behavior trees using a fluent API.
   */
  public static class Builder {
    private Stack<Node> nodeStack = new Stack<>();
    private Stack<Composite> compositeStack = new Stack<>();

    private Builder() {
    }

    /**
     * Starts building a sequence node.
     * All children must succeed for the sequence to succeed.
     * 
     * @return This builder for chaining
     */
    public Builder sequence() {
      Sequence sequence = new Sequence();
      if (!compositeStack.isEmpty()) {
        compositeStack.peek().addChild(sequence);
      }
      compositeStack.push(sequence);
      return this;
    }

    /**
     * Starts building a selector node.
     * The first child to succeed causes the selector to succeed.
     * 
     * @return This builder for chaining
     */
    public Builder selector() {
      Selector selector = new Selector();
      if (!compositeStack.isEmpty()) {
        compositeStack.peek().addChild(selector);
      }
      compositeStack.push(selector);
      return this;
    }

    /**
     * Starts building a random selector node.
     * Children are evaluated in random order.
     * 
     * @return This builder for chaining
     */
    public Builder randomSelector() {
      RandomSelector randomSelector = new RandomSelector();
      if (!compositeStack.isEmpty()) {
        compositeStack.peek().addChild(randomSelector);
      }
      compositeStack.push(randomSelector);
      return this;
    }

    /**
     * Starts building a parallel node.
     * 
     * @param successPolicy Policy for success
     * @param failurePolicy Policy for failure
     * @return This builder for chaining
     */
    public Builder parallel(Parallel.Policy successPolicy, Parallel.Policy failurePolicy) {
      Parallel parallel = new Parallel(successPolicy, failurePolicy);
      if (!compositeStack.isEmpty()) {
        compositeStack.peek().addChild(parallel);
      }
      compositeStack.push(parallel);
      return this;
    }

    /**
     * Starts building a parallel node with default policies.
     * Default: REQUIRE_ALL for success, REQUIRE_ONE for failure.
     * 
     * @return This builder for chaining
     */
    public Builder parallel() {
      return parallel(Parallel.Policy.REQUIRE_ALL, Parallel.Policy.REQUIRE_ONE);
    }

    /**
     * Adds an action node.
     * 
     * @param action The action function
     * @return This builder for chaining
     */
    public Builder action(Supplier<Status> action) {
      return action("Action", action);
    }

    /**
     * Adds an action node with a name.
     * 
     * @param name   Name of the action
     * @param action The action function
     * @return This builder for chaining
     */
    public Builder action(String name, Supplier<Status> action) {
      Node node = new Action(name, action);
      addNode(node);
      return this;
    }

    public Builder succeed() {
      return action("Succeed", () -> {
        return Status.SUCCESS;
      });
    }

    /**
     * Adds an action that always succeeds.
     * 
     * @param action The action to execute
     * @return This builder for chaining
     */
    public Builder succeed(Runnable action) {
      return action("Succeed", () -> {
        action.run();
        return Status.SUCCESS;
      });
    }

    /**
     * Adds an action that always fails.
     * 
     * @param action The action to execute
     * @return This builder for chaining
     */
    public Builder fail(Runnable action) {
      return action("Fail", () -> {
        action.run();
        return Status.FAILURE;
      });
    }

    /**
     * Adds a condition node.
     * 
     * @param condition The condition
     * @return This builder for chaining
     */
    public Builder condition(BooleanSupplier condition) {
      return condition("Condition", condition);
    }

    /**
     * Adds a condition node with a name.
     * 
     * @param name      Name of the condition
     * @param condition The condition
     * @return This builder for chaining
     */
    public Builder condition(String name, BooleanSupplier condition) {
      Node node = new Condition(name, condition);
      addNode(node);
      return this;
    }

    public Builder sleep(long duration) {
      Node node = new Sleep(duration);
      addNode(node);
      return this;
    }

    public Builder sleepUntil(long duration) {
      nodeStack.push(new SleepUntil.Decorator(duration));
      return this;
    }

    public Builder sleepUntil(BooleanSupplier predicate, long duration) {
      Node node = new SleepUntil(predicate, duration);
      addNode(node);
      return this;
    }

    public Builder sleepWhile(BooleanSupplier predicate, long duration) {
      Node node = new SleepWhile(predicate, duration);
      addNode(node);
      return this;
    }

    /**
     * Adds an inverter decorator to the next node.
     * Success becomes failure and vice versa.
     * 
     * @return This builder for chaining
     */
    public Builder invert() {
      nodeStack.push(new Inverter());
      return this;
    }

    /**
     * Adds a repeater decorator to the next node.
     * 
     * @param maxRepeats Maximum number of repeats (-1 for infinite)
     * @return This builder for chaining
     */
    public Builder repeat(int maxRepeats) {
      nodeStack.push(new Repeater(null, maxRepeats));
      return this;
    }

    /**
     * Adds an infinite repeater decorator to the next node.
     * 
     * @return This builder for chaining
     */
    public Builder repeatForever() {
      return repeat(-1);
    }

    /**
     * Adds a retry decorator to the next node.
     * 
     * @param maxAttempts Maximum retry attempts (-1 for infinite)
     * @return This builder for chaining
     */
    public Builder retry(int maxAttempts) {
      nodeStack.push(new RetryUntilSuccess(null, maxAttempts));
      return this;
    }

    /**
     * Adds an infinite retry decorator to the next node.
     * 
     * @return This builder for chaining
     */
    public Builder retryForever() {
      return retry(-1);
    }

    public Builder subtree(BehaviorTree tree) {
      return subtree("SubTree", tree);
    }

    public Builder subtree(String name, BehaviorTree tree) {
      Node node = new SubTree(name, () -> tree.root);
      addNode(node);
      return this;
    }

    public Builder subtree(Supplier<BehaviorTree> treeSupplier) {
      return subtree("SubTree", treeSupplier);
    }

    public Builder subtree(String name, Supplier<BehaviorTree> treeSupplier) {
      Node node = new DynamicSubTree(name, treeSupplier);
      addNode(node);
      return this;
    }

    /**
     * Adds a cooldown decorator to the next node.
     * 
     * @param cooldownDuration Cooldown duration in ticks
     * @return This builder for chaining
     */
    public Builder cooldown(int ticks) {
      nodeStack.push(new Cooldown(null, ticks));
      return this;
    }

    public Builder cooldown(Supplier<Integer> ticksSupplier) {
      nodeStack.push(new Cooldown(null, ticksSupplier.get()));
      return this;
    }

    public Builder successRate(float chance) {
      nodeStack.push(new SuccessRate(null, chance));
      return this;
    }

    public Builder successRate(Supplier<Float> chanceSupplier) {
      nodeStack.push(new SuccessRate(null, chanceSupplier.get()));
      return this;
    }

    /**
     * Ends the current composite node.
     * 
     * @return This builder for chaining
     */
    public Builder end() {
      if (!compositeStack.isEmpty()) {
        Node composite = compositeStack.pop();
        if (!compositeStack.isEmpty()) {
          // If there's still a parent composite, we already added this as a child
        } else {
          // This is the root node
          nodeStack.push(composite);
        }
      }
      return this;
    }

    /**
     * Applies a function to the builder.
     * Useful for composing reusable behavior patterns.
     * 
     * @param builderFunction Function that modifies the builder
     * @return This builder for chaining
     */
    public Builder apply(Function<Builder, Builder> builderFunction) {
      return builderFunction.apply(this);
    }

    /**
     * Adds a custom node.
     * 
     * @param node The node to add
     * @return This builder for chaining
     */
    public Builder node(Node node) {
      addNode(node);
      return this;
    }

    /**
     * Builds and returns the behavior tree.
     * 
     * @return The constructed behavior tree
     */
    public BehaviorTree build() {
      if (!compositeStack.isEmpty()) {
        throw new IllegalStateException("Unclosed composite nodes. Call end() for each composite.");
      }
      if (nodeStack.size() != 1) {
        throw new IllegalStateException("Invalid tree structure. Expected exactly one root node.");
      }
      return new BehaviorTree(nodeStack.pop());
    }

    private void addNode(Node node) {
      // Apply any pending decorators
      while (!nodeStack.isEmpty() && nodeStack.peek() instanceof Decorator) {
        Decorator decorator = (Decorator) nodeStack.pop();
        decorator.setChild(node);
        node = decorator;
      }

      if (!compositeStack.isEmpty()) {
        compositeStack.peek().addChild(node);
      } else {
        nodeStack.push(node);
      }
    }

    public Builder logInfo(String message) {
      return succeed(() -> Log.info(message));
    }

    public Builder logInfo(Supplier<String> message) {
      return succeed(() -> Log.info(message.get()));
    }

    public Builder healthPercent(int minPercent) {
      return condition(() -> {
        return Health.getPercent() >= minPercent;
      });
    }

    public Builder healthPercent(Supplier<Integer> minPercent) {
      return condition(() -> {
        return Health.getPercent() >= minPercent.get();
      });
    }

    public Builder prayerPercent(int minPercent) {
      return condition(() -> {
        return Prayers.getPercent() >= minPercent;
      });
    }

    public Builder prayerPercent(Supplier<Integer> minPercent) {
      return condition(() -> {
        return Prayers.getPercent() >= minPercent.get();
      });
    }

    public Builder energyPercent(int minPercent) {
      return condition(() -> {
        return Movement.getRunEnergy() >= minPercent;
      });
    }

    public Builder energyPercent(Supplier<Integer> minPercent) {
      return condition(() -> {
        return Movement.getRunEnergy() >= minPercent.get();
      });
    }

    public Builder isAnimating() {
      return condition(() -> Players.self().isAnimating());
    }

    public Builder isAnimating(int animationId) {
      return condition(() -> Players.self().getAnimationId() == animationId);
    }

    public Builder isMoving() {
      return condition(() -> Players.self().isMoving());
    }

    public Builder hasTarget() {
      return condition(() -> Players.self().getTarget() != null);
    }

    public Builder isTargetting(Supplier<? extends PathingEntity<?>> entityProvider) {
      return condition(() -> {
        var target = Players.self().getTarget();
        return target != null && target.equals(entityProvider.get());
      });
    }

    public Builder within(Area area) {
      return condition(() -> {
        return area.contains(Players.self());
      });
    }

    public Builder within(Supplier<Area> areaSupplier) {
      return condition(() -> {
        var area = areaSupplier.get();
        return area.contains(Players.self());
      });
    }

    public Builder within(SceneNode node, Supplier<Area> areaSupplier) {
      return condition(() -> {
        var area = areaSupplier.get();
        if (area == null)
          return false;

        return area.contains(node);
      });
    }

    public Builder within(Supplier<SceneNode> nodeSupplier, Supplier<Area> areaSupplier) {
      return condition(() -> {
        var area = areaSupplier.get();
        var node = nodeSupplier.get();
        if (area == null || node == null)
          return false;

        return area.contains(node);
      });
    }

    public Builder within(Supplier<SceneNode> nodeSupplier, Area area) {
      return condition(() -> {
        var node = nodeSupplier.get();
        if (node == null)
          return false;

        return area.contains(node);
      });
    }

    public Builder on(Supplier<Position> positionSupplier) {
      return condition(() -> {
        return Players.self().getPosition().equals(positionSupplier.get());
      });
    }

    public Builder interact(Supplier<? extends Interactable> interactableSupplier, String action) {
      return action(() -> {
        var interactable = interactableSupplier.get();
        if (interactable == null)
          return Status.FAILURE;

        return interactable.interact(action) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder canCast(Spell spell) {
      return condition(() -> {
        return Magic.canCast(spell);
      });
    }

    public Builder canCast(Supplier<Spell> spell) {
      return condition(() -> {
        return Magic.canCast(spell.get());
      });
    }

    public Builder castSpell(Supplier<Interactable> interactableSupplier, Spell spell) {
      return action(() -> {
        var interactable = interactableSupplier.get();
        if (interactable == null)
          return Status.FAILURE;

        return Magic.cast(spell, interactable) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder castSpell(Supplier<Interactable> interactableSupplier, Supplier<Spell> spellSupplier) {
      return action(() -> {
        var interactable = interactableSupplier.get();
        var spell = spellSupplier.get();
        if (interactable == null || spell == null)
          return Status.FAILURE;

        return Magic.cast(spell, interactable) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder castSpell(Spell spell, String action) {
      return action(() -> {
        return Magic.interact(spell, action) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder castSpell(Supplier<Spell> spellSupplier, String action) {
      return action(() -> {
        var spell = spellSupplier.get();
        if (spell == null)
          return Status.FAILURE;

        return Magic.interact(spell, action) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder autoCasting(Supplier<Spell> spellSupplier) {
      return condition(() -> {
        return Magic.Autocast.isSpellSelected(spellSupplier.get());
      });
    }

    public Builder autoCast(Mode mode, Supplier<Spell> spellSupplier) {
      return condition(() -> {
        return Magic.Autocast.select(mode, spellSupplier.get());
      });
    }

    public Builder prayerActive(Prayer... prayer) {
      return condition(() -> {
        return Prayers.isActive(prayer);
      });
    }

    public Builder prayerActive(Supplier<Prayer> prayerSupplier) {
      return condition(() -> {
        var prayer = prayerSupplier.get();
        if (prayer == null)
          return false;

        return Prayers.isActive(prayer);
      });
    }

    public Builder flickPrayer(Prayer... prayer) {
      return action(() -> {
        if (Prayers.getPoints() <= 0)
          return Status.FAILURE;

        Prayers.flick(prayer);
        return Status.SUCCESS;
      });
    }

    public Builder flickPrayer(Supplier<Prayer[]> prayerSupplier) {
      return action(() -> {
        var prayer = prayerSupplier.get();
        if (prayer == null || Prayers.getPoints() <= 0)
          return Status.FAILURE;

        Prayers.flick(prayer);
        return Status.SUCCESS;
      });
    }

    public Builder selectPrayer(boolean on, Prayer... prayer) {
      return action(() -> {
        return Prayers.select(on, prayer) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder selectPrayer(boolean on, Supplier<Prayer[]> prayerSupplier) {
      return action(() -> {
        var prayer = prayerSupplier.get();
        if (prayer == null)
          return Status.FAILURE;

        return Prayers.select(on, prayer) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder prayerUnlocked(Prayer prayer) {
      return condition(() -> {
        return Prayers.isUnlocked(prayer);
      });
    }

    public Builder prayerUnlocked(Supplier<Prayer> prayerSupplier) {
      return condition(() -> {
        var prayer = prayerSupplier.get();
        return prayer != null && Prayers.isUnlocked(prayer);
      });
    }

    public Builder walkTo(Position position) {
      return action(() -> {
        return Movement.walkTo(position) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder walkTo(Supplier<Position> positionSupplier) {
      return condition(() -> {
        var position = positionSupplier.get();
        if (position == null)
          return false;

        return Movement.walkTo(position);
      });
    }

    public Builder walkToArea(Area area) {
      return condition(() -> {
        return Movement.walkTo(area);
      });
    }

    public Builder walkToArea(Supplier<Area> areaSupplier) {
      return condition(() -> {
        var area = areaSupplier.get();
        if (area == null)
          return false;

        return Movement.walkTo(area);
      });
    }

    public Builder walkTowards(Position position) {
      return action(() -> {
        Movement.walkTowards(position);
        return Status.SUCCESS;
      });
    }

    public Builder walkTowards(Supplier<Position> positionSupplier) {
      return action(() -> {
        var position = positionSupplier.get();
        if (position == null)
          return Status.FAILURE;

        Movement.walkTowards(position);
        return Status.SUCCESS;
      });
    }

    public Builder canReach(SceneNode node) {
      return condition(() -> {
        return Collisions.canReach(node);
      });
    }

    public Builder canReach(Supplier<SceneNode> nodeSupplier) {
      return condition(() -> {
        var node = nodeSupplier.get();
        if (node == null)
          return false;

        return Collisions.canReach(node);
      });
    }

    public Builder isReachable(Position position) {
      return condition(() -> {
        return Collisions.isReachable(position);
      });
    }

    public Builder isReachable(Supplier<Position> positionSupplier) {
      return condition(() -> {
        var position = positionSupplier.get();
        if (position == null)
          return false;

        return Collisions.isReachable(position);
      });
    }

    public Builder toggleRun(boolean on) {
      return action(() -> {
        return Movement.toggleRun(on) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder toggleRun(BooleanSupplier onSupplier) {
      return action(() -> {
        return Movement.toggleRun(onSupplier.getAsBoolean()) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder isStaminaEnhanced() {
      return condition(() -> {
        return Movement.isStaminaEnhancementActive();
      });
    }

    public Builder eatFood() {
      return condition(() -> {
        var food = Inventories.backpack()
            .query()
            .category(ItemCategory.EDIBLE_FOOD)
            .results()
            .first();

        return food != null && food.interact("Eat");
      });
    }

    public Builder hasLoadout(Supplier<InventoryLoadout> loadoutSupplier, boolean strict) {
      return condition(() -> {
        var loadout = loadoutSupplier.get();
        if (loadout == null) {
          Log.warn("Loadout null");
          return false;
        }

        var missing = loadout.getMissingEntries(strict)
            .stream()
            .collect(Collectors.toList());

        missing.forEach(entry -> Log.warn("Missing: " +
            entry.getKey()));
        return missing.isEmpty();
      });
    }

    public Builder isLoadoutBagged(Supplier<InventoryLoadout> loadoutSupplier, boolean strict) {
      return condition(() -> {
        var loadout = loadoutSupplier.get();
        if (loadout == null)
          return false;

        return loadout.isBackpackValid(strict);
      });
    }

    public Builder isLoadoutBaggedTest(Supplier<InventoryLoadout> loadoutSupplier, boolean strict) {
      return condition(() -> {
        var loadout = loadoutSupplier.get();
        var backpack = Inventories.backpack();

        for (var entry : loadout) {
          if (!entry.contained(backpack)) {
            if (entry.isOptional()) {
              continue;
            }
            return false;
          }

          int count = entry.getContained(backpack)
              .stream()
              .mapToInt(Item::getStackSize)
              .sum();

          int required = strict ? entry.getQuantity() : entry.getMinimumQuantity();
          if (count < required) {
            return false;
          }
        }

        return true;
      });
    }

    public Builder isLoadoutEquipped(Supplier<InventoryLoadout> loadoutSupplier) {
      return condition(() -> {
        var loadout = loadoutSupplier.get();
        if (loadout == null)
          return false;

        return loadout.isEquipmentValid();
      });
    }

    public Builder loadoutEquipped(Supplier<EquipmentLoadout> loadoutSupplier) {
      return condition(() -> {
        var loadout = loadoutSupplier.get();
        if (loadout == null) {
          Log.warn("Loadout null");
          return false;
        }

        return loadout.getMissingEquipmentEntries().isEmpty();
      });
    }

    public Builder equipLoadout(Supplier<EquipmentLoadout> loadoutSupplier) {
      return action(() -> {
        var loadout = loadoutSupplier.get();
        if (loadout == null)
          return Status.FAILURE;

        loadout.equip();
        return Status.SUCCESS;
      });
    }

    public Builder inventoryContains(Supplier<Inventory> inventorySupplier,
        Function<ItemQuery, ItemQueryResults> queryFunc) {
      return condition(() -> {
        var inventory = inventorySupplier.get();
        if (inventory == null)
          return false;

        return inventory.contains(queryFunc);
      });
    }

    public Builder backpackContains(Function<ItemQuery, ItemQueryResults> queryFunc) {
      return inventoryContains(Inventories::backpack, queryFunc);
    }

    public Builder bankContains(Function<ItemQuery, ItemQueryResults> queryFunc) {
      return inventoryContains(Inventories::bank, queryFunc);
    }

    public Builder equipmentContains(Function<ItemQuery, ItemQueryResults> queryFunc) {
      return inventoryContains(Inventories::equipment, queryFunc);
    }

    public Builder useItem(Function<ItemQuery, Item> itemFunc, Supplier<Interactable> interactableSupplier) {
      return condition(() -> {
        return Inventories.backpack().use(itemFunc, interactableSupplier.get());
      });
    }

    public Builder useItem(Function<ItemQuery, Item> itemFuncA, Function<ItemQuery, Item> itemFuncB) {
      return condition(() -> {
        return Inventories.backpack().use(itemFuncA, itemFuncB);
      });
    }

    public Builder isBankOpen() {
      return condition(() -> {
        return Bank.isOpen();
      });
    }

    public Builder openBank() {
      return action(() -> {
        return Bank.open() ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder openBank(Bank.Location location) {
      return action(() -> {
        return Bank.open(location) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder openBank(Supplier<Bank.Location> locationSupplier) {
      return action(() -> {
        var location = locationSupplier.get();
        if (location == null)
          return Status.FAILURE;

        return Bank.open(location) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder deposit(int id, int amount) {
      return action(() -> {
        var bank = Inventories.bank();
        if (bank == null)
          return Status.FAILURE;

        return bank.deposit(id, amount) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder deposit(String name, int amount) {
      return action(() -> {
        var bank = Inventories.bank();
        return bank.deposit(name, amount) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder deposit(Function<ItemQuery, Item> function, int amount) {
      return action(() -> {
        var bank = Inventories.bank();
        return bank.deposit(function, amount) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder depositAll(Function<ItemQuery, ItemQueryResults> function) {
      return action(() -> {
        var bank = Inventories.bank();
        return bank.depositAll(function) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder depositAllExcept(Function<ItemQuery, ItemQueryResults> function) {
      return action(() -> {
        var bank = Inventories.bank();
        return bank.depositAllExcept(function) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder depositLoadout(Supplier<InventoryLoadout> loadoutSupplier) {
      return condition(() -> {
        var loadout = loadoutSupplier.get();
        if (loadout == null)
          return false;

        var bank = Inventories.bank();
        var keys = StreamSupport.stream(loadout.spliterator(), false).map(entry -> entry.getKey())
            .toArray(String[]::new);
        return bank.depositAll(query -> query.nameContains(keys).results());
      });
    }

    public Builder depositInventory() {
      return action(() -> {
        var bank = Inventories.bank();
        return bank.depositInventory() ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder depositEquipment() {
      return action(() -> {
        var bank = Inventories.bank();
        return bank.depositEquipment() ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder withdraw(int id, int amount) {
      return action(() -> {
        var bank = Inventories.bank();
        return bank.withdraw(id, amount) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder withdraw(String name, int amount) {
      return action(() -> {
        var bank = Inventories.bank();
        return bank.withdraw(name, amount) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder withdraw(Function<ItemQuery, Item> function, int amount) {
      return action(() -> {
        var bank = Inventories.bank();
        return bank.withdraw(function, amount) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder withdrawAll(Function<ItemQuery, Item> function) {
      return action(() -> {
        var bank = Inventories.bank();
        return bank.withdrawAll(function) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder withdrawLoadout(Supplier<InventoryLoadout> loadoutSupplier) {
      return condition(() -> {
        var loadout = loadoutSupplier.get();
        if (loadout == null) {
          Log.warn("Loadout is null");
          return false;
        }

        if (loadout instanceof BackpackLoadout) {
          return loadout.withdraw();
        }

        var bank = Inventories.bank();
        loadout.getMissingBackpackEntries()
            .stream()
            .limit(4)
            .filter(entry -> {
              return loadout.getMissingEquipmentEntries().contains(entry);
            })
            .forEach(entry -> {
              Log.info("Withdrawing: " + entry.getKey());
              bank.withdraw(query -> {
                if (entry instanceof InterchangeableItemEntry) {
                  var interchangeable = ((InterchangeableItemEntry) entry);
                  return query.names(interchangeable.getNames()).results().first();
                }

                if (entry instanceof FuzzyItemEntry) {
                  var fuzzy = ((FuzzyItemEntry) entry);
                  return fuzzy.getContained(query).first();
                }

                return query.names(entry.getKey()).results().first();
              }, entry.getQuantity());
            });

        return true;
      });
    }

    // Helper method to get equipped quantity consistently
    private int getEquippedQuantity(Inventory equipment, ItemEntry entry) {
      if (entry instanceof InterchangeableItemEntry) {
        var interchangeableEntry = (InterchangeableItemEntry) entry;
        return equipment.getCount(query -> query.names(interchangeableEntry.getNames()).results());
      } else {
        return equipment.getCount(query -> query.ids(entry.getRestockMeta().getItemId()).results());
      }
    }

    public Builder isBoosted(Skill skill, double percentage) {
      return condition(() -> {
        var realLevel = Skills.getLevel(skill); // Base level without boosts
        var currentLevel = Skills.getCurrentLevel(skill);
        double requiredBoost = realLevel * (percentage / 100.0);
        return currentLevel >= (realLevel + requiredBoost);
      });
    }

    public Builder isReduced(Skill skill) {
      return condition(() -> {
        var realLevel = Skills.getLevel(skill);
        var currentLevel = Skills.getCurrentLevel(skill);
        return currentLevel < realLevel;
      });
    }

    public Builder isReduced(Skill skill, double percentage) {
      return condition(() -> {
        var realLevel = Skills.getLevel(skill);
        var currentLevel = Skills.getCurrentLevel(skill);
        double requiredReduction = realLevel * (percentage / 100.0);
        return currentLevel <= (realLevel - requiredReduction);
      });
    }

    public Builder setWithdrawMode(Bank.WithdrawMode mode) {
      return action(() -> {
        var bank = Inventories.bank();
        return bank.setWithdrawMode(mode) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder isDepositInventoryEnabled() {
      return condition(() -> Bank.isDepositInventoryEnabled());
    }

    public Builder isDepositEquipmentEnabled() {
      return condition(() -> Bank.isDepositEquipmentEnabled());
    }

    public Builder isBankFull() {
      return condition(() -> {
        var bank = Inventories.bank();
        return bank.isFull();
      });
    }

    public Builder isViewingMainTab() {
      return condition(() -> Bank.isViewingMainTab());
    }

    public Builder withdrawMode(Bank.WithdrawMode mode) {
      return condition(() -> {
        var bank = Inventories.bank();
        return bank.getWithdrawMode() == mode;
      });
    }

    public Builder deposit(Supplier<String> nameSupplier, int amount) {
      return action(() -> {
        var name = nameSupplier.get();
        if (name == null)
          return Status.FAILURE;

        var bank = Inventories.bank();
        return bank.deposit(name, amount) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder withdraw(Supplier<String> nameSupplier, int amount) {
      return action(() -> {
        var name = nameSupplier.get();
        if (name == null)
          return Status.FAILURE;

        var bank = Inventories.bank();
        return bank.withdraw(name, amount) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder withdrawX(String name, int amount) {
      return sequence()
          .setWithdrawMode(Bank.WithdrawMode.ITEM)
          .withdraw(name, amount)
          .end();
    }

    public Builder withdrawNoted(String name, int amount) {
      return sequence()
          .setWithdrawMode(Bank.WithdrawMode.NOTED)
          .withdraw(name, amount)
          .end();
    }

    public Builder depositAndWithdraw(Function<ItemQuery, ItemQueryResults> toDeposit, String toWithdraw, int amount) {
      return sequence()
          .depositAllExcept(toDeposit)
          .withdraw(toWithdraw, amount)
          .end();
    }

    // Deposit methods with amount suppliers
    public Builder deposit(int id, Function<Backpack, Integer> amountSupplier) {
      return action(() -> {
        var amount = amountSupplier.apply(Inventories.backpack());
        if (amount == null)
          return Status.FAILURE;

        var bank = Inventories.bank();
        return bank.deposit(id, amount) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder deposit(String name, Function<Backpack, Integer> amountSupplier) {
      return action(() -> {
        var amount = amountSupplier.apply(Inventories.backpack());
        if (amount == null)
          return Status.FAILURE;

        var bank = Inventories.bank();
        return bank.deposit(name, amount) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder deposit(Supplier<String> nameSupplier, Function<Backpack, Integer> amountSupplier) {
      return action(() -> {
        var name = nameSupplier.get();
        var amount = amountSupplier.apply(Inventories.backpack());
        if (name == null || amount == null)
          return Status.FAILURE;

        var bank = Inventories.bank();
        return bank.deposit(name, amount) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder deposit(Function<ItemQuery, Item> function, Function<Backpack, Integer> amountSupplier) {
      return action(() -> {
        var amount = amountSupplier.apply(Inventories.backpack());
        if (amount == null)
          return Status.FAILURE;

        var bank = Inventories.bank();
        return bank.deposit(function, amount) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    // Withdraw methods with amount suppliers
    public Builder withdraw(int id, Supplier<Integer> amountSupplier) {
      return action(() -> {
        var amount = amountSupplier.get();
        if (amount == null)
          return Status.FAILURE;

        var bank = Inventories.bank();
        return bank.withdraw(id, amount) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder withdraw(String name, Supplier<Integer> amountSupplier) {
      return action(() -> {
        var amount = amountSupplier.get();
        if (amount == null)
          return Status.FAILURE;

        var bank = Inventories.bank();
        return bank.withdraw(name, amount) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder withdraw(Supplier<String> nameSupplier, Supplier<Integer> amountSupplier) {
      return action(() -> {
        var name = nameSupplier.get();
        var amount = amountSupplier.get();
        if (name == null || amount == null)
          return Status.FAILURE;

        var bank = Inventories.bank();
        return bank.withdraw(name, amount) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder withdraw(Function<ItemQuery, Item> function, Supplier<Integer> amountSupplier) {
      return action(() -> {
        var amount = amountSupplier.get();
        if (amount == null)
          return Status.FAILURE;

        var bank = Inventories.bank();
        return bank.withdraw(function, amount) ? Status.SUCCESS : Status.FAILURE;
      });
    }

    public Builder withdrawX(String name, Supplier<Integer> amountSupplier) {
      return sequence()
          .setWithdrawMode(Bank.WithdrawMode.ITEM)
          .withdraw(name, amountSupplier)
          .end();
    }

    public Builder withdrawX(Supplier<String> nameSupplier, Supplier<Integer> amountSupplier) {
      return sequence()
          .setWithdrawMode(Bank.WithdrawMode.ITEM)
          .withdraw(nameSupplier, amountSupplier)
          .end();
    }

    public Builder withdrawNoted(String name, Supplier<Integer> amountSupplier) {
      return sequence()
          .setWithdrawMode(Bank.WithdrawMode.NOTED)
          .withdraw(name, amountSupplier)
          .end();
    }

    public Builder withdrawNoted(Supplier<String> nameSupplier, Supplier<Integer> amountSupplier) {
      return sequence()
          .setWithdrawMode(Bank.WithdrawMode.NOTED)
          .withdraw(nameSupplier, amountSupplier)
          .end();
    }

    public Builder depositAndWithdraw(Function<ItemQuery, ItemQueryResults> toDeposit,
        String toWithdraw,
        Supplier<Integer> amountSupplier) {
      return sequence()
          .depositAllExcept(toDeposit)
          .withdraw(toWithdraw, amountSupplier)
          .end();
    }

    public Builder depositAndWithdraw(Function<ItemQuery, ItemQueryResults> toDeposit,
        Supplier<String> toWithdrawSupplier,
        Supplier<Integer> amountSupplier) {
      return sequence()
          .depositAllExcept(toDeposit)
          .withdraw(toWithdrawSupplier, amountSupplier)
          .end();
    }

    public Builder inventoryFull(Supplier<Inventory> inventorySupplier) {
      return condition(() -> {
        var inventory = inventorySupplier.get();
        if (inventory == null)
          return false;

        return inventory.isFull();
      });
    }

    public Builder backpackFull() {
      return inventoryFull(Inventories::backpack);
    }

    public Builder bankFull() {
      return inventoryFull(Inventories::bank);
    }

    public Builder skillLevel(Skill skill, int level) {
      return condition(() -> Skills.getLevel(skill) >= level);
    }

    public Builder skillLevel(Supplier<Skill> skillProvider, int level) {
      return condition(() -> Skills.getLevel(skillProvider.get()) >= level);
    }

    public Builder skillLevel(Skill skill, Supplier<Integer> levelProvider) {
      return condition(() -> Skills.getLevel(skill) >= levelProvider.get());
    }

    public Builder skillLevel(Supplier<Skill> skillProvider, Supplier<Integer> levelProvider) {
      return condition(() -> Skills.getLevel(skillProvider.get()) >= levelProvider.get());
    }

    public Builder canContinueDialog() {
      return condition(Dialog::canContinue);
    }

    public Builder viewingChatOptions() {
      return condition(Dialog::isViewingChatOptions);
    }

    // public Builder hasChatOption(String optionText) {
    // return condition(() -> {
    // return !Dialog.getChatOptions()
    // .texts(text -> optionText.equals(text) ||
    // text.contains(optionText))
    // .results()
    // .isEmpty();
    // });
    // }

    public Builder hasChatOption(Function<ComponentQuery, ComponentQueryResults> predicate) {
      return condition(() -> {
        var options = Dialog.getChatOptions();
        var results = predicate.apply(options);
        if (results.isEmpty()) {
          Log.info("No results!");
        }

        return !results.isEmpty();
      });
    }

    public Builder chooseChatOption(Function<ComponentQuery, ComponentQueryResults> predicate) {
      return condition(() -> {
        var option = predicate.apply(Dialog.getChatOptions()).first();
        return option.interact("Continue");
      });
    }

    public Builder dialogTitle(Predicate<String> predicate) {
      return condition(() -> {
        var text = Dialog.getTitle();
        if (text == null || text.isEmpty() || text.isBlank())
          return false;

        return predicate.apply(text);
      });
    }

    public Builder continueDialog() {
      return condition(Dialog::processContinue);
    }

    public Builder closeInterfaces() {
      return condition(Interfaces::closeSubs);
    }

    public Builder worldHop(Function<WorldQuery, World> worldFunc) {
      return condition(() -> {
        var query = Worlds.queryNormal(true);
        var world = worldFunc.apply(query);
        return Worlds.hopTo(world);
      });
    }

    public Builder isAlive(Supplier<Npc> npcSupplier) {
      return condition(() -> {
        var npc = npcSupplier.get();
        if (npc == null)
          return false;

        return !npc.isDying();
      });
    }

    public Builder isEnvenomed() {
      return condition(Combat::isEnvenomed);
    }

    public Builder isProductionOpen() {
      return condition(() -> {
        var active = Production.getActive();
        if (active == null)
          return false;

        return active.isOpen();
      });
    }

    public Builder setProductionAmount(Production.Amount amount) {
      return condition(() -> {
        var active = Production.getActive();
        if (active == null)
          return false;

        if (active.getAmount() == amount)
          return true;

        return active.setAmount(amount);
      });
    }

    public Builder initiateProduction(int index) {
      return condition(() -> {
        var active = Production.getActive();
        if (active == null) {
          Log.info("Active null");
          return false;
        }

        active.initiate(index);
        return true;
      });
    }

    public Builder questProgress(Quest quest, Quest.Progress progress) {
      return condition(() -> Quests.getProgress(quest).equals(progress));
    }

    public Builder questState(Quest quest, IntSupplier state) {
      return condition(() -> Quests.getState(quest) == state.getAsInt());
    }

    public Builder enterInputOpen() {
      return condition(EnterInput::isOpen);
    }

    public Builder sendText(Supplier<String> textSupplier, boolean enter) {
      return succeed(() -> Keyboard.sendText(textSupplier.get(), enter));
    }

    public Builder distanceFrom(Supplier<SceneNode> nodeSupplier, int dist) {
      return condition(() -> {
        return nodeSupplier.get().distance(Players.self()) >= 5;
      });
    }

    public Builder wildernessLevel(int level) {
      return condition(() -> {
        return Wilderness.getLevel() > level;
      });
    }
  }
}