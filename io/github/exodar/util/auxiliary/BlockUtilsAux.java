/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.util.auxiliary;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;

/**
 * Identical to cc.unknown.utils.player.BlockUtils
 */
public final class BlockUtilsAux {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private BlockUtilsAux() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static CustomBlock getBlockAt(int x, int y, int z) {
        IBlockState state = getBlockState(new BlockPos(x, y, z));
        if (state == null) {
            return new CustomBlock(Blocks.air, new BlockPos(x, y, z));
        }
        return new CustomBlock(state, new BlockPos(x, y, z));
    }

    public static IBlockState getBlockState(BlockPos blockPos) {
        if (mc.theWorld == null) return null;
        return mc.theWorld.getBlockState(blockPos);
    }

    public static class CustomBlock {
        public String name;

        public CustomBlock(Block block, BlockPos blockPos) {
            this.name = Block.blockRegistry.getNameForObject(block).toString().replace("minecraft:", "");
        }

        public CustomBlock(IBlockState state, BlockPos blockPos) {
            Block block = state.getBlock();
            this.name = Block.blockRegistry.getNameForObject(block).toString().replace("minecraft:", "");
        }
    }
}
