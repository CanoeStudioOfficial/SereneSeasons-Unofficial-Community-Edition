/*******************************************************************************
 * Copyright 2014-2017, the Biomes O' Plenty Team
 *
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/
package sereneseasons.init;

import net.minecraft.client.Minecraft;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import sereneseasons.api.season.ISeasonColorProvider;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.config.BiomeConfig;
import sereneseasons.handler.PacketHandler;
import sereneseasons.handler.season.*;
import sereneseasons.season.SeasonTime;
import sereneseasons.util.SeasonColourUtil;

public class ModHandlers
{
    private static final SeasonHandler SEASON_HANDLER = new SeasonHandler();

    public static void init()
    {
        PacketHandler.init();

        //Handlers for functionality related to seasons
        MinecraftForge.EVENT_BUS.register(SEASON_HANDLER);
        MinecraftForge.TERRAIN_GEN_BUS.register(SEASON_HANDLER);
        SeasonHelper.dataProvider = SEASON_HANDLER;
        
        MinecraftForge.EVENT_BUS.register(new RandomUpdateHandler());
        MinecraftForge.EVENT_BUS.register(new SeasonSleepHandler());
        MinecraftForge.EVENT_BUS.register(new SeasonalCropGrowthHandler());

        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
        {
            registerSeasonColourHandlers();
        }
    }

    @SideOnly(Side.CLIENT)
    private static BiomeColorHelper.ColorResolver originalGrassColorResolver;
    @SideOnly(Side.CLIENT)
    private static BiomeColorHelper.ColorResolver originalFoliageColorResolver;

    // OPTIMIZATION: Cache the current color provider to prevent creating new SeasonTime objects 
    // and doing math thousands of times per frame in the render thread.
    @SideOnly(Side.CLIENT)
    private static ISeasonColorProvider cachedColorProvider;
    @SideOnly(Side.CLIENT)
    private static int cachedClientTick = -1;
    @SideOnly(Side.CLIENT)
    private static int cachedClientDimension = Integer.MIN_VALUE;

    @SideOnly(Side.CLIENT)
    private static int getClientDimension()
    {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.player == null ? 0 : minecraft.player.dimension;
    }

    @SideOnly(Side.CLIENT)
    private static ISeasonColorProvider getCachedColorProvider()
    {
        int dimension = getClientDimension();
        int currentTick = SeasonHandler.clientSeasonCycleTicks.getOrDefault(dimension, 0);
        if (currentTick != cachedClientTick || dimension != cachedClientDimension)
        {
            cachedClientTick = currentTick;
            cachedClientDimension = dimension;
            SeasonTime calendar = new SeasonTime(currentTick);
            // We assume the biome is not tropical for the global cache, but the lambda will handle tropical biomes correctly
            // Actually, we need to cache both or just let the lambda do the fast lookup.
            // To keep it simple and fast, we just cache the standard sub-season.
            cachedColorProvider = calendar.getSubSeason();
        }
        return cachedColorProvider;
    }

    @SideOnly(Side.CLIENT)
    private static void registerSeasonColourHandlers()
    {
        originalGrassColorResolver = BiomeColorHelper.GRASS_COLOR;
        originalFoliageColorResolver = BiomeColorHelper.FOLIAGE_COLOR;

        BiomeColorHelper.GRASS_COLOR = (biome, blockPosition) ->
        {
            // OPTIMIZATION: Use cached provider for standard seasons, only calculate tropical if needed
            ISeasonColorProvider colorProvider = BiomeConfig.usesTropicalSeasons(biome) 
                    ? new SeasonTime(SeasonHandler.clientSeasonCycleTicks.getOrDefault(getClientDimension(), 0)).getTropicalSeason()
                    : getCachedColorProvider();
                    
            return SeasonColourUtil.applySeasonalGrassColouring(colorProvider, biome, originalGrassColorResolver.getColorAtPos(biome, blockPosition));
        };

        BiomeColorHelper.FOLIAGE_COLOR = (biome, blockPosition) ->
        {
            ISeasonColorProvider colorProvider = BiomeConfig.usesTropicalSeasons(biome) 
                    ? new SeasonTime(SeasonHandler.clientSeasonCycleTicks.getOrDefault(getClientDimension(), 0)).getTropicalSeason()
                    : getCachedColorProvider();
                    
            return SeasonColourUtil.applySeasonalFoliageColouring(colorProvider, biome, originalFoliageColorResolver.getColorAtPos(biome, blockPosition));
        };
    }
    
    public static void postInit()
    {
    	if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
        {
    		BirchColorHandler.init();
        }
    }
}
