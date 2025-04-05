package sereneseasons.handler.season;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

public class SleepEventHandler {

    @SubscribeEvent
    public void onSleepCheck(SleepingTimeCheckEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        World world = player.world;
        Season season = SeasonHelper.getSeasonState(world).getSeason();

        int currentTime = (int) (world.getWorldTime() % 24000);
        int sunset = DayNightHandler.getSunsetTime(season);

        // 冬季允许更早睡觉
        if (season == Season.WINTER && currentTime > sunset - 2000) {

            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public void onWakeUp(PlayerWakeUpEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        World world = player.world;
        Season season = SeasonHelper.getSeasonState(world).getSeason();

        int sunrise = DayNightHandler.getSunriseTime(season);
        long currentTime = world.getWorldTime() % 24000;

        // 强制推进到日出时间
        if (currentTime < sunrise) {
            world.setWorldTime(world.getWorldTime() + (sunrise - currentTime));
        }

        // 清除玩家疲劳效果
        if (player.isPotionActive(FatigueHandler.FATIGUE_EFFECT)) {
            player.removePotionEffect(FatigueHandler.FATIGUE_EFFECT);
        }
    }
}