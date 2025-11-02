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
import java.util.Set;

public class TimeStampsWorldSavedData extends WorldSavedData{

    private static Map<String, Integer> timeStampMap = new HashMap<>();

    public static void setChunkTimeStamp(Chunk chunk, int timeStamp) {
        if (timeStampMap.isEmpty()) {
            get(chunk.getWorld());
        }
        ChunkPos chunkPos = chunk.getPos();
        String key = chunkPos.toString();
        timeStampMap.put(key, timeStamp);
    }

    public static int getChunkTimeStamp(Chunk chunk) {
        if (timeStampMap.isEmpty()) {
            get(chunk.getWorld());
        }
        ChunkPos chunkPos = chunk.getPos();
        String key = chunkPos.toString();
        if (!timeStampMap.containsKey(key)) {
            return 0;
        }
        return timeStampMap.get(key);
    }

    private static final String DATA_NAME = SereneSeasons.MOD_ID + "_TimeStampData";

    public TimeStampsWorldSavedData() {
        super(DATA_NAME);
    }

    public TimeStampsWorldSavedData(String dataName) {
        super(dataName);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        Set<String> keys = nbt.getKeySet();
        for (String key : keys) {
            int value = nbt.getInteger(key);
            timeStampMap.put(key, value);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        Set<String> keys = timeStampMap.keySet();
        for (String key : keys) {
            int value = timeStampMap.get(key);
            nbt.setInteger(key, value);
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

}
