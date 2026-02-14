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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.ChatComponentText;
import org.lwjgl.input.Mouse;

public class ChestStealer extends Module {
  private final TickSetting debug;
  private final SliderSetting startDelayMin;
  private final SliderSetting startDelayMax;
  private final SliderSetting grabDelayMin;
  private final SliderSetting grabDelayMax;
  private final SliderSetting closeDelayMin;
  private final SliderSetting closeDelayMax;
  private final SliderSetting mouseSpeed;
  private final SliderSetting curveFactor;
  private final SliderSetting jitterStrength;
  private final TickSetting blockMouse;
  private final TickSetting ignoreTrash;
  private final TickSetting autoClose;
  private final TickSetting onlyBest;
  private final SliderSetting missClickChance;

  private final Random random = new Random();
  private long openTimestamp;
  private long nextActionTime;
  private Slot targetSlot;
  private Point currentPos = new Point(0.0D, 0.0D);
  private Point startPos = new Point(0.0D, 0.0D);
  private Point endPos = new Point(0.0D, 0.0D);
  private Point controlPos = new Point(0.0D, 0.0D);
  private long movementStartTime;
  private long movementDuration;
  private boolean stealing;
  private boolean finished;
  private boolean missClicked;

  public ChestStealer() {
    super("ChestStealer", ModuleCategory.PLAYER);
    this.registerSetting(debug = new TickSetting("Debug", false));
    this.registerSetting(startDelayMin = new SliderSetting("Start Delay Min", 150.0D, 0.0D, 1000.0D, 10.0D));
    this.registerSetting(startDelayMax = new SliderSetting("Start Delay Max", 300.0D, 0.0D, 1000.0D, 10.0D));
    this.registerSetting(grabDelayMin = new SliderSetting("Grab Delay Min", 80.0D, 0.0D, 500.0D, 10.0D));
    this.registerSetting(grabDelayMax = new SliderSetting("Grab Delay Max", 150.0D, 0.0D, 500.0D, 10.0D));
    this.registerSetting(closeDelayMin = new SliderSetting("Close Delay Min", 100.0D, 0.0D, 500.0D, 10.0D));
    this.registerSetting(closeDelayMax = new SliderSetting("Close Delay Max", 200.0D, 0.0D, 500.0D, 10.0D));
    this.registerSetting(mouseSpeed = new SliderSetting("Mouse Speed", 20.0D, 5.0D, 60.0D, 1.0D));
    this.registerSetting(curveFactor = new SliderSetting("Curve Factor", 2.5D, 0.1D, 5.0D, 0.1D));
    this.registerSetting(jitterStrength = new SliderSetting("Jitter", 0.5D, 0.0D, 2.0D, 0.1D));
    this.registerSetting(blockMouse = new TickSetting("Block Mouse", true));
    this.registerSetting(ignoreTrash = new TickSetting("Ignore Trash", true));
    this.registerSetting(autoClose = new TickSetting("Auto Close", true));
    this.registerSetting(onlyBest = new TickSetting("Only Best Items", true));
    this.registerSetting(missClickChance = new SliderSetting("Miss Click %", 5.0D, 0.0D, 50.0D, 1.0D));
  }

  private void debug(String msg) {
    if (debug.isEnabled() && mc.thePlayer != null) {
      mc.thePlayer.addChatMessage(new ChatComponentText("§e[ChestStealer] §f" + msg));
    }
  }

  @Override
  public void onEnable() {
    reset();
  }

