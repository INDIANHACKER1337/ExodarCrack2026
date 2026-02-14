/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.event;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

/**
 * BlockShapeEvent - Fired when block collision shape is calculated
 * Port from LiquidBounce mixin system
 * Allows modules to modify block collision boxes (e.g., shrink for Spider)
 */
public class BlockShapeEvent extends Event {

    private final BlockPos pos;
    private AxisAlignedBB shape;

    public BlockShapeEvent(BlockPos pos, AxisAlignedBB shape) {
        this.pos = pos;
        this.shape = shape;
    }

    public BlockPos getPos() {
        return pos;
    }

    public AxisAlignedBB getShape() {
        return shape;
    }

    /**
     * Set the new collision shape for this block
     * @param shape The new bounding box
     */
    public void setShape(AxisAlignedBB shape) {
        this.shape = shape;
    }

    /**
     * Shrink the collision shape on X and Z axes
     * This makes the block appear narrower, allowing the player to "walk inside" it
     * @param x Amount to shrink on X axis (from each side)
     * @param z Amount to shrink on Z axis (from each side)
     */
    public void shrink(double x, double z) {
        if (shape != null) {
            this.shape = new AxisAlignedBB(
                shape.minX + x,
                shape.minY,
                shape.minZ + z,
                shape.maxX - x,
                shape.maxY,
                shape.maxZ - z
            );
        }
    }
}
