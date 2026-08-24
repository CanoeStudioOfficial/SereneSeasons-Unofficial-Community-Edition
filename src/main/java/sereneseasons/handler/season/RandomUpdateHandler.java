/*******************************************************************************
 * Copyright 2016, the Biomes O' Plenty Team
 *
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/
package sereneseasons.handler.season;

import net.minecraft.block.Block;
import net.minecraft.block.BlockIce;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.config.BiomeConfig;
import sereneseasons.config.SeasonsConfig;
import sereneseasons.init.ModConfig;
import sereneseasons.season.SeasonASMHelper;

import java.util.Iterator;

public class RandomUpdateHandler
{
	//Randomly melt ice and snow when it isn't winter
	@SubscribeEvent
	public void onWorldTick(TickEvent.WorldTickEvent event)
	{
		if (event.phase == Phase.END && event.side == Side.SERVER)
		{
			WorldServer world = (WorldServer) event.world;
			if (!SeasonsConfig.isDimensionWhitelisted(world.provider.getDimension()))
			{
				return;
			}

			Season.SubSeason subSeason = SeasonHelper.getSeasonState(world).getSubSeason();
			Season season = subSeason.getSeason();

			// OPTIMIZATION: Handle weather changes first, cleanly separated
			if (ModConfig.seasons.changeWeatherFrequency)
			{
				handleWeatherChanges(world, season);
			}

			if (season == Season.WINTER)
			{
				return;
			}

			// OPTIMIZATION: Calculate rand threshold once per tick
			int rand;
			switch (subSeason)
			{
				case EARLY_SPRING: rand = 16; break;
				case MID_SPRING:   rand = 12; break;
				case LATE_SPRING:  rand = 8;  break;
				default:           rand = 4;  break;
			}

			// OPTIMIZATION: Reusable BlockPos to prevent massive GC pressure
			BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

			for (Iterator<Chunk> iterator = world.getPersistentChunkIterable(world.getPlayerChunkMap().getChunkIterator()); iterator.hasNext();)
			{
				Chunk chunk = iterator.next();
				
				// BUG FIX: Using world.rand instead of manipulating world.updateLCG which breaks vanilla random ticks
				if (world.rand.nextInt(rand) != 0) continue;

				int x = chunk.x << 4;
				int z = chunk.z << 4;

				// Use standard random offset for chunk
				int randOffset = world.rand.nextInt(256);
				int localX = randOffset & 15;
				int localZ = (randOffset >> 4) & 15;

				mutablePos.setPos(x + localX, 0, z + localZ);
				BlockPos precipPos = world.getPrecipitationHeight(mutablePos);
				
				Biome biome = world.getBiome(precipPos);

				if(!BiomeConfig.enablesSeasonalEffects(biome))
					continue;

				boolean first = true;
				
				// BUG FIX: chunk.getBlockState(int x, int y, int z) expects LOCAL coordinates (0-15).
				// The original code passed absolute pos.getX(), which read the wrong block or crashed.
				// We now use world.getBlockState() with absolute coordinates.
				for (int y = precipPos.getY(); y >= 0; y--)
				{
					mutablePos.setY(y);
					Block block = world.getBlockState(mutablePos).getBlock();

					if (block == Blocks.SNOW_LAYER)
					{
						if (SeasonASMHelper.getFloatTemperature(world, biome, mutablePos) >= 0.15F)
						{
							world.setBlockToAir(mutablePos);
							break;
						}
					}

					if(!first)
					{
						if(block == Blocks.ICE)
						{
							if (SeasonASMHelper.getFloatTemperature(world, biome, mutablePos) >= 0.15F)
							{
								// BUG FIX: turnIntoWater requires absolute coordinates
								((BlockIce)Blocks.ICE).turnIntoWater(world, mutablePos);
								break;
							}
						}
					}
					else
						first = false;
				}
			}
		}
	}

	private void handleWeatherChanges(WorldServer world, Season season)
	{
		if (season == Season.WINTER)
		{
			if (world.getWorldInfo().isThundering())
			{
				world.getWorldInfo().setThundering(false);
			}
			if (!world.getWorldInfo().isRaining() && world.getWorldInfo().getRainTime() > 36000)
			{
				world.getWorldInfo().setRainTime(world.rand.nextInt(24000) + 12000);
			}
		}
		else if (season == Season.SPRING)
		{
			if (!world.getWorldInfo().isRaining() && world.getWorldInfo().getRainTime() > 96000)
			{
				world.getWorldInfo().setRainTime(world.rand.nextInt(84000) + 12000);
			}
		}
		else if (season == Season.SUMMER)
		{
			if (!world.getWorldInfo().isThundering() && world.getWorldInfo().getThunderTime() > 36000)
			{
				world.getWorldInfo().setThunderTime(world.rand.nextInt(24000) + 12000);
			}
		}
	}
}
