/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.player;

import io.github.exodar.event.Render2DEvent;
import io.github.exodar.event.Subscribe;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentText;
import org.lwjgl.input.Mouse;

public class AutoSort extends Module {
  private final TickSetting debug;
  private final SliderSetting startDelayMin;
  private final SliderSetting startDelayMax;
  private final SliderSetting actionDelayMin;
  private final SliderSetting actionDelayMax;
  private final SliderSetting mouseSpeed;
  private final SliderSetting curveFactor;
  private final SliderSetting jitterStrength;
  private final TickSetting blockMouse;
  private final TickSetting autoClose;
  private final SliderSetting closeDelayMin;
  private final SliderSetting closeDelayMax;
  private final SliderSetting swordSlot;
  private final SliderSetting pickSlot;
  private final SliderSetting axeSlot;
  private final SliderSetting bowSlot;
  private final SliderSetting pearlSlot;
  private final SliderSetting potionSlot;
  private final SliderSetting blockSlot;

  private final Random random = new Random();
  private long openTimestamp;
  private long nextActionTime;
  private Action currentAction;
  private Point currentPos = new Point(0.0D, 0.0D);
  private Point startPos = new Point(0.0D, 0.0D);
  private Point endPos = new Point(0.0D, 0.0D);
  private Point controlPos = new Point(0.0D, 0.0D);
  private long movementStartTime;
  private long movementDuration;
  private boolean active;
  private boolean finished;
  private long closeTime;
  private boolean didWork;

  public AutoSort() {
    super("AutoSort", ModuleCategory.PLAYER);
    this.registerSetting(debug = new TickSetting("Debug", false));
    this.registerSetting(startDelayMin = new SliderSetting("Start Delay Min", 150.0D, 0.0D, 1000.0D, 10.0D));
    this.registerSetting(startDelayMax = new SliderSetting("Start Delay Max", 300.0D, 0.0D, 1000.0D, 10.0D));
    this.registerSetting(actionDelayMin = new SliderSetting("Action Delay Min", 80.0D, 0.0D, 500.0D, 10.0D));
    this.registerSetting(actionDelayMax = new SliderSetting("Action Delay Max", 150.0D, 0.0D, 500.0D, 10.0D));
    this.registerSetting(mouseSpeed = new SliderSetting("Mouse Speed", 20.0D, 5.0D, 60.0D, 1.0D));
    this.registerSetting(curveFactor = new SliderSetting("Curve Factor", 2.5D, 0.1D, 5.0D, 0.1D));
    this.registerSetting(jitterStrength = new SliderSetting("Jitter", 0.5D, 0.0D, 2.0D, 0.1D));
    this.registerSetting(blockMouse = new TickSetting("Block Mouse", true));
    this.registerSetting(autoClose = new TickSetting("Auto Close", false));
    this.registerSetting(closeDelayMin = new SliderSetting("Close Delay Min", 100.0D, 0.0D, 500.0D, 10.0D));
    this.registerSetting(closeDelayMax = new SliderSetting("Close Delay Max", 200.0D, 0.0D, 500.0D, 10.0D));
    this.registerSetting(swordSlot = new SliderSetting("Sword Slot", 1.0D, 0.0D, 9.0D, 1.0D).setMinDisplayText("Disable"));
    this.registerSetting(pickSlot = new SliderSetting("Pickaxe Slot", 2.0D, 0.0D, 9.0D, 1.0D).setMinDisplayText("Disable"));
    this.registerSetting(axeSlot = new SliderSetting("Axe Slot", 3.0D, 0.0D, 9.0D, 1.0D).setMinDisplayText("Disable"));
    this.registerSetting(bowSlot = new SliderSetting("Bow Slot", 4.0D, 0.0D, 9.0D, 1.0D).setMinDisplayText("Disable"));
    this.registerSetting(pearlSlot = new SliderSetting("Pearl Slot", 5.0D, 0.0D, 9.0D, 1.0D).setMinDisplayText("Disable"));
    this.registerSetting(potionSlot = new SliderSetting("Potion Slot", 6.0D, 0.0D, 9.0D, 1.0D).setMinDisplayText("Disable"));
    this.registerSetting(blockSlot = new SliderSetting("Block Slot", 9.0D, 0.0D, 9.0D, 1.0D).setMinDisplayText("Disable"));
  }

  @Override
  public void onEnable() {
    reset();
  }

  @Override
  public void onDisable() {
    InventoryModuleHelper.release("AutoSort");
    reset();
  }

