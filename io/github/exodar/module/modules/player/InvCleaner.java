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
import io.github.exodar.setting.KeybindSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import java.util.Random;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.util.ChatComponentText;
import org.lwjgl.input.Mouse;

public class InvCleaner extends Module {
  private final TickSetting debug;
  private final TickSetting alwaysClean;
  private final KeybindSetting forceCleanKey;
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
  private final SliderSetting maxBlockStacks;
  private final SliderSetting maxFoodStacks;
  private final TickSetting dropWorse;

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
  private boolean forceCleanActive;
  private boolean forceCleanKeyWasPressed;
  private boolean forceOpenedInventory;
  private boolean didWork;

  public InvCleaner() {
    super("InvCleaner", ModuleCategory.PLAYER);
    this.registerSetting(debug = new TickSetting("Debug", false));
    this.registerSetting(alwaysClean = new TickSetting("Always Clean", true));
    this.registerSetting(forceCleanKey = new KeybindSetting("Force Clean Key", 0));
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
    this.registerSetting(maxBlockStacks = new SliderSetting("Max Block Stacks", 2.0D, 0.0D, 8.0D, 1.0D));
    this.registerSetting(maxFoodStacks = new SliderSetting("Max Food Stacks", 1.0D, 0.0D, 8.0D, 1.0D));
    this.registerSetting(dropWorse = new TickSetting("Drop Worse", true));
  }

  @Override
  public void onEnable() {
    reset();
  }

  @Override
  public void onDisable() {
    InventoryModuleHelper.release("InvCleaner");
    reset();
  }

