/*******************************************************************************
 * Copyright 2016, the Biomes O' Plenty Team
 * 
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 * 
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/
package sereneseasons.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.config.SeasonsConfig;
import sereneseasons.season.SeasonTime;

import java.util.HashMap;
import java.util.Map;

public class ItemSeasonClock extends Item
{
    // BUG FIX: Store animation state per-world instead of globally.
    // The original code had all clocks share the same animation state, causing them to sync incorrectly.
    private final Map<Integer, ClockAnimationState> animationStates = new HashMap<>();

    public ItemSeasonClock()
    {
        this.addPropertyOverride(new ResourceLocation("time"), new IItemPropertyGetter()
        {
            @Override
            @SideOnly(Side.CLIENT)
            public float apply(ItemStack stack, World world, EntityLivingBase entity)
            {
                Entity holder = (Entity)(entity != null ? entity : stack.getItemFrame());

                if (world == null && holder != null)
                {
                    world = holder.world;
                }

                if (world == null)
                {
                    return 0.0F;
                }
                else
                {
                    double d0;
                    
                    if (SeasonsConfig.isDimensionWhitelisted(world.provider.getDimension()))
                    {
                        int seasonCycleTicks = SeasonHelper.getSeasonState(world).getSeasonCycleTicks();
                        d0 = (double)((float)seasonCycleTicks / (float) SeasonTime.ZERO.getCycleDuration());
                    }
                    else
                    {
                        d0 = Math.random();
                    }
                    
                    int dimension = world.provider.getDimension();
                    ClockAnimationState state = animationStates.computeIfAbsent(dimension, k -> new ClockAnimationState());
                    d0 = state.actualFrame(world, d0);
                    return MathHelper.positiveModulo((float)d0, 1.0F);
                }
            }
        });
    }

    @SideOnly(Side.CLIENT)
    private static class ClockAnimationState
    {
        private double field_185088_a;
        private double field_185089_b;
        private int ticks;

        private double actualFrame(World world, double frame)
        {
            if (world.getTotalWorldTime() != this.ticks)
            {
                this.ticks = (int)world.getTotalWorldTime();
                double newFrame = frame - this.field_185088_a;

                if (newFrame < -0.5D)
                {
                    ++newFrame;
                }

                this.field_185089_b += newFrame * 0.1D;
                this.field_185089_b *= 0.9D;
                this.field_185088_a += this.field_185089_b;
            }

            return this.field_185088_a;
        }
    }
}