  @Subscribe
  public void onRender(Render2DEvent event) {
    if (!enabled || mc.thePlayer == null) return;

    if (!(mc.currentScreen instanceof GuiInventory)) {
      if (active || currentAction != null) {
        InventoryModuleHelper.release("AutoSort");
      }
      reset();
      return;
    }

    if (!InventoryModuleHelper.tryAcquire("AutoSort")) return;

    GuiInventory gui = (GuiInventory) mc.currentScreen;

    if (this.openTimestamp == 0L) {
      this.openTimestamp = System.currentTimeMillis();
      long startDelay = randomInt((int) this.startDelayMin.getValue(), (int) this.startDelayMax.getValue());
      this.nextActionTime = this.openTimestamp + startDelay;
      this.currentPos.x = (Mouse.getX() * gui.width / mc.displayWidth);
      this.currentPos.y = (gui.height - Mouse.getY() * gui.height / mc.displayHeight - 1);
      this.active = false;
      this.currentAction = null;
      this.finished = false;
      this.didWork = false;
    }

    if (this.blockMouse.isEnabled() && this.active && !this.finished && this.currentAction != null)
      Mouse.setCursorPosition((int) (this.currentPos.x * mc.displayWidth / gui.width), mc.displayHeight - (int) (this.currentPos.y * mc.displayHeight / gui.height) - 1);

    if (System.currentTimeMillis() < this.nextActionTime && !this.active) return;

    this.active = true;
    InventoryModuleHelper.keepAlive("AutoSort");

    if (this.currentAction == null && !this.finished) {
      this.currentAction = getNextAction();
      if (this.currentAction == null) {
        this.finished = true;
        long closeDelay = randomInt((int) this.closeDelayMin.getValue(), (int) this.closeDelayMax.getValue());
        this.closeTime = System.currentTimeMillis() + closeDelay;
        InventoryModuleHelper.release("AutoSort");
        return;
      }
      this.startPos = new Point(this.currentPos.x, this.currentPos.y);
      Slot slot = this.currentAction.slot;
      int targetX = (gui.width - 176) / 2 + slot.xDisplayPosition + 8;
      int targetY = (gui.height - 166) / 2 + slot.yDisplayPosition + 8;
      targetX += randomInt(-3, 3);
      targetY += randomInt(-3, 3);
      this.endPos = new Point(targetX, targetY);
      float dist = (float) Math.sqrt(Math.pow(this.endPos.x - this.startPos.x, 2.0D) + Math.pow(this.endPos.y - this.startPos.y, 2.0D));
      float offset = dist / (float) this.curveFactor.getValue();
      this.controlPos = new Point((this.startPos.x + this.endPos.x) / 2.0D + ((this.random.nextFloat() - 0.5F) * offset), (this.startPos.y + this.endPos.y) / 2.0D + ((this.random.nextFloat() - 0.5F) * offset));
      this.movementStartTime = System.currentTimeMillis();
      this.movementDuration = (long) (dist / (float) this.mouseSpeed.getValue() * 50.0F);
      this.movementDuration = Math.max(this.movementDuration, 100L);
    }

    if (this.currentAction != null) {
      long elapsed = System.currentTimeMillis() - this.movementStartTime;
      float progress = Math.min(1.0F, (float) elapsed / (float) this.movementDuration);
      float t = easeOutQuart(progress);
      double oneMinusT = (1.0F - t);
      double curX = oneMinusT * oneMinusT * this.startPos.x + 2.0D * oneMinusT * t * this.controlPos.x + (t * t) * this.endPos.x;
      double curY = oneMinusT * oneMinusT * this.startPos.y + 2.0D * oneMinusT * t * this.controlPos.y + (t * t) * this.endPos.y;
      if (progress < 1.0F) {
        curX += (this.random.nextFloat() - 0.5F) * this.jitterStrength.getValue();
        curY += (this.random.nextFloat() - 0.5F) * this.jitterStrength.getValue();
      }
      this.currentPos.x = curX;
      this.currentPos.y = curY;
      if (progress >= 1.0F) {
        performAction(this.currentAction);
        this.didWork = true;
        this.currentAction = null;
        long actionDelay = randomInt((int) this.actionDelayMin.getValue(), (int) this.actionDelayMax.getValue());
        this.nextActionTime = System.currentTimeMillis() + actionDelay;
      }
    }

    if (this.finished && this.didWork && this.autoClose.isEnabled() && System.currentTimeMillis() >= this.closeTime) {
      mc.thePlayer.closeScreen();
    }
  }

