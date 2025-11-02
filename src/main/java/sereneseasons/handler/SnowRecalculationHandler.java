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

import java.util.ArrayList;


public class SnowRecalculationHandler {

    private static ArrayList<Chunk> recalculationQueue = new ArrayList<Chunk>();

    @SubscribeEvent
    public void onTick(TickEvent.WorldTickEvent event) {
        Type type = event.type;
        Side side = event.side;
        Phase phase = event.phase;
        World world = event.world;
        if (world.provider.getDimension() != 0) {
            return;
        }
        if (!(type == Type.WORLD && side == Side.SERVER)) {
            return;
        }
        if (world.isRemote) {
            return;
        }
        if (recalculationQueue.size() == 0) {
            return;
        }
        int i = 0;
        int c = 0;
        while (recalculationQueue.size() > i && c < 20) {
            Chunk chunk = recalculationQueue.get(i);
            if (chunk.isLoaded() && chunk.isPopulated()) {
                boolean success = true;
                BlockPos pos = new BlockPos(chunk.x*16, 0, chunk.z*16);
                for (int k2 = 0; k2 < 16; ++k2) {
                    for (int j3 = 0; j3 < 16; ++j3) {
                        BlockPos blockpos1 = chunk.getPrecipitationHeight(pos.add(k2, 0, j3));
                        BlockPos blockpos2 = blockpos1.down();

                        if (world.canBlockFreezeWater(blockpos2)) {
                            success = success && world.setBlockState(blockpos2, Blocks.ICE.getDefaultState(), 2);
                        }

                        if (world.canSnowAt(blockpos1, true)) {
                            success = success && world.setBlockState(blockpos1, Blocks.SNOW_LAYER.getDefaultState(), 2);
                        }

                        if (shouldMelt(world, blockpos2)) {
                            if (world.getBlockState(blockpos2).getBlock() == Blocks.ICE) {
                                success = success && world.setBlockState(blockpos2, Blocks.WATER.getDefaultState(), 2);
                            }
                            if (world.getBlockState(blockpos1).getBlock() == Blocks.SNOW_LAYER) {
                                success = success && world.setBlockState(blockpos1, Blocks.AIR.getDefaultState(), 2);
                            }
                        }
                    }
                }
                if (success) {
                    recalculationQueue.remove(i);
                    c++;
                } else {
                    i++;
                }
            } else {
                i++;
            }
        }

    }

    @SubscribeEvent
    public void onChunkLoaded(ChunkEvent.Load event) {
        if(!FertilityConfig.General.shouldRecalculateSnow) {
            return;
        }
        World world = event.getWorld();
        if (world.isRemote) {
            return;
        }
        Chunk chunk = event.getChunk();
        if (world.provider.getDimension() != 0) {
            return;
        }
        int currentTime = (int) (System.currentTimeMillis()/1000/60);
        int savedTime = TimeStampsWorldSavedData.getChunkTimeStamp(chunk);
        if (currentTime - savedTime > FertilityConfig.General.timeToRecalculateSnow) {
            recalculationQueue.add(chunk);
        }
    }

    @SubscribeEvent
    public void playerJoinedWorld(PlayerEvent.PlayerLoggedInEvent event) {
        if(!FertilityConfig.General.shouldRecalculateSnow) {
            return;
        }
        EntityPlayer player = event.player;
        World world = player.world;
        if (world.provider.getDimension() != 0) {
            return;
        }
        if (world.isRemote) {
            return;
        }
        for (int i = -5; i < 5; i++) {
            for (int j = -5; j < 5; j++) {
                Chunk chunk = world.getChunk(((int)player.posX/16) + i, ((int)player.posZ/16) + j);
                int currentTime = (int) (System.currentTimeMillis()/1000/60);
                int savedTime = TimeStampsWorldSavedData.getChunkTimeStamp(chunk);
                if (currentTime - savedTime > FertilityConfig.General.timeToRecalculateSnow) {
                    recalculationQueue.add(chunk);
                }
            }
        }
    }

    @SubscribeEvent
    public void onChunkUnLoaded(ChunkEvent.Unload event) {
        World world = event.getWorld();
        if (world.isRemote) {
            return;
        }
        Chunk chunk = event.getChunk();
        if (world.provider.getDimension() != 0) {
            return;
        }
        if(!removeFromRecalculationQueue(chunk)) {
            int currentTime = (int) (System.currentTimeMillis()/1000/60);
            TimeStampsWorldSavedData.setChunkTimeStamp(chunk, currentTime);
        }
    }

    @SubscribeEvent
    public void playerLeftWorld(PlayerEvent.PlayerLoggedOutEvent event) {
        EntityPlayer player = event.player;
        World world = player.world;
        if (world.provider.getDimension() != 0) {
            return;
        }
        if (world.isRemote) {
            return;
        }
        int currentTime = (int) (System.currentTimeMillis()/1000/60);
        for (int i = -5; i < 5; i++) {
            for (int j = -5; j < 5; j++) {
                Chunk chunk = world.getChunk(((int)player.posX/16) + i, ((int)player.posZ/16) + j);
                if(!removeFromRecalculationQueue(chunk)) {
                    TimeStampsWorldSavedData.setChunkTimeStamp(chunk, currentTime);
                }
            }
        }
    }

    private boolean shouldMelt(World world, BlockPos pos) {
        Biome biome = world.getBiome(pos);
        SubSeason subSeason = SeasonHelper.getSeasonState(world).getSubSeason();
        float f = BiomeHooks.getFloatTemperature(subSeason, biome, pos);

        if (f >= 0.15F)
        {
            return true;
        }
        return false;
    }

    private boolean removeFromRecalculationQueue(Chunk chunk) {
        for (int i = 0; i < recalculationQueue.size(); i++) {
            Chunk queueChunk = recalculationQueue.get(i);
            if (chunk.getPos().x == queueChunk.getPos().x && chunk.getPos().z == queueChunk.getPos().z) {
                recalculationQueue.remove(i);
                i--;
                return true;
            }
        }
        return false;
    };

}