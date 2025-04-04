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
    public void onPlayerSleep(PlayerSleepInBedEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        World world = player.world;

        if (!world.isRemote) {
            long currentTime = world.getWorldTime() % TICKS_PER_DAY;
            int dayDuration = SeasonTimeHandler.getDayDuration(SeasonHelper.getSeasonState(world).getSubSeason());
            int nightStart = dayDuration;

            // 只有在夜晚允许睡觉
            if (currentTime < nightStart) {
                event.setResult(Result.DENY);
                return;
            }

            // 夏季需要更晚才能睡觉
            if (SeasonHelper.getSeasonState(world).getSeason() == Season.SUMMER) {
                if (currentTime < 13000) {
                    event.setResult(Result.DENY);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerWakeUp(PlayerWakeUpEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        World world = player.world;

        if (!world.isRemote) {
            long currentTime = world.getWorldTime() % TICKS_PER_DAY;
            int dayDuration = SeasonTimeHandler.getDayDuration(SeasonHelper.getSeasonState(world).getSubSeason());

            // 调整起床时间为对应季节的日出时间
            if (currentTime > dayDuration) {
                world.setWorldTime(world.getWorldTime() + (TICKS_PER_DAY - currentTime) + dayDuration);
            }
        }
    }
}