  private void debug(String msg) {
    if (debug.isEnabled() && mc.thePlayer != null) {
      mc.thePlayer.addChatMessage(new ChatComponentText("§b[AutoSort] §f" + msg));
    }
  }

  private void performAction(Action action) {
    int windowId = mc.thePlayer.inventoryContainer.windowId;
    int slotId = action.slot.slotNumber;
    ItemStack stack = action.slot.getStack();
    debug("§aSwap: " + (stack != null ? stack.getDisplayName() : "null") + " slot=" + slotId + " -> hotbar=" + (action.button + 1));
    mc.playerController.windowClick(windowId, slotId, action.button, 2, (EntityPlayer) mc.thePlayer);
  }

  private Action getNextAction() {
    int sSword = (int) this.swordSlot.getValue() - 1;
    int sPick = (int) this.pickSlot.getValue() - 1;
    int sAxe = (int) this.axeSlot.getValue() - 1;
    int sBow = (int) this.bowSlot.getValue() - 1;
    int sPearl = (int) this.pearlSlot.getValue() - 1;
    int sPotion = (int) this.potionSlot.getValue() - 1;
    int sBlock = (int) this.blockSlot.getValue() - 1;

    Action action = null;
    if ((action = checkSortTyped(isBestSword(), sSword, ItemSword.class)) != null) return action;
    if ((action = checkSortTyped(isBestPick(), sPick, ItemPickaxe.class)) != null) return action;
    if ((action = checkSortTyped(isBestAxe(), sAxe, ItemAxe.class)) != null) return action;
    if ((action = checkSortTyped(isBestBow(), sBow, ItemBow.class)) != null) return action;
    if ((action = checkSortTyped(isBestPearl(), sPearl, ItemEnderPearl.class)) != null) return action;
    if ((action = checkSortTyped(isBestPotion(), sPotion, ItemPotion.class)) != null) return action;

    if (sBlock >= 0 && sBlock < 9) {
      int bestBlockSlot = findBestBlockSlot();
      if (bestBlockSlot != -1) {
        ItemStack current = mc.thePlayer.inventory.getStackInSlot(sBlock);
        boolean isBlock = (current != null && current.getItem() instanceof net.minecraft.item.ItemBlock);
        if (bestBlockSlot != sBlock + 36 && !isBlock)
          return new Action(mc.thePlayer.inventoryContainer.getSlot(bestBlockSlot), sBlock);
      }
    }
    return null;
  }

  private Action checkSortTyped(int bestItemSlot, int desiredHotbarIndex, Class<?> itemClass) {
    if (bestItemSlot == -1 || desiredHotbarIndex < 0 || desiredHotbarIndex >= 9) {
      return null;
    }

    // Check if the target hotbar slot already has the correct item type
    ItemStack hotbarStack = mc.thePlayer.inventory.getStackInSlot(desiredHotbarIndex);
    if (hotbarStack != null && itemClass.isInstance(hotbarStack.getItem())) {
      // Already has this item type in hotbar, check if it's the best one
      int hotbarContainerSlot = 36 + desiredHotbarIndex;
      if (bestItemSlot == hotbarContainerSlot) {
        // Best item is already in the correct slot
        return null;
      }
      // There's already an item of this type in the hotbar slot
      // Only swap if the best item is significantly better (has higher rating)
      ItemStack bestStack = mc.thePlayer.inventoryContainer.getSlot(bestItemSlot).getStack();
      if (bestStack != null) {
        // For tools, compare efficiency
        if (itemClass == ItemPickaxe.class || itemClass == ItemAxe.class) {
          float bestRating = toolRating(bestStack);
          float currentRating = toolRating(hotbarStack);
          if (bestRating <= currentRating) {
            return null; // Current is good enough
          }
        }
        // For swords, compare damage
        else if (itemClass == ItemSword.class) {
          double bestDamage = getDamageSword(bestStack);
          double currentDamage = getDamageSword(hotbarStack);
          if (bestDamage <= currentDamage) {
            return null;
          }
        }
        // For bows, compare power
        else if (itemClass == ItemBow.class) {
          double bestPower = getBowPower(bestStack);
          double currentPower = getBowPower(hotbarStack);
          if (bestPower <= currentPower) {
            return null;
          }
        }
        // For pearls and potions, if there's already one, don't swap
        else {
          return null;
        }
      }
    }

    // Check if best item is already in the desired hotbar slot
    int currentHotbarSlot = bestItemSlot - 36;
    if (bestItemSlot >= 36 && currentHotbarSlot == desiredHotbarIndex) {
      return null;
    }

    return new Action(mc.thePlayer.inventoryContainer.getSlot(bestItemSlot), desiredHotbarIndex);
  }

