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
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import org.lwjgl.input.Mouse;

public class TestInvManager extends Module {
  private final SliderSetting startDelayMin;
  private final SliderSetting startDelayMax;
  private final SliderSetting actionDelayMin;
  private final SliderSetting actionDelayMax;
  private final SliderSetting mouseSpeed;
  private final SliderSetting curveFactor;
  private final SliderSetting jitterStrength;
  private final TickSetting openInv;
  private final TickSetting blockMouse;
  private final TickSetting autoArmor;
  private final TickSetting autoClean;
  private final TickSetting autoSort;
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
  private final SliderSetting maxBlockStacks;
  private final SliderSetting maxFoodStacks;

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

  public TestInvManager() {
    super("TestInvManager", ModuleCategory.PLAYER);
    this.registerSetting(startDelayMin = new SliderSetting("Start Delay Min", 150.0D, 0.0D, 1000.0D, 10.0D));
    this.registerSetting(startDelayMax = new SliderSetting("Start Delay Max", 300.0D, 0.0D, 1000.0D, 10.0D));
    this.registerSetting(actionDelayMin = new SliderSetting("Action Delay Min", 80.0D, 0.0D, 500.0D, 10.0D));
    this.registerSetting(actionDelayMax = new SliderSetting("Action Delay Max", 150.0D, 0.0D, 500.0D, 10.0D));
    this.registerSetting(mouseSpeed = new SliderSetting("Mouse Speed", 20.0D, 5.0D, 60.0D, 1.0D));
    this.registerSetting(curveFactor = new SliderSetting("Curve Factor", 2.5D, 0.1D, 5.0D, 0.1D));
    this.registerSetting(jitterStrength = new SliderSetting("Jitter", 0.5D, 0.0D, 2.0D, 0.1D));
    this.registerSetting(openInv = new TickSetting("Open Inv Only", true));
    this.registerSetting(blockMouse = new TickSetting("Block Mouse", true));
    this.registerSetting(autoArmor = new TickSetting("Auto Armor", true));
    this.registerSetting(autoClean = new TickSetting("Auto Clean", true));
    this.registerSetting(autoSort = new TickSetting("Auto Sort", true));
    this.registerSetting(autoClose = new TickSetting("Auto Close", true));
    this.registerSetting(closeDelayMin = new SliderSetting("Close Delay Min", 100.0D, 0.0D, 500.0D, 10.0D));
    this.registerSetting(closeDelayMax = new SliderSetting("Close Delay Max", 200.0D, 0.0D, 500.0D, 10.0D));
    this.registerSetting(swordSlot = new SliderSetting("Sword Slot", 1.0D, 1.0D, 9.0D, 1.0D));
    this.registerSetting(pickSlot = new SliderSetting("Pickaxe Slot", 2.0D, 1.0D, 9.0D, 1.0D));
    this.registerSetting(axeSlot = new SliderSetting("Axe Slot", 3.0D, 1.0D, 9.0D, 1.0D));
    this.registerSetting(bowSlot = new SliderSetting("Bow Slot", 4.0D, 1.0D, 9.0D, 1.0D));
    this.registerSetting(pearlSlot = new SliderSetting("Pearl Slot", 5.0D, 1.0D, 9.0D, 1.0D));
    this.registerSetting(potionSlot = new SliderSetting("Potion Slot", 6.0D, 1.0D, 9.0D, 1.0D));
    this.registerSetting(blockSlot = new SliderSetting("Block Slot", 9.0D, 1.0D, 9.0D, 1.0D));
    this.registerSetting(maxBlockStacks = new SliderSetting("Max Block Stacks", 2.0D, 0.0D, 8.0D, 1.0D));
    this.registerSetting(maxFoodStacks = new SliderSetting("Max Food Stacks", 1.0D, 0.0D, 8.0D, 1.0D));
  }

  @Override
  public void onEnable() {
    reset();
  }