  @Subscribe
  public void onRender(Render2DEvent event) {
    if (!enabled) return;
    if (!isInGame()) return;
    if (mc.currentScreen instanceof GuiChest) {
      GuiChest chest = (GuiChest)mc.currentScreen;
      ContainerChest container = (ContainerChest)chest.inventorySlots;

      // Only work with actual chests, not server menus
      if (!isRealChest(container)) {
        if (debug.isEnabled() && openTimestamp == 0L) {
          String name = container.getLowerChestInventory().getDisplayName().getUnformattedText();
          int size = container.getLowerChestInventory().getSizeInventory();
          debug("§cNot a real chest: '" + name + "' size=" + size);
        }
        reset();
        return;
      }

      if (this.openTimestamp == 0L) {
        String name = container.getLowerChestInventory().getDisplayName().getUnformattedText();
        debug("§aOpened chest: '" + name + "'");
        this.openTimestamp = System.currentTimeMillis();
        long startDelay = randomInt((int)this.startDelayMin.getValue(), (int)this.startDelayMax.getValue());
        this.nextActionTime = this.openTimestamp + startDelay;
        this.currentPos.x = (Mouse.getX() * chest.width / mc.displayWidth);
        this.currentPos.y = (chest.height - Mouse.getY() * chest.height / mc.displayHeight - 1);
        this.stealing = false;
        this.finished = false;
        this.targetSlot = null;
      }
      if (this.blockMouse.isEnabled())
        Mouse.setCursorPosition((int)(this.currentPos.x * mc.displayWidth / chest.width), mc.displayHeight - (int)(this.currentPos.y * mc.displayHeight / chest.height) - 1);
      if (System.currentTimeMillis() < this.nextActionTime && !this.stealing)
        return;
      this.stealing = true;
      if (this.targetSlot == null && !this.finished) {
        this.targetSlot = getNextSlot(container);
        if (this.targetSlot == null) {
          this.finished = true;
          debug("§aFinished stealing");
          long closeDelay = randomInt((int)this.closeDelayMin.getValue(), (int)this.closeDelayMax.getValue());
          this.nextActionTime = System.currentTimeMillis() + closeDelay;
          return;
        }
        ItemStack stack = this.targetSlot.getStack();
        debug("§bTarget: " + (stack != null ? stack.getDisplayName() : "null") + " slot=" + this.targetSlot.slotNumber);
        this.startPos = new Point(this.currentPos.x, this.currentPos.y);
        int targetX = (chest.width - 176) / 2 + this.targetSlot.xDisplayPosition + 8;
        int targetY = (chest.height - 166) / 2 + this.targetSlot.yDisplayPosition + 8;
        if (Math.random() * 100.0D < this.missClickChance.getValue()) {
          this.missClicked = true;
          targetX += (this.random.nextBoolean() ? 1 : -1) * randomInt(5, 15);
          targetY += (this.random.nextBoolean() ? 1 : -1) * randomInt(5, 15);
        } else {
          this.missClicked = false;
          targetX += randomInt(-4, 4);
          targetY += randomInt(-4, 4);
        }
        this.endPos = new Point(targetX, targetY);
        float dist = (float)Math.sqrt(Math.pow(this.endPos.x - this.startPos.x, 2.0D) + Math.pow(this.endPos.y - this.startPos.y, 2.0D));
        float offset = dist / (float)this.curveFactor.getValue();
        this.controlPos = new Point((this.startPos.x + this.endPos.x) / 2.0D + ((this.random.nextFloat() - 0.5F) * offset), (this.startPos.y + this.endPos.y) / 2.0D + ((this.random.nextFloat() - 0.5F) * offset));
        this.movementStartTime = System.currentTimeMillis();
        this.movementDuration = (long)(dist / (float)this.mouseSpeed.getValue() * 50.0F);
        this.movementDuration = Math.max(this.movementDuration, 100L);
      }
      if (this.targetSlot != null) {
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
        if (progress >= 1.0F)
          if (this.missClicked) {
            debug("§eMiss click!");
            this.targetSlot = null;
            this.missClicked = false;
            this.nextActionTime = System.currentTimeMillis() + 100L;
          } else {
            ItemStack stack = this.targetSlot.getStack();
            debug("§aSteal: " + (stack != null ? stack.getDisplayName() : "null") + " slot=" + this.targetSlot.slotNumber);
            mc.playerController.windowClick(container.windowId, this.targetSlot.slotNumber, 0, 1, (EntityPlayer)mc.thePlayer);
            mc.thePlayer.inventoryContainer.detectAndSendChanges();
            this.targetSlot = null;
            long grabDelay = randomInt((int)this.grabDelayMin.getValue(), (int)this.grabDelayMax.getValue());
            this.nextActionTime = System.currentTimeMillis() + grabDelay;
          }
      } else if (this.finished && this.autoClose.isEnabled() &&
        System.currentTimeMillis() >= this.nextActionTime) {
        debug("§aAuto closing");
        mc.thePlayer.closeScreen();
      }
    } else {
      reset();
    }
  }

  private float easeOutQuart(float x) {
    return 1.0F - (float)Math.pow((1.0F - x), 4.0D);
  }

  private Slot getNextSlot(ContainerChest container) {
    List<Slot> validSlots = new ArrayList<>();
    int chestSize = container.getLowerChestInventory().getSizeInventory();
    for (int i = 0; i < chestSize; i++) {
      Slot slot = container.inventorySlots.get(i);
      if (slot.getHasStack() && shouldSteal(slot.getStack()))
        validSlots.add(slot);
    }
    if (validSlots.isEmpty())
      return null;
    Collections.shuffle(validSlots);
    return validSlots.get(0);
  }

