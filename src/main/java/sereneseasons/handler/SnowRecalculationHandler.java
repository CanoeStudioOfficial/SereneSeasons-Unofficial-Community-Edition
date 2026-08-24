package sereneseasons.handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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
import java.util.HashMap;
import java.util.Map;

public class SnowRecalculationHandler {

    private static final Deque<Chunk> recalculationQueue = new ArrayDeque<>();

    private static final Map<String, Long> playerLastChunkPos = new HashMap<>();

    private static int recalculationCooldown = 0;
    private static final int RECALCULATION_INTERVAL = 200;

    @SubscribeEvent
    public void onTick(TickEvent.WorldTickEvent event) {
        if (event.type != Type.WORLD || event.side != Side.SERVER || event.phase != Phase.END) {
            return;
        }

        World world = event.world;
        if (world.isRemote || world.provider.getDimension() != 0) {
            return;
        }

        if (!FertilityConfig.general_category.shouldRecalculateSnow) {
            return;
        }

        recalculationCooldown--;
        if (recalculationCooldown <= 0) {
            recalculationCooldown = RECALCULATION_INTERVAL;
            checkPlayerMovement(world);
        }

        if (recalculationQueue.isEmpty()) {
            return;
        }

        int processed = 0;
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
                TimeStampsWorldSavedData.setChunkTimeStamp(chunk, currentTime);
            } else {
                recalculationQueue.offer(chunk);
            }
        }
    }

    private void checkPlayerMovement(World world) {
        int currentTime = (int) (System.currentTimeMillis() / 1000 / 60);

        for (EntityPlayer player : world.playerEntities) {
            int playerChunkX = MathHelper.floor(player.posX / 16.0D);
            int playerChunkZ = MathHelper.floor(player.posZ / 16.0D);
            long currentChunkKey = (((long) playerChunkX) << 32) | (playerChunkZ & 0xFFFFFFFFL);

            String playerName = player.getName();
            Long lastChunkKey = playerLastChunkPos.get(playerName);

            if (lastChunkKey == null || lastChunkKey != currentChunkKey) {
                playerLastChunkPos.put(playerName, currentChunkKey);

                scheduleChunksAroundPlayer(world, playerChunkX, playerChunkZ, currentTime);
            }
        }

        playerLastChunkPos.keySet().removeIf(name ->
            world.playerEntities.stream().noneMatch(p -> p.getName().equals(name))
        );
    }

    private void scheduleChunksAroundPlayer(World world, int centerChunkX, int centerChunkZ, int currentTime) {
        int radius = 5;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int chunkX = centerChunkX + x;
                int chunkZ = centerChunkZ + z;

                Chunk chunk = world.getChunkProvider().getLoadedChunk(chunkX, chunkZ);

                if (chunk != null && chunk.isLoaded() && chunk.isPopulated()) {
                    int savedTime = TimeStampsWorldSavedData.getChunkTimeStamp(chunk);

                    if (currentTime - savedTime > FertilityConfig.general_category.timeToRecalculateSnow) {
                        if (!recalculationQueue.contains(chunk)) {
                            recalculationQueue.offer(chunk);
                        }
                    }
                }
            }
        }
    }

    private boolean processChunk(World world, Chunk chunk, SubSeason subSeason) {
        boolean success = true;
        int baseX = chunk.x * 16;
        int baseZ = chunk.z * 16;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int k2 = 0; k2 < 16; ++k2) {
            for (int j3 = 0; j3 < 16; ++j3) {
                mutablePos.setPos(baseX + k2, 0, baseZ + j3);
                BlockPos blockpos1 = chunk.getPrecipitationHeight(mutablePos);
                BlockPos blockpos2 = blockpos1.down();

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
        int playerChunkX = MathHelper.floor(player.posX / 16.0D);
        int playerChunkZ = MathHelper.floor(player.posZ / 16.0D);

        scheduleChunksAroundPlayer(world, playerChunkX, playerChunkZ, currentTime);
    }

    @SubscribeEvent
    public void onChunkUnLoaded(ChunkEvent.Unload event) {
        World world = event.getWorld();
        if (world.isRemote || world.provider.getDimension() != 0) {
            return;
        }

        Chunk chunk = event.getChunk();
        removeFromRecalculationQueue(chunk);
    }

    @SubscribeEvent
    public void playerLeftWorld(PlayerEvent.PlayerLoggedOutEvent event) {
        EntityPlayer player = event.player;

        playerLastChunkPos.remove(player.getName());
    }

    private boolean shouldMelt(World world, BlockPos pos, SubSeason subSeason) {
        Biome biome = world.getBiome(pos);
        float f = BiomeHooks.getFloatTemperature(subSeason, biome, pos);
        return f >= 0.15F;
    }

    private boolean removeFromRecalculationQueue(Chunk chunk) {
        return recalculationQueue.remove(chunk);
    }
}