  @Subscribe
  public void onRender(Render2DEvent event) {
    if (!enabled) return;
    if (mc.currentScreen instanceof GuiInventory) {
      GuiInventory gui = (GuiInventory)mc.currentScreen;
      if (this.openTimestamp == 0L) {
        this.openTimestamp = System.currentTimeMillis();
        long startDelay = randomInt((int)this.startDelayMin.getValue(), (int)this.startDelayMax.getValue());
        this.nextActionTime = this.openTimestamp + startDelay;
        this.currentPos.x = (Mouse.getX() * gui.width / mc.displayWidth);
        this.currentPos.y = (gui.height - Mouse.getY() * gui.height / mc.displayHeight - 1);
        this.active = false;
        this.currentAction = null;
      }
      if (this.blockMouse.isEnabled())
        Mouse.setCursorPosition((int)(this.currentPos.x * mc.displayWidth / gui.width), mc.displayHeight - (int)(this.currentPos.y * mc.displayHeight / gui.height) - 1);
      if (System.currentTimeMillis() < this.nextActionTime && !this.active)
        return;
      this.active = true;
      if (this.currentAction == null && !this.finished) {
        this.currentAction = getNextAction();
        if (this.currentAction == null) {
          this.finished = true;
          long closeDelay = randomInt((int)this.closeDelayMin.getValue(), (int)this.closeDelayMax.getValue());
          this.closeTime = System.currentTimeMillis() + closeDelay;
          return;
        }
        this.startPos = new Point(this.currentPos.x, this.currentPos.y);
        Slot slot = this.currentAction.slot;
        int targetX = (gui.width - 176) / 2 + slot.xDisplayPosition + 8;
        int targetY = (gui.height - 166) / 2 + slot.yDisplayPosition + 8;
        targetX += randomInt(-3, 3);
        targetY += randomInt(-3, 3);
        this.endPos = new Point(targetX, targetY);
        float dist = (float)Math.sqrt(Math.pow(this.endPos.x - this.startPos.x, 2.0D) + Math.pow(this.endPos.y - this.startPos.y, 2.0D));
        float offset = dist / (float)this.curveFactor.getValue();
        this.controlPos = new Point((this.startPos.x + this.endPos.x) / 2.0D + ((this.random.nextFloat() - 0.5F) * offset), (this.startPos.y + this.endPos.y) / 2.0D + ((this.random.nextFloat() - 0.5F) * offset));
        this.movementStartTime = System.currentTimeMillis();
        this.movementDuration = (long)(dist / (float)this.mouseSpeed.getValue() * 50.0F);
        this.movementDuration = Math.max(this.movementDuration, 100L);
      }
      if (this.currentAction != null) {
        long elapsed = System.currentTimeMillis() - this.movementStartTime;
        float progress = Math.min(1.0F, (float)elapsed / (float)this.movementDuration);
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
          this.currentAction = null;
          long actionDelay = randomInt((int)this.actionDelayMin.getValue(), (int)this.actionDelayMax.getValue());
          this.nextActionTime = System.currentTimeMillis() + actionDelay;
        }
      }
      if (this.finished && this.autoClose.isEnabled() && System.currentTimeMillis() >= this.closeTime) {
        mc.thePlayer.closeScreen();
      }
    } else if (this.openInv.isEnabled()) {
      reset();
    } else {
      reset();
    }
  }

  private void performAction(Action action) {
    int windowId = mc.thePlayer.inventoryContainer.windowId;
    int slotId = action.slot.slotNumber;
    switch (action.type) {
      case DROP:
        mc.playerController.windowClick(windowId, slotId, 1, 4, (EntityPlayer)mc.thePlayer);
        break;
      case SHIFT_CLICK:
        mc.playerController.windowClick(windowId, slotId, 0, 1, (EntityPlayer)mc.thePlayer);
        break;
      case SWAP:
        mc.playerController.windowClick(windowId, slotId, action.button, 2, (EntityPlayer)mc.thePlayer);
        break;
    }
  }

  private Action getNextAction() {
    if (this.autoArmor.isEnabled())
      for (int i = 5; i < 9; i++) {
        ItemStack equipped = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
        if (equipped != null) {
          if (!isBestArmor(equipped))
            return new Action(mc.thePlayer.inventoryContainer.getSlot(i), ActionType.SHIFT_CLICK);
        } else {
          int type = i - 4;
          int bestSlot = getBestArmorSlotInInv(type);
          if (bestSlot != -1)
            return new Action(mc.thePlayer.inventoryContainer.getSlot(bestSlot), ActionType.SHIFT_CLICK);
        }
      }
    if (this.autoClean.isEnabled())
      for (int i = 9; i < 45; i++) {
        Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
        ItemStack stack = slot.getStack();
        if (stack != null && shouldClean(stack))
          return new Action(slot, ActionType.DROP);
      }
    if (this.autoSort.isEnabled()) {
      int sSword = (int)this.swordSlot.getValue() - 1;
      int sPick = (int)this.pickSlot.getValue() - 1;
      int sAxe = (int)this.axeSlot.getValue() - 1;
      int sBow = (int)this.bowSlot.getValue() - 1;
      int sPearl = (int)this.pearlSlot.getValue() - 1;
      int sPotion = (int)this.potionSlot.getValue() - 1;
      int sBlock = (int)this.blockSlot.getValue() - 1;
      Action action = null;
      if ((action = checkSort(isBestSword(), sSword)) != null)
        return action;
      if ((action = checkSort(isBestPick(), sPick)) != null)
        return action;
      if ((action = checkSort(isBestAxe(), sAxe)) != null)
        return action;
      if ((action = checkSort(isBestBow(), sBow)) != null)
        return action;
      if ((action = checkSort(isBestPearl(), sPearl)) != null)
        return action;
      if ((action = checkSort(isBestPotion(), sPotion)) != null)
        return action;
      if (sBlock >= 0 && sBlock < 9) {
        int bestBlockSlot = findBestBlockSlot();
        if (bestBlockSlot != -1) {
          ItemStack current = mc.thePlayer.inventory.getStackInSlot(sBlock);
          boolean isBlock = (current != null && current.getItem() instanceof net.minecraft.item.ItemBlock);
          if (bestBlockSlot != sBlock + 36 && !isBlock)
            return new Action(mc.thePlayer.inventoryContainer.getSlot(bestBlockSlot), ActionType.SWAP, sBlock);
        }
      }
    }
    return null;
  }

  private Action checkSort(int bestItemSlot, int desiredHotbarIndex) {
    if (bestItemSlot != -1 && desiredHotbarIndex >= 0 && desiredHotbarIndex < 9) {
      int currentHotbarSlot = bestItemSlot - 36;
      if (bestItemSlot < 36 || currentHotbarSlot != desiredHotbarIndex)
        return new Action(mc.thePlayer.inventoryContainer.getSlot(bestItemSlot), ActionType.SWAP, desiredHotbarIndex);
    }
    return null;
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
      if (stack != null && stack.getItem() instanceof net.minecraft.item.ItemBow) {
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
      if (stack != null && stack.getItem() instanceof net.minecraft.item.ItemEnderPearl)
        return i;
    }
    return -1;
  }

  private int isBestPotion() {
    for (int i = 9; i < 45; i++) {
      ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
      if (stack != null && stack.getItem() instanceof ItemPotion) {
        ItemPotion potion = (ItemPotion)stack.getItem();
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
    efficiency += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack) * 0.1F;
    return efficiency;
  }

  private boolean shouldClean(ItemStack stack) {
    if (trash(stack, true, true))
      return true;
    if (stack.getItem() instanceof net.minecraft.item.ItemBlock &&
      getBlockCount() > this.maxBlockStacks.getValue() * 64.0D)
      return true;
    if (stack.getItem() instanceof net.minecraft.item.ItemFood &&
      getFoodCount() > this.maxFoodStacks.getValue() * 64.0D)
      return true;
    return false;
  }

  private boolean trash(ItemStack stack, boolean checkArmor, boolean checkTools) {
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

  private boolean isBestArmor(ItemStack stack) {
    if (!(stack.getItem() instanceof ItemArmor))
      return false;
    ItemArmor armor = (ItemArmor)stack.getItem();
    double reduction = getDamageReduceAmount(stack);
    for (int i = 9; i < 45; i++) {
      ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
      if (is != null && is.getItem() instanceof ItemArmor) {
        ItemArmor otherArmor = (ItemArmor)is.getItem();
        if (armor.armorType == otherArmor.armorType && getDamageReduceAmount(is) > reduction)
          return false;
      }
    }
    return true;
  }

  private int getBestArmorSlotInInv(int type) {
    int bestSlot = -1;
    double maxReduc = -1.0D;
    for (int i = 9; i < 45; i++) {
      ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
      if (stack != null && stack.getItem() instanceof ItemArmor) {
        ItemArmor armor = (ItemArmor)stack.getItem();
        if (armor.armorType == type - 1) {
          double reduc = getDamageReduceAmount(stack);
          if (reduc > maxReduc) {
            maxReduc = reduc;
            bestSlot = i;
          }
        }
      }
    }
    return bestSlot;
  }

  private double getDamageReduceAmount(ItemStack stack) {
    ItemArmor armor = (ItemArmor)stack.getItem();
    double amount = armor.damageReduceAmount;
    amount += EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack) * 0.04D;
    amount += EnchantmentHelper.getEnchantmentLevel(Enchantment.projectileProtection.effectId, stack) * 0.02D;
    amount += EnchantmentHelper.getEnchantmentLevel(Enchantment.blastProtection.effectId, stack) * 0.02D;
    return amount;
  }

  private double getDamageSword(ItemStack stack) {
    double damage = 0.0D;
    if (stack.getItem() instanceof ItemSword) {
      ItemSword sword = (ItemSword)stack.getItem();
      damage += sword.getDamageVsEntity();
      damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 1.25D;
      damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack) * 0.01D;
    }
    return damage;
  }

  private double getBowPower(ItemStack stack) {
    double power = 0.0D;
    power += EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack);
    power += EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, stack) * 0.1D;
    return power;
  }

  private float easeOutQuart(float x) {
    return 1.0F - (float)Math.pow((1.0F - x), 4.0D);
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

    ActionType type;

    int button;

    Action(Slot slot, ActionType type) {
      this.slot = slot;
      this.type = type;
    }

    Action(Slot slot, ActionType type, int button) {
      this.slot = slot;
      this.type = type;
      this.button = button;
    }
  }

  private enum ActionType {
    DROP, SHIFT_CLICK, SWAP;
  }
}
