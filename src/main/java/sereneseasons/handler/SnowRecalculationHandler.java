package sereneseasons.handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.Type;
import net.minecraftforge.fml.relauncher.Side;
import sereneseasons.api.season.BiomeHooks;
import sereneseasons.api.season.Season.SubSeason;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.config.FertilityConfig;
import sereneseasons.data.TimeStampsWorldSavedData;

import java.util.ArrayDeque;
import java.util.Deque;

public class SnowRecalculationHandler {

    // OPTIMIZATION: Replaced ArrayList with ArrayDeque. 
    // ArrayList.remove(int) is O(N) and shifts elements, causing lag. ArrayDeque is O(1) for queues.
    private static final Deque<Chunk> recalculationQueue = new ArrayDeque<>();

    @SubscribeEvent
    public void onTick(TickEvent.WorldTickEvent event) {
        // OPTIMIZATION: Fail fast checks consolidated
        if (event.type != Type.WORLD || event.side != Side.SERVER || event.phase != Phase.END) {
            return;
        }
        
        World world = event.world;
        if (world.isRemote || world.provider.getDimension() != 0) {
            return;
        }
        
        if (recalculationQueue.isEmpty()) {
            return;
        }

        int processed = 0;
        // OPTIMIZATION: Cache SubSeason outside the loop. 
        // Originally, SeasonHelper.getSeasonState was called 256 times per chunk!
        SubSeason subSeason = SeasonHelper.getSeasonState(world).getSubSeason();
        int currentTime = (int) (System.currentTimeMillis() / 1000 / 60);

        while (!recalculationQueue.isEmpty() && processed < 20) {
            Chunk chunk = recalculationQueue.poll();
            
            if (!chunk.isLoaded() || !chunk.isPopulated()) {
                continue;
            }

            boolean success = processChunk(world, chunk, subSeason);
            
            if (success) {
                processed++;
                // BUG FIX: Update timestamp ONLY when successfully processed.
                TimeStampsWorldSavedData.setChunkTimeStamp(chunk, currentTime);
            } else {
                // If it failed, put it back at the end of the queue to retry later
                recalculationQueue.offer(chunk);
            }
        }
    }

    private boolean processChunk(World world, Chunk chunk, SubSeason subSeason) {
        boolean success = true;
        int baseX = chunk.x * 16;
        int baseZ = chunk.z * 16;
        
        // OPTIMIZATION: Reusable BlockPos to avoid massive GC (Garbage Collection) pressure.
        // Prevents creating 256+ BlockPos objects per chunk per tick.
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        
        for (int k2 = 0; k2 < 16; ++k2) {
            for (int j3 = 0; j3 < 16; ++j3) {
                mutablePos.setPos(baseX + k2, 0, baseZ + j3);
                BlockPos blockpos1 = chunk.getPrecipitationHeight(mutablePos);
                BlockPos blockpos2 = blockpos1.down();

                // BUG FIX: Use &= instead of && to prevent short-circuiting.
                // Original code skipped placing snow/ice for the rest of the chunk if ONE block failed.
                if (world.canBlockFreezeWater(blockpos2)) {
                    success &= world.setBlockState(blockpos2, Blocks.ICE.getDefaultState(), 2);
                }

                if (world.canSnowAt(blockpos1, true)) {
                    success &= world.setBlockState(blockpos1, Blocks.SNOW_LAYER.getDefaultState(), 2);
                }

                if (shouldMelt(world, blockpos2, subSeason)) {
                    if (world.getBlockState(blockpos2).getBlock() == Blocks.ICE) {
                        success &= world.setBlockState(blockpos2, Blocks.WATER.getDefaultState(), 2);
                    }
                    if (world.getBlockState(blockpos1).getBlock() == Blocks.SNOW_LAYER) {
                        success &= world.setBlockState(blockpos1, Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }
        return success;
    }

    @SubscribeEvent
    public void onChunkLoaded(ChunkEvent.Load event) {
        if (!FertilityConfig.general_category.shouldRecalculateSnow) {
            return;
        }
        World world = event.getWorld();
        if (world.isRemote || world.provider.getDimension() != 0) {
            return;
        }
        
        Chunk chunk = event.getChunk();
        int currentTime = (int) (System.currentTimeMillis() / 1000 / 60);
        int savedTime = TimeStampsWorldSavedData.getChunkTimeStamp(chunk);
        
        if (currentTime - savedTime > FertilityConfig.general_category.timeToRecalculateSnow) {
            recalculationQueue.offer(chunk);
        }
    }

    @SubscribeEvent
    public void playerJoinedWorld(PlayerEvent.PlayerLoggedInEvent event) {
        if(!FertilityConfig.general_category.shouldRecalculateSnow) {
            return;
        }
        EntityPlayer player = event.player;
        World world = player.world;
        if (world.isRemote || world.provider.getDimension() != 0) {
            return;
        }
        
        int currentTime = (int) (System.currentTimeMillis() / 1000 / 60);
        int playerChunkX = (int) player.posX / 16;
        int playerChunkZ = (int) player.posZ / 16;

        for (int i = -5; i < 5; i++) {
            for (int j = -5; j < 5; j++) {
                Chunk chunk = world.getChunk(playerChunkX + i, playerChunkZ + j);
                if (chunk != null) {
                    int savedTime = TimeStampsWorldSavedData.getChunkTimeStamp(chunk);
                    if (currentTime - savedTime > FertilityConfig.general_category.timeToRecalculateSnow) {
                        recalculationQueue.offer(chunk);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onChunkUnLoaded(ChunkEvent.Unload event) {
        World world = event.getWorld();
        if (world.isRemote || world.provider.getDimension() != 0) {
            return;
        }
        
        Chunk chunk = event.getChunk();
        removeFromRecalculationQueue(chunk);
        // BUG FIX: Removed incorrect timestamp update here. 
        // Timestamps should only be updated when recalculation actually succeeds, not just because a chunk unloaded.
    }

    @SubscribeEvent
    public void playerLeftWorld(PlayerEvent.PlayerLoggedOutEvent event) {
        EntityPlayer player = event.player;
        World world = player.world;
        if (world.isRemote || world.provider.getDimension() != 0) {
            return;
        }
        
        int playerChunkX = (int) player.posX / 16;
        int playerChunkZ = (int) player.posZ / 16;

        for (int i = -5; i < 5; i++) {
            for (int j = -5; j < 5; j++) {
                Chunk chunk = world.getChunk(playerChunkX + i, playerChunkZ + j);
                if (chunk != null) {
                    removeFromRecalculationQueue(chunk);
                }
            }
        }
    }

    private boolean shouldMelt(World world, BlockPos pos, SubSeason subSeason) {
        Biome biome = world.getBiome(pos);
        float f = BiomeHooks.getFloatTemperature(subSeason, biome, pos);
        return f >= 0.15F;
    }

    // OPTIMIZATION: Simplified removal using Deque's built-in remove(Object)
    private boolean removeFromRecalculationQueue(Chunk chunk) {
        return recalculationQueue.remove(chunk);
    }
}