package sereneseasons.handler;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import sereneseasons.api.config.SyncedConfig;
import sereneseasons.core.SereneSeasons;
import sereneseasons.network.message.MessageSyncConfigs;
import sereneseasons.network.message.MessageSyncSeasonCycle;

public class PacketHandler
{
    public static final SimpleNetworkWrapper instance = NetworkRegistry.INSTANCE.newSimpleChannel(SereneSeasons.MOD_ID);

    public static void init()
    {
        instance.registerMessage(MessageSyncSeasonCycle.class, MessageSyncSeasonCycle.class, 3, Side.CLIENT);
        instance.registerMessage(MessageSyncConfigs.class, MessageSyncConfigs.class, 4, Side.CLIENT);
    }

    public static void sendSyncedConfigs(EntityPlayerMP player)
    {
        NBTTagCompound options = new NBTTagCompound();
        for (java.util.Map.Entry<String, SyncedConfig.SyncedConfigEntry> option : SyncedConfig.optionsToSync.entrySet())
        {
            options.setString(option.getKey(), option.getValue().value);
        }

        instance.sendTo(new MessageSyncConfigs(options), player);
    }
}