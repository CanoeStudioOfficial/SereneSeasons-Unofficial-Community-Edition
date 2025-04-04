package sereneseasons.handler;

import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.season.SeasonTime;

public class SeasonTimeHandler {
    private static final int TICKS_PER_DAY = 24000;
    private static final float[] SEASON_SPEED = {
            1.0F,  // SPRING
            0.8F,  // SUMMER (时间流逝更慢)
            1.0F,  // AUTUMN
            1.2F   // WINTER (时间流逝更快)
    };

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;

        World world = event.world;
        Season season = SeasonHelper.getSeasonState(world).getSeason();

        // 根据季节调整时间流速
        counter++;
        if (counter >= 5) {
            long originalTime = world.getWorldTime();
            long adjustedTime = originalTime + getTimeAdjustment(season);
            world.setWorldTime(adjustedTime);
            counter = 0;
        }
    }

    private int getTimeAdjustment(Season season) {
        switch (season) {
            case SUMMER: return -1; // 减缓时间流逝
            case WINTER: return 1;  // 加速时间流逝
            default: return 0;
        }
    }

    public static int getDaytimeDuration(Season season) {
        switch (season) {
            case SUMMER: return 16000; // 16小时白天
            case WINTER: return 8000;  // 8小时白天
            default: return 12000;      // 12小时白天
        }
    }
}
