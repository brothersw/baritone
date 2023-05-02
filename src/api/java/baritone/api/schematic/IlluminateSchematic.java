/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.api.schematic;

import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockOptionalMeta;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
public class IlluminateSchematic extends AbstractSchematic {
    private static final BlockOptionalMeta bom = new BlockOptionalMeta(Blocks.TORCH);
    private final BetterBlockPos pos1;
    private final BetterBlockPos pos2;
    private final ClientLevel level;
    private final boolean[][][] ignoreCache;

    public IlluminateSchematic(BetterBlockPos pos1, BetterBlockPos pos2) {
        super(Math.abs(pos1.getX()-pos2.getX()) + 1,
                Math.abs(pos1.getY()-pos2.getY()) + 1,
                Math.abs(pos1.getZ()-pos2.getZ()) + 1);

        this.pos1 = pos1;
        this.pos2 = pos2;

        ignoreCache = new boolean[widthX()][heightY()][lengthZ()];
        level = Minecraft.getInstance().level;
        //TODO: air blocks -> just spawnable spaces (i.e. tall grass)
        //TODO: warn for tall selections to do layer by layer if setting isn't set
        //TODO: predictive torch placement, not just all valid places to put a torch
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockState desiredState(int offsetX, int offsetY, int offsetZ, BlockState current, List<BlockState> approxPlaceable) {
        if(ignoreCache[offsetX][offsetY][offsetZ])
            return current;

        if(shouldTorch(offsetX, offsetY, offsetZ) && !bom.matches(current)) {
            for (BlockState state : approxPlaceable) {
                if (bom.matches(state)) {
                    return state;
                }
            }
        }
        return current;
    }

    private boolean shouldTorch(int offsetX, int offsetY, int offsetZ) {
        BetterBlockPos pos = getWorldPos(offsetX, offsetY, offsetZ);
        BlockState state = level.getBlockState(pos);
        BlockState stateDown = level.getBlockState(pos.below());

        if(!isAir(state) ||
                !stateDown.canOcclude() ||
                !stateDown.isValidSpawn(level, pos.below(), EntityType.CREEPER)) {
            ignoreCache[offsetX][offsetY][offsetZ] = true;
            return false;
        }

        int illumination = level.getLightEngine().getLayerListener(LightLayer.BLOCK).getLightValue(pos);
        if(!(illumination == 0 || illumination == 14)) { //it doesn't detect torch placements fast enough to cache ignoring the torch
            ignoreCache[offsetX][offsetY][offsetZ] = true;
            return false;
        }
        return illumination == 0;
    }

    private BetterBlockPos getWorldPos(int offsetX, int offsetY, int offsetZ) {
        int x = pos1.getX() < pos2.getX() ? pos1.getX() + offsetX: pos2.getX() + offsetX;
        int y = pos1.getY() < pos2.getY() ? pos1.getY() + offsetY: pos2.getY() + offsetY;
        int z = pos1.getZ() < pos2.getZ() ? pos1.getZ() + offsetZ: pos2.getZ() + offsetZ;

        return new BetterBlockPos(x, y, z);
    }

    private boolean isAir(BlockState state){
        return state.is(Blocks.AIR) || state.is(Blocks.CAVE_AIR);
    }

}