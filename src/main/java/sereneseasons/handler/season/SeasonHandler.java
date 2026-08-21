/*******************************************************************************
 * Copyright 2016, the Biomes O' Plenty Team
 * 
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 * 
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/
package sereneseasons.handler.season;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import sereneseasons.api.config.SeasonsOption;
import sereneseasons.api.config.SyncedConfig;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.config.SeasonsConfig;
import sereneseasons.core.SereneSeasons;
import sereneseasons.handler.PacketHandler;
import sereneseasons.network.message.MessageSyncSeasonCycle;
import sereneseasons.season.SeasonASMHelper;
import sereneseasons.season.SeasonSavedData;
import sereneseasons.season.SeasonTime;

import java.util.HashMap;

public class SeasonHandler implements SeasonHelper.ISeasonDataProvider
{
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event)
    {
        World world = event.world;

        if (event.phase == TickEvent.Phase.END && !world.isRemote)
        {
            if (!SyncedConfig.getBooleanValue(SeasonsOption.PROGRESS_SEASON_WHILE_OFFLINE))
            {
                MinecraftServer server = world.getMinecraftServer();
                if (server != null && server.getPlayerList().getCurrentPlayerCount() == 0)
                    return;
            }
                
            SeasonSavedData savedData = getSeasonSavedData(world);

            // BUG FIX: Get cycle duration and validate it's positive
            int cycleDuration = SeasonTime.ZERO.getCycleDuration();
            if (cycleDuration <= 0)
            {
                SereneSeasons.logger.warn("Invalid cycle duration: " + cycleDuration + ". Skipping season tick.");
                return;
            }

            savedData.seasonCycleTicks++;
            
            if (savedData.seasonCycleTicks >= cycleDuration)
            {
                savedData.seasonCycleTicks = 0;
            }
            
            if (savedData.seasonCycleTicks % 20 == 0)
            {
                sendSeasonUpdate(world);
                // BUG FIX: Only mark dirty when sending updates (every 1 second) instead of every single tick
                // This prevents massive disk I/O lag and unnecessary world saves
                savedData.markDirty();
            }
        }
    }
    
    @SubscribeEvent
    public void onPlayerLogin(PlayerLoggedInEvent event)
    {
        EntityPlayer player = event.player;
        World world = player.world;

        if (!world.isRemote && player instanceof EntityPlayerMP)
        {
            PacketHandler.sendSyncedConfigs((EntityPlayerMP) player);
            sendSeasonUpdate(world);
        }
    }

    private Season.SubSeason lastSeason = null;
    public static final HashMap<Integer, Integer> clientSeasonCycleTicks = new HashMap<>();
    
    private static int getClientDimension()
    {
        return Minecraft.getMinecraft().player == null ? 0 : Minecraft.getMinecraft().player.dimension;
    }

    public static SeasonTime getClientSeasonTime() {
        Integer i = clientSeasonCycleTicks.get(getClientDimension());
        return new SeasonTime(i == null ? 0 : i);
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) 
    {
        //Only do this when in the world
        if (Minecraft.getMinecraft().player == null) return;
        
        int dimension = Minecraft.getMinecraft().player.dimension;

        if (event.phase == TickEvent.Phase.END && SeasonsConfig.isDimensionWhitelisted(dimension))
        {
            clientSeasonCycleTicks.compute(dimension, (k, v) -> v == null ? 0 : v + 1);
        	
            //Keep moving forward since they are only synchronized with the server every second
            int cycleDuration = SeasonTime.ZERO.getCycleDuration();
            if (cycleDuration > 0 && clientSeasonCycleTicks.get(dimension) >= cycleDuration)
            {
                clientSeasonCycleTicks.put(dimension, 0);
            }
            
            SeasonTime calendar = new SeasonTime(clientSeasonCycleTicks.get(dimension));
            
            if (calendar.getSubSeason() != lastSeason)
            {
                Minecraft.getMinecraft().renderGlobal.loadRenderers();
                lastSeason = calendar.getSubSeason();
            }
        }
    }

    @SubscribeEvent
    public void onPopulateChunk(PopulateChunkEvent.Populate event)
    {
        World world = event.getWorld();
        if (world.isRemote || event.getType() != PopulateChunkEvent.Populate.EventType.ICE || !SeasonsConfig.isDimensionWhitelisted(world.provider.getDimension()))
            return;

        event.setResult(Event.Result.DENY);
        
        // BUG FIX / OPTIMIZATION: Cache season state outside the loop to avoid 256 lookups per chunk
        ISeasonState seasonState = SeasonHelper.getSeasonState(world);
        
        int baseX = event.getChunkX() * 16 + 8;
        int baseZ = event.getChunkZ() * 16 + 8;
        
        // OPTIMIZATION: Reusable BlockPos to avoid massive GC pressure during chunk generation
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        
        for (int k2 = 0; k2 < 16; ++k2)
        {
            for (int j3 = 0; j3 < 16; ++j3)
            {
                mutablePos.setPos(baseX + k2, 0, baseZ + j3);
                BlockPos blockpos1 = world.getPrecipitationHeight(mutablePos);
                BlockPos blockpos2 = blockpos1.down();

                if (SeasonASMHelper.canBlockFreezeInSeason(world, blockpos2, false, seasonState, true))
                {
                    world.setBlockState(blockpos2, Blocks.ICE.getDefaultState(), 2);
                }

                if (SeasonASMHelper.canSnowAtInSeason(world, blockpos1, true, seasonState, true))
                {
                    world.setBlockState(blockpos1, Blocks.SNOW_LAYER.getDefaultState(), 2);
                }
            }
        }
    }
    
    public static void sendSeasonUpdate(World world)
    {
        if (!world.isRemote)
        {
            SeasonSavedData savedData = getSeasonSavedData(world);
            PacketHandler.instance.sendToAll(new MessageSyncSeasonCycle(world.provider.getDimension(), savedData.seasonCycleTicks));
        }
    }
    
    public static SeasonSavedData getSeasonSavedData(World world)
    {
        MapStorage mapStorage = world.getPerWorldStorage();
        SeasonSavedData savedData = (SeasonSavedData)mapStorage.getOrLoadData(SeasonSavedData.class, SeasonSavedData.DATA_IDENTIFIER);

        //If the saved data file hasn't been created before, create it
        if (savedData == null)
        {
            savedData = new SeasonSavedData(SeasonSavedData.DATA_IDENTIFIER);
            
            int startingSeason = SyncedConfig.getIntValue(SeasonsOption.STARTING_SUB_SEASON);
            
            if (startingSeason == 0)
            {
            	savedData.seasonCycleTicks = (world.rand.nextInt(12)) * SeasonTime.ZERO.getSubSeasonDuration();
            }
            if (startingSeason > 0)
            {
            	savedData.seasonCycleTicks = (startingSeason - 1) * SeasonTime.ZERO.getSubSeasonDuration();
            }
            
            mapStorage.setData(SeasonSavedData.DATA_IDENTIFIER, savedData);
            savedData.markDirty(); //Mark for saving
        }
        
        return savedData;
    }
    
    //
    // Used to implement getSeasonState in the API
    //
    
    public ISeasonState getServerSeasonState(World world)
    {
        SeasonSavedData savedData = getSeasonSavedData(world);
        return new SeasonTime(savedData.seasonCycleTicks);
    }
    
    public ISeasonState getClientSeasonState()
    {
        Integer i = clientSeasonCycleTicks.get(getClientDimension());
        return new SeasonTime(i == null ? 0 : i);
    }
}
