package sereneseasons.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import sereneseasons.core.SereneSeasons;

import java.util.HashMap;
import java.util.Map;

public class TimeStampsWorldSavedData extends WorldSavedData {

    private final Map<Long, Integer> timeStampMap = new HashMap<>();

    private static final String DATA_NAME = SereneSeasons.MOD_ID + "_TimeStampData";

    public TimeStampsWorldSavedData() {
        super(DATA_NAME);
    }

    public TimeStampsWorldSavedData(String dataName) {
        super(dataName);
    }

    public static void setChunkTimeStamp(Chunk chunk, int timeStamp) {
        TimeStampsWorldSavedData data = get(chunk.getWorld());
        long key = ChunkPos.asLong(chunk.x, chunk.z);
        data.timeStampMap.put(key, timeStamp);

        data.markDirty();
    }

    public static int getChunkTimeStamp(Chunk chunk) {
        TimeStampsWorldSavedData data = get(chunk.getWorld());
        long key = ChunkPos.asLong(chunk.x, chunk.z);
        return data.timeStampMap.getOrDefault(key, 0);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        timeStampMap.clear();
        boolean migratedLegacyKey = false;
        for (String key : nbt.getKeySet()) {
            if (nbt.hasKey(key, 99)) {
                Long chunkKey = parseChunkKey(key);
                if (chunkKey != null) {
                    if (!isLongKey(key)) {
                        migratedLegacyKey = true;
                    }
                    timeStampMap.put(chunkKey, nbt.getInteger(key));
                }
            }
        }

        if (migratedLegacyKey) {
            markDirty();
        }
    }

    private static Long parseChunkKey(String key) {
        try {
            return Long.parseLong(key);
        } catch (NumberFormatException ignored) {
        }

        if (key.length() < 5 || key.charAt(0) != '[' || key.charAt(key.length() - 1) != ']') {
            return null;
        }

        String[] coordinates = key.substring(1, key.length() - 1).split(",");
        if (coordinates.length != 2) {
            return null;
        }

        try {
            int x = Integer.parseInt(coordinates[0].trim());
            int z = Integer.parseInt(coordinates[1].trim());
            return ChunkPos.asLong(x, z);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean isLongKey(String key) {
        try {
            Long.parseLong(key);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        for (Map.Entry<Long, Integer> entry : timeStampMap.entrySet()) {
            nbt.setInteger(entry.getKey().toString(), entry.getValue());
        }
        return nbt;
    }

    public static TimeStampsWorldSavedData get(World world) {
        MapStorage storage = world.getPerWorldStorage();
        TimeStampsWorldSavedData instance = (TimeStampsWorldSavedData) storage.getOrLoadData(TimeStampsWorldSavedData.class, DATA_NAME);

        if (instance == null) {
            instance = new TimeStampsWorldSavedData();
            storage.setData(DATA_NAME, instance);
        }
        return instance;
    }

    public static void clearChunkTimeStamp(Chunk chunk) {
        TimeStampsWorldSavedData data = get(chunk.getWorld());
        long key = ChunkPos.asLong(chunk.x, chunk.z);
        data.timeStampMap.remove(key);
        data.markDirty();
    }
}