  private boolean shouldSteal(ItemStack stack) {
    if (!this.onlyBest.isEnabled())
      return !trash(stack, true, true);
    if (stack.getItem() instanceof ItemSword)
      return isBestSword(stack);
    if (stack.getItem() instanceof ItemArmor)
      return isBestArmor(stack);
    if (stack.getItem() instanceof net.minecraft.item.ItemTool)
      return isBestTool(stack);
    if (stack.getItem() instanceof net.minecraft.item.ItemBow)
      return isBestBow(stack);
    if (stack.getItem() instanceof net.minecraft.item.ItemBlock || stack.getItem() instanceof net.minecraft.item.ItemFood || stack
      .getItem() instanceof net.minecraft.item.ItemPotion || stack.getItem() instanceof net.minecraft.item.ItemFishingRod)
      return (!this.ignoreTrash.isEnabled() || !trash(stack, true, true));
    return !this.ignoreTrash.isEnabled();
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

  private boolean isBestSword(ItemStack itemStack) {
    double damage = getDamageSword(itemStack);
    for (int i = 0; i < 45; i++) {
      ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
      if (is != null && is.getItem() instanceof ItemSword && getDamageSword(is) >= damage)
        return false;
    }
    return true;
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

  private boolean isBestArmor(ItemStack itemStack) {
    ItemArmor armor = (ItemArmor)itemStack.getItem();
    double reduction = getDamageReduceAmount(itemStack);
    for (int i = 0; i < 45; i++) {
      ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
      if (is != null && is.getItem() instanceof ItemArmor) {
        ItemArmor otherArmor = (ItemArmor)is.getItem();
        if (armor.armorType == otherArmor.armorType && getDamageReduceAmount(is) >= reduction)
          return false;
      }
    }
    return true;
  }

  private double getDamageReduceAmount(ItemStack stack) {
    ItemArmor armor = (ItemArmor)stack.getItem();
    double amount = armor.damageReduceAmount;
    amount += EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack) * 0.04D;
    return amount;
  }

  private boolean isBestBow(ItemStack itemStack) {
    double power = getBowPower(itemStack);
    for (int i = 0; i < 45; i++) {
      ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
      if (is != null && is.getItem() instanceof net.minecraft.item.ItemBow && getBowPower(is) >= power)
        return false;
    }
    return true;
  }

  private double getBowPower(ItemStack stack) {
    double power = 0.0D;
    power += EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack);
    power += EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, stack) * 0.1D;
    return power;
  }

  private boolean isBestTool(ItemStack itemStack) {
    return !trash(itemStack, true, true);
  }

  private boolean isRealChest(ContainerChest container) {
    IInventory inv = container.getLowerChestInventory();
    int size = inv.getSizeInventory();

    // Real chests are 27 (single) or 54 (double) slots
    if (size != 27 && size != 54) {
      return false;
    }

    // Get name and manually strip ALL color codes (§ followed by any character)
    String rawName = inv.getDisplayName().getUnformattedText();
    String name = stripColorCodes(rawName);
    String lowerName = name.toLowerCase().trim();

    // Known chest names in various languages - these are ALWAYS real chests
    if (lowerName.equals("chest") || lowerName.equals("large chest") ||
        lowerName.equals("cofre") || lowerName.equals("cofre grande") ||  // Spanish
        lowerName.equals("truhe") || lowerName.equals("große truhe") ||   // German
        lowerName.equals("coffre") || lowerName.equals("grand coffre") || // French
        lowerName.equals("baú") || lowerName.equals("baú grande") ||      // Portuguese
        lowerName.equals("сундук") || lowerName.equals("большой сундук") || // Russian
        lowerName.equals("箱") || lowerName.equals("大きなチェスト") ||     // Japanese
        lowerName.equals("상자") || lowerName.equals("큰 상자")) {          // Korean
      return true;
    }

    // Check for common server menu patterns (these are NOT chests)
    if (lowerName.contains("menu") || lowerName.contains("shop") || lowerName.contains("kit") ||
        lowerName.contains("warp") || lowerName.contains("hub") || lowerName.contains("lobby") ||
        lowerName.contains("select") || lowerName.contains("game") || lowerName.contains("play") ||
        lowerName.contains("click") || lowerName.contains("buy") || lowerName.contains("sell") ||
        lowerName.contains("trade") || lowerName.contains("gui") || lowerName.contains("cosmetic") ||
        lowerName.contains("crate") || lowerName.contains("reward") || lowerName.contains("loot") ||
        lowerName.contains("tienda") || lowerName.contains("menú")) {  // Spanish server patterns
      return false;
    }

    // Size matches and no server menu patterns - assume it's a real chest
    return true;
  }

  private String stripColorCodes(String text) {
    if (text == null) return "";
    // Remove all color codes: § followed by any character (0-9, a-f, k-o, r)
    return text.replaceAll("§[0-9a-fk-or]", "").replaceAll("§", "");
  }

  private void reset() {
    this.openTimestamp = 0L;
    this.targetSlot = null;
    this.stealing = false;
    this.finished = false;
    this.missClicked = false;
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
}
