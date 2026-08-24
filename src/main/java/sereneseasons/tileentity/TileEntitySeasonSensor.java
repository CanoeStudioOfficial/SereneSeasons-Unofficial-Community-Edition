/*******************************************************************************
 * Copyright 2016, the Biomes O' Plenty Team
 * 
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 * 
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/
package sereneseasons.tileentity;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.block.BlockSeasonSensor;

public class TileEntitySeasonSensor extends TileEntity implements ITickable 
{
    // BUG FIX: Removed the @SubscribeEvent method.
    // TileEntity should NOT subscribe to global chunk events. This was an anti-pattern that:
    // 1. Never worked because the TE wasn't registered to the event bus
    // 2. Would cause massive performance issues if it did work (every TE checking every chunk load)
    
    @Override
    public void update()
    {
        // OPTIMIZATION: Only update every second (20 ticks) instead of checking every tick
        if (this.world != null && !this.world.isRemote)
        {
            long seasonTicks = SeasonHelper.getSeasonState(this.world).getSeasonCycleTicks();
            if (seasonTicks % 20L == 0L)
            {
                ((BlockSeasonSensor)this.getBlockType()).updatePower(this.world, this.pos);
            }
        }
    }
}