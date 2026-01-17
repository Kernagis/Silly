package cc.silk.module.modules.combat;

import cc.silk.event.impl.input.HandleInputEvent;
import cc.silk.mixin.MinecraftClientAccessor;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class AutoCart extends Module {

    private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", true);

    private boolean isActive = false;
    private BlockPos targetPos = null;
    private int originalSlot = -1;
    private int tickCounter = 0;
    private boolean hasRail = false;
    private boolean hasTntCart = false;
    private int railRetries = 0;

    // increased distance for raycasts to improve reach
    private static final double RAYCAST_DISTANCE = 6.5;
    // how many times we retry placing a rail before giving up
    private static final int MAX_RAIL_RETRIES = 5;

    public AutoCart() {
        super("Auto Cart", "Places TNT minecarts on rails when shooting arrows", -1, Category.COMBAT);
        this.addSettings(autoSwitch);
    }

    @EventHandler
    private void onTickEvent(HandleInputEvent event) {
        if (isNull()) return;

        if (mc.player.isUsingItem() && mc.player.getActiveItem().getItem() == Items.BOW) {
            if (!isActive) {
                startPlacing();
            }
        } else if (isActive && !mc.player.isUsingItem()) {
            if (tickCounter == 0) {
                tickCounter = 1;
            }
        }

        if (!isActive) return;

        if (tickCounter == 1) {
            placeRail();
            // either advance to next stage or remain to retry placing
            if (hasRail) {
                tickCounter = 2;
            } else {
                // allow retrying rail placement a few times to tolerate timing issues
                railRetries++;
                if (railRetries > MAX_RAIL_RETRIES) {
                    // give up and stop
                    stopPlacing();
                }
            }
        } else if (tickCounter == 2) {
            placeTntCart();
            stopPlacing();
        }
    }

    private void startPlacing() {
        if (isActive) return;

        if (mc.player.getMainHandStack().getItem() != Items.BOW) {
            return;
        }

        BlockPos targetPos = getTargetPosition();
        if (targetPos == null) return;

        this.targetPos = targetPos;
        isActive = true;
        tickCounter = 0;
        originalSlot = mc.player.getInventory().selectedSlot;
        railRetries = 0;
    }

    private void stopPlacing() {
        if (!isActive) return;

        if (autoSwitch.getValue() && originalSlot != -1) {
            mc.player.getInventory().selectedSlot = originalSlot;
        }

        resetState();
    }

    private void resetState() {
        isActive = false;
        targetPos = null;
        originalSlot = -1;
        tickCounter = 0;
        hasRail = false;
        hasTntCart = false;
        railRetries = 0;
    }

    private void placeRail() {
        if (hasRail) return;
        if (targetPos == null) return;

        int railSlot = findAnyRailInHotbar();
        if (railSlot == -1) return;

        // select the rail slot
        mc.player.getInventory().selectedSlot = railSlot;

        // Set crosshair target to the top face of the block under the target position,
        // so right-click places the rail on that block.
        BlockPos placeOn = targetPos.down();
        BlockHitResult bh = createTopFaceHit(placeOn);
        mc.crosshairTarget = bh;

        ((MinecraftClientAccessor) mc).invokeDoItemUse();
        hasRail = true;
    }

    private void placeTntCart() {
        if (hasTntCart) {
            stopPlacing();
            return;
        }

        if (targetPos == null) return;

        int tntCartSlot = findItemInHotbar();
        if (tntCartSlot == -1) return;

        mc.player.getInventory().selectedSlot = tntCartSlot;

        // For placing a minecart, target the rail's block position (the space where the rail sits).
        BlockHitResult bh = createTopFaceHit(targetPos);
        mc.crosshairTarget = bh;

        ((MinecraftClientAccessor) mc).invokeDoItemUse();
        hasTntCart = true;
    }

    private BlockPos getTargetPosition() {
        HitResult hitResult = mc.crosshairTarget;
        if (hitResult == null) return null;

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            // If the player is looking at the side of a block, place on the adjacent block.
            return blockHit.getBlockPos().offset(blockHit.getSide());
        } else if (hitResult.getType() == HitResult.Type.ENTITY) {
            // place at player's foot level +1 so rails are placed under entity
            return mc.player.getBlockPos().add(0, 1, 0);
        } else {
            // If crosshair didn't hit a block or entity, do a raycast forward a bit further
            Vec3d cameraPos = mc.player.getCameraPosVec(1.0f);
            Vec3d rotation = mc.player.getRotationVec(1.0f);
            Vec3d end = cameraPos.add(rotation.multiply(RAYCAST_DISTANCE));

            BlockHitResult blockHit = mc.world.raycast(new RaycastContext(cameraPos, end,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));

            if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
                // target the adjacent block in the direction we're pointing
                return blockHit.getBlockPos().offset(blockHit.getSide());
            } else {
                // fallback: place on block above player's feet
                return mc.player.getBlockPos().add(0, 1, 0);
            }
        }
    }

    private int findItemInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.TNT_MINECART) {
                return i;
            }
        }
        return -1;
    }

    private int findAnyRailInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && isRail(stack.getItem())) {
                return i;
            }
        }
        return -1;
    }

    private boolean isRail(net.minecraft.item.Item item) {
        return item == Items.RAIL ||
               item == Items.POWERED_RAIL ||
               item == Items.DETECTOR_RAIL ||
               item == Items.ACTIVATOR_RAIL;
    }

    /**
     * Creates a BlockHitResult pointing to the top face center of the given blockPos.
     * We use the top face because rails are placed on top of blocks and minecarts are placed on rails' top face.
     */
    private BlockHitResult createTopFaceHit(BlockPos blockPos) {
        // hit position: center of the top face
        Vec3d hitPos = new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 1.0, blockPos.getZ() + 0.5);
        return new BlockHitResult(hitPos, Direction.UP, blockPos, false);
    }

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        if (isActive) {
            stopPlacing();
        }
    }
}