  @Override
  public void onUpdate() {
    if (!enabled || mc == null || mc.thePlayer == null) return;
    checkForceCleanKey();
    if (forceCleanActive && mc.currentScreen == null && !forceOpenedInventory) {
      mc.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiInventory(mc.thePlayer));
      forceOpenedInventory = true;
    }
  }

  private void checkForceCleanKey() {
    if (forceCleanKey.getKeyCode() == 0) return;
    boolean isPressed = forceCleanKey.isKeyPressed();
    if (isPressed && !forceCleanKeyWasPressed && !forceCleanActive) {
      forceCleanActive = true;
      forceOpenedInventory = false;
    }
    forceCleanKeyWasPressed = isPressed;
  }

  @Subscribe
  public void onRender(Render2DEvent event) {
    if (!enabled || mc.thePlayer == null) return;

    checkForceCleanKey();

    if (!(mc.currentScreen instanceof GuiInventory)) {
      if (active || currentAction != null) {
        InventoryModuleHelper.release("InvCleaner");
      }
      resetState();
      return;
    }

    if (!alwaysClean.isEnabled() && !forceCleanActive) {
      return;
    }

    if (!InventoryModuleHelper.tryAcquire("InvCleaner")) return;

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
    InventoryModuleHelper.keepAlive("InvCleaner");

    if (this.currentAction == null && !this.finished) {
      this.currentAction = getNextAction();
      if (this.currentAction == null) {
        this.finished = true;
        long closeDelay = randomInt((int) this.closeDelayMin.getValue(), (int) this.closeDelayMax.getValue());
        this.closeTime = System.currentTimeMillis() + closeDelay;
        InventoryModuleHelper.release("InvCleaner");
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

    if (this.finished && this.didWork && (this.autoClose.isEnabled() || forceCleanActive) && System.currentTimeMillis() >= this.closeTime) {
      mc.thePlayer.closeScreen();
      forceCleanActive = false;
      forceOpenedInventory = false;
    }

    if (this.finished && !this.didWork) {
      forceCleanActive = false;
      forceOpenedInventory = false;
    }
  }

  private void debug(String msg) {
    if (debug.isEnabled() && mc.thePlayer != null) {
      mc.thePlayer.addChatMessage(new ChatComponentText("§c[InvCleaner] §f" + msg));
    }
  }

  private void performAction(Action action) {
    int windowId = mc.thePlayer.inventoryContainer.windowId;
    int slotId = action.slot.slotNumber;
    ItemStack stack = action.slot.getStack();
    debug("§aDrop: " + (stack != null ? stack.getDisplayName() : "null") + " slot=" + slotId);
    mc.playerController.windowClick(windowId, slotId, 1, 4, (EntityPlayer) mc.thePlayer);
  }

  private Action getNextAction() {
    // First scan main inventory (9-35) for all item types
    for (int i = 9; i < 36; i++) {
      Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
      ItemStack stack = slot.getStack();
      if (stack != null && shouldClean(stack, i))
        return new Action(slot);
    }

    // Then scan hotbar (36-44) but ONLY for worse armor
    if (dropWorse.isEnabled()) {
      for (int i = 36; i < 45; i++) {
        Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
        ItemStack stack = slot.getStack();
        if (stack != null && stack.getItem() instanceof ItemArmor) {
          if (!isBestArmor(stack, i)) {
            debug("§eWorse armor in hotbar: " + stack.getDisplayName());
            return new Action(slot);
          }
        }
      }
    }

    return null;
  }

  private boolean shouldClean(ItemStack stack, int slot) {
    if (trash(stack)) {
      debug("§eTrash: " + stack.getDisplayName());
      return true;
    }
    if (stack.getItem() instanceof net.minecraft.item.ItemBlock && getBlockCount() > this.maxBlockStacks.getValue() * 64.0D) {
      debug("§eToo many blocks: " + stack.getDisplayName());
      return true;
    }
    if (stack.getItem() instanceof net.minecraft.item.ItemFood && getFoodCount() > this.maxFoodStacks.getValue() * 64.0D) {
      debug("§eToo much food: " + stack.getDisplayName());
      return true;
    }
    // Only drop worse items if setting is enabled
    if (!dropWorse.isEnabled()) return false;

    if (stack.getItem() instanceof ItemSword && !isBestSword(stack, slot)) {
      debug("§eWorse sword: " + stack.getDisplayName());
      return true;
    }
    if (stack.getItem() instanceof ItemArmor && !isBestArmor(stack, slot)) {
      debug("§eWorse armor: " + stack.getDisplayName());
      return true;
    }
    if (stack.getItem() instanceof ItemBow && !isBestBow(stack, slot)) {
      debug("§eWorse bow: " + stack.getDisplayName());
      return true;
    }
    if (stack.getItem() instanceof ItemTool && !isBestTool(stack, slot)) {
      debug("§eWorse tool: " + stack.getDisplayName());
      return true;
    }
    return false;
  }

  private boolean trash(ItemStack stack) {
    if (stack == null) return false;
    String name = stack.getDisplayName().toLowerCase();
    if (name.contains("stick") || name.contains("string") || name.contains("rotten") ||
        name.contains("spider eye") || name.contains("bone") || name.contains("gunpowder") ||
        name.contains("seeds") || name.contains("feather") || name.contains("flint") ||
        name.contains("snow") || name.contains("anvil") || name.contains("gravel")) {
      return true;
    }
    return false;
  }

  private int getBlockCount() {
    int count = 0;
    for (int i = 9; i < 45; i++) {
      ItemStack s = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
      if (s != null && s.getItem() instanceof net.minecraft.item.ItemBlock)
        count += s.stackSize;
    }
    return count;
  }

  private int getFoodCount() {
    int count = 0;
    for (int i = 9; i < 45; i++) {
      ItemStack s = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
      if (s != null && s.getItem() instanceof net.minecraft.item.ItemFood)
        count += s.stackSize;
    }
    return count;
  }

  private boolean isBestSword(ItemStack stack, int mySlot) {
    double damage = getDamageSword(stack);
    // Compare against ALL inventory including hotbar (9-44)
    for (int i = 9; i < 45; i++) {
      ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
      if (is != null && is != stack && is.getItem() instanceof ItemSword) {
        double otherDamage = getDamageSword(is);
        // If hotbar item (36-44) is better or equal, this inventory item is worse
        if (otherDamage > damage) return false;
        // If equal damage and other is in hotbar, keep hotbar version
        if (otherDamage == damage && i >= 36) return false;
        // If equal damage in inventory, prefer lower slot
        if (otherDamage == damage && i < mySlot) return false;
      }
    }
    return true;
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

  private boolean isBestArmor(ItemStack stack, int mySlot) {
    if (!(stack.getItem() instanceof ItemArmor)) return false;
    ItemArmor armor = (ItemArmor) stack.getItem();
    double reduction = getDamageReduceAmount(stack);
    // Also check equipped armor slots (5-8)
    int equippedSlot = 5 + armor.armorType;
    ItemStack equipped = mc.thePlayer.inventoryContainer.getSlot(equippedSlot).getStack();
    if (equipped != null && equipped.getItem() instanceof ItemArmor) {
      double equippedReduction = getDamageReduceAmount(equipped);
      if (equippedReduction >= reduction) return false; // Equipped is better or equal
    }
    // Compare against ALL inventory including hotbar (9-44)
    for (int i = 9; i < 45; i++) {
      ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
      if (is != null && is != stack && is.getItem() instanceof ItemArmor) {
        ItemArmor otherArmor = (ItemArmor) is.getItem();
        if (armor.armorType == otherArmor.armorType) {
          double otherReduction = getDamageReduceAmount(is);
          if (otherReduction > reduction) return false;
          // If equal and other is in hotbar, keep hotbar version
          if (otherReduction == reduction && i >= 36) return false;
          if (otherReduction == reduction && i < mySlot) return false;
        }
      }
    }
    return true;
  }

  private double getDamageReduceAmount(ItemStack stack) {
    ItemArmor armor = (ItemArmor) stack.getItem();
    double amount = armor.damageReduceAmount;
    amount += EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack) * 0.04D;
    return amount;
  }

  private boolean isBestBow(ItemStack stack, int mySlot) {
    double power = getBowPower(stack);
    // Compare against ALL inventory including hotbar (9-44)
    for (int i = 9; i < 45; i++) {
      ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
      if (is != null && is != stack && is.getItem() instanceof ItemBow) {
        double otherPower = getBowPower(is);
        if (otherPower > power) return false;
        // If equal and other is in hotbar, keep hotbar version
        if (otherPower == power && i >= 36) return false;
        if (otherPower == power && i < mySlot) return false;
      }
    }
    return true;
  }

  private double getBowPower(ItemStack stack) {
    double power = 0.0D;
    power += EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack);
    return power;
  }

  private boolean isBestTool(ItemStack stack, int mySlot) {
    if (!(stack.getItem() instanceof ItemTool)) return false;
    ItemTool tool = (ItemTool) stack.getItem();
    Class<?> toolClass = tool.getClass();
    float rating = toolRating(stack);
    // Compare against ALL inventory including hotbar (9-44)
    for (int i = 9; i < 45; i++) {
      ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
      if (is != null && is != stack && toolClass.isInstance(is.getItem())) {
        float otherRating = toolRating(is);
        if (otherRating > rating) return false;
        // If equal and other is in hotbar, keep hotbar version
        if (otherRating == rating && i >= 36) return false;
        if (otherRating == rating && i < mySlot) return false;
      }
    }
    return true;
  }

  private float toolRating(ItemStack stack) {
    if (!(stack.getItem() instanceof ItemTool)) return 0;
    ItemTool tool = (ItemTool) stack.getItem();
    float efficiency = tool.getToolMaterial().getEfficiencyOnProperMaterial();
    efficiency += EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack) * 0.3F;
    return efficiency;
  }

  private float easeOutQuart(float x) {
    return 1.0F - (float) Math.pow((1.0F - x), 4.0D);
  }

  private void resetState() {
    this.openTimestamp = 0L;
    this.currentAction = null;
    this.active = false;
    this.finished = false;
    this.closeTime = 0L;
    this.didWork = false;
  }

  private void reset() {
    resetState();
    this.forceCleanActive = false;
    this.forceOpenedInventory = false;
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
    Action(Slot slot) {
      this.slot = slot;
    }
  }
}