  private int isBestSword() {
    int bestSlot = -1;
    double maxDamage = -1.0D;
    for (int i = 9; i < 45; i++) {
      ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
      if (stack != null && stack.getItem() instanceof ItemSword) {
        double damage = getDamageSword(stack);
        if (damage > maxDamage) {
          maxDamage = damage;
          bestSlot = i;
        }
      }
    }
    return bestSlot;
  }

  private int isBestBow() {
    int bestSlot = -1;
    double maxPower = -1.0D;
    for (int i = 9; i < 45; i++) {
      ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
      if (stack != null && stack.getItem() instanceof ItemBow) {
        double power = getBowPower(stack);
        if (power > maxPower) {
          maxPower = power;
          bestSlot = i;
        }
      }
    }
    return bestSlot;
  }

  private int isBestPick() {
    return getBestToolSlot(ItemPickaxe.class);
  }

  private int isBestAxe() {
    return getBestToolSlot(ItemAxe.class);
  }

  private int isBestPearl() {
    for (int i = 9; i < 45; i++) {
      ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
      if (stack != null && stack.getItem() instanceof ItemEnderPearl)
        return i;
    }
    return -1;
  }

  private int isBestPotion() {
    for (int i = 9; i < 45; i++) {
      ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
      if (stack != null && stack.getItem() instanceof ItemPotion) {
        ItemPotion potion = (ItemPotion) stack.getItem();
        if (ItemPotion.isSplash(stack.getMetadata())) {
          List<PotionEffect> effects = potion.getEffects(stack);
          if (effects != null)
            for (PotionEffect effect : effects) {
              if (effect.getPotionID() == Potion.heal.id || effect.getPotionID() == Potion.moveSpeed.id)
                return i;
            }
        }
      }
    }
    return -1;
  }

  private int findBestBlockSlot() {
    int bestSlot = -1;
    int maxStack = -1;
    for (int i = 9; i < 45; i++) {
      ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
      if (stack != null && stack.getItem() instanceof net.minecraft.item.ItemBlock && stack.stackSize > 0 &&
          stack.stackSize > maxStack) {
        maxStack = stack.stackSize;
        bestSlot = i;
      }
    }
    return bestSlot;
  }

  private int getBestToolSlot(Class<? extends ItemTool> toolClass) {
    int bestSlot = -1;
    float maxScore = -1.0F;
    for (int i = 9; i < 45; i++) {
      ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
      if (stack != null && toolClass.isInstance(stack.getItem())) {
        float score = toolRating(stack);
        if (score > maxScore) {
          maxScore = score;
          bestSlot = i;
        }
      }
    }
    return bestSlot;
  }

  private float toolRating(ItemStack stack) {
    if (!(stack.getItem() instanceof ItemTool)) return 0;
    ItemTool tool = (ItemTool) stack.getItem();
    float efficiency = tool.getToolMaterial().getEfficiencyOnProperMaterial();
    efficiency += EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack) * 0.3F;
    return efficiency;
  }

  private double getDamageSword(ItemStack stack) {
    double damage = 0.0D;
    if (stack.getItem() instanceof ItemSword) {
      ItemSword sword = (ItemSword) stack.getItem();
      damage += sword.getDamageVsEntity();
      damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 1.25D;
    }
    return damage;
  }

  private double getBowPower(ItemStack stack) {
    double power = 0.0D;
    power += EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack);
    return power;
  }

  private float easeOutQuart(float x) {
    return 1.0F - (float) Math.pow((1.0F - x), 4.0D);
  }

  private void reset() {
    this.openTimestamp = 0L;
    this.currentAction = null;
    this.active = false;
    this.finished = false;
    this.closeTime = 0L;
  }

  private int randomInt(int min, int max) {
    if (min >= max) return min;
    return min + random.nextInt(max - min + 1);
  }

  private static class Point {
    double x;
    double y;
    Point(double x, double y) {
      this.x = x;
      this.y = y;
    }
  }

  private static class Action {
    Slot slot;
    int button;
    Action(Slot slot, int button) {
      this.slot = slot;
      this.button = button;
    }
  }
}
