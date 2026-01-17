package cc.silk.module.modules.combat;

import cc.silk.event.impl.player.TickEvent;
import cc.silk.mixin.MinecraftClientAccessor;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.SlabBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class XbowCart extends Module {

    // Added "Slab" as an available mode
    private final ModeSetting firstAction = new ModeSetting("First", "Fire", "Fire", "Rail", "Slab", "None");
    private final ModeSetting secondAction = new ModeSetting("Second", "Rail", "Fire", "Rail", "Slab", "None");
    private final ModeSetting thirdAction = new ModeSetting("Third", "None", "Fire", "Rail", "Slab", "None");
    private final NumberSetting delay = new NumberSetting("Delay", 0, 10, 2, 1);

    private int tickCounter = 0;
    private int actionIndex = 0;
    private boolean active = false;
    private final List<String> sequence = new ArrayList<>();

    public XbowCart() {
        super("Xbow cart", "Customizable cart placement module", -1, Category.COMBAT);
        this.addSettings(firstAction, secondAction, thirdAction, delay);
    }

    @Override
    public void onEnable() {
        if (isNull()) {
            setEnabled(false);
            return;
        }

        sequence.clear();
        if (!firstAction.isMode("None")) sequence.add(firstAction.getMode());
        if (!secondAction.isMode("None")) sequence.add(secondAction.getMode());
        if (!thirdAction.isMode("None")) sequence.add(thirdAction.getMode());

        active = true;
        tickCounter = 0;
        actionIndex = 0;
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (!active || isNull()) return;

        if (actionIndex < sequence.size()) {
            if (tickCounter == 0) {
                String currentAction = sequence.get(actionIndex);
                executeAction(currentAction);
            }

            tickCounter++;

            if (tickCounter > delay.getValueInt()) {
                tickCounter = 0;
                actionIndex++;
            }
        } else {
            // After sequence finished, switch back to crossbow
            switchToItem(Items.CROSSBOW);
            active = false;
            setEnabled(false);
        }
    }

    private void executeAction(String action) {
        if (action.equals("Fire")) {
            if (switchToItem(Items.FLINT_AND_STEEL)) {
                ((MinecraftClientAccessor) mc).invokeDoItemUse();
            }
        } else if (action.equals("Rail")) {
            // More deterministic rail + tnt_minecart placement:
            // 1) Try to place a rail (any rail type).
            // 2) Then try to place a TNT_MINECART (hotbar first, fallback: swap from inventory).
            boolean placedRail = false;

            // Try to find any rail type in hotbar and place it
            if (switchToItem(Items.RAIL) || switchToItem(Items.POWERED_RAIL) ||
                switchToItem(Items.DETECTOR_RAIL) || switchToItem(Items.ACTIVATOR_RAIL)) {
                ((MinecraftClientAccessor) mc).invokeDoItemUse();
                placedRail = true;
            }

            // Try to place TNT minecart from hotbar first
            if (switchToItem(Items.TNT_MINECART)) {
                // call invoke twice to increase reliability (some servers/clients need consecutive clicks)
                ((MinecraftClientAccessor) mc).invokeDoItemUse();
                ((MinecraftClientAccessor) mc).invokeDoItemUse();
                return;
            }

            // If TNT cart wasn't in hotbar, try to find it in the rest of the inventory and swap it into the selected hotbar slot
            int invIndex = findItemInInventory(Items.TNT_MINECART);
            if (invIndex != -1) {
                // swap the found inventory slot into the currently selected hotbar slot (client-side)
                moveInventorySlotToSelected(invIndex);
                // attempt placement after moving
                ((MinecraftClientAccessor) mc).invokeDoItemUse();
                ((MinecraftClientAccessor) mc).invokeDoItemUse();
                return;
            }

            // If we didn't place anything, but we did place a rail, try a small fallback: re-attempt placing TNT in a subsequent tick(s).
            // (This module uses delay between actions; if TNT is in a slow-changing inventory, the user can set larger Delay.)
        } else if (action.equals("Slab")) {
            // Search hotbar for any slab-type block and place it
            // Prefer BlockItem whose block is a SlabBlock
            if (findAndSwitchToSlabInHotbar()) {
                ((MinecraftClientAccessor) mc).invokeDoItemUse();
                return;
            }

            // Fallback: explicit common slab items (in case the BlockItem/SlabBlock check fails for some versions)
            if (switchToItem(Items.OAK_SLAB) || switchToItem(Items.SPRUCE_SLAB) || switchToItem(Items.BIRCH_SLAB) ||
                switchToItem(Items.JUNGLE_SLAB) || switchToItem(Items.ACACIA_SLAB) || switchToItem(Items.DARK_OAK_SLAB) ||
                switchToItem(Items.STONE_SLAB) || switchToItem(Items.SANDSTONE_SLAB) || switchToItem(Items.CUT_SANDSTONE_SLAB) ||
                switchToItem(Items.PETRIFIED_OAK_SLAB) || switchToItem(Items.COBBLESTONE_SLAB) || switchToItem(Items.BRICK_SLAB) ||
                switchToItem(Items.STONE_BRICK_SLAB) || switchToItem(Items.NETHER_BRICK_SLAB) || switchToItem(Items.QUARTZ_SLAB) ||
                switchToItem(Items.RED_SANDSTONE_SLAB) || switchToItem(Items.PURPUR_SLAB) || switchToItem(Items.PRISMARINE_SLAB) ||
                switchToItem(Items.PRISMARINE_BRICK_SLAB) || switchToItem(Items.DARK_PRISMARINE_SLAB) || switchToItem(Items.SMOOTH_STONE_SLAB)) {
                ((MinecraftClientAccessor) mc).invokeDoItemUse();
            }
        }
    }

    /**
     * Switch to an item in the hotbar (0-8). Returns true if switched.
     */
    private boolean switchToItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                mc.player.getInventory().selectedSlot = i;
                return true;
            }
        }
        return false;
    }

    /**
     * Find any slab in the hotbar by checking BlockItem -> SlabBlock.
     * If found, select that hotbar slot and return true.
     */
    private boolean findAndSwitchToSlabInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                net.minecraft.block.Block block = ((BlockItem) stack.getItem()).getBlock();
                if (block instanceof SlabBlock) {
                    mc.player.getInventory().selectedSlot = i;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Search the whole player inventory (0..35) for an item and return its index or -1 if not found.
     * Note: indices 0-8 are hotbar, 9-35 typically main inventory.
     */
    private int findItemInInventory(net.minecraft.item.Item item) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Move the item at inventory index 'fromIndex' into the currently selected hotbar slot (client-side swap).
     * fromIndex is expected to be in 0..35. This swaps client-side stacks — server may correct if not in sync,
     * but many clients accept this for quick placement automation.
     */
    private void moveInventorySlotToSelected(int fromIndex) {
        int sel = mc.player.getInventory().selectedSlot;
        ItemStack from = mc.player.getInventory().getStack(fromIndex);
        ItemStack selStack = mc.player.getInventory().getStack(sel);
        mc.player.getInventory().setStack(sel, from);
        mc.player.getInventory().setStack(fromIndex, selStack);
    }
}
