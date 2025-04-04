package sereneseasons.handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.handler.SeasonTimeHandler;

public class SleepHandler {
    private static final int TICKS_PER_DAY = 24000;

    @SubscribeEvent
    public void onPlayerWakeUp(PlayerWakeUpEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        World world = player.world;
        if (world.isRemote) return;

        Season season = SeasonHelper.getSeasonState(world).getSeason();
        int dayDuration = SeasonTimeHandler.getDaytimeDuration(season);
        long currentTime = world.getWorldTime() % TICKS_PER_DAY;

        // 对齐到当前季节的日出时间
        if (currentTime > dayDuration) {
            long timeAdjustment = TICKS_PER_DAY - currentTime + (dayDuration - 1000);
            world.setWorldTime(world.getWorldTime() + timeAdjustment);
        }
    }
}