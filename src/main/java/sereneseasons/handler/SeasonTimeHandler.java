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
        if (event.phase == TickEvent.Phase.END && !event.world.isRemote) {
            World world = event.world;
            Season.SubSeason subSeason = SeasonHelper.getSeasonState(world).getSubSeason();
            Season season = subSeason.getSeason();

            long worldTime = world.getWorldTime();
            long adjustedTime = calculateAdjustedTime(worldTime, season);

            // 保持时间在合理范围内
            if (adjustedTime < 0) adjustedTime += TICKS_PER_DAY;
            if (adjustedTime >= TICKS_PER_DAY) adjustedTime -= TICKS_PER_DAY;

            world.setWorldTime(adjustedTime);
        }
    }

    private long calculateAdjustedTime(long originalTime, Season season) {
        float speedFactor = SEASON_SPEED[season.ordinal()];
        long baseTime = originalTime % TICKS_PER_DAY;

        // 根据季节调整时间流逝
        if (season == Season.SUMMER) {
            return (long) (originalTime - (1 - speedFactor));
        } else if (season == Season.WINTER) {
            return (long) (originalTime + (speedFactor - 1));
        }
        return originalTime;
    }

    public static int getDayDuration(Season.SubSeason subSeason) {
        Season season = subSeason.getSeason();
        switch (season) {
            case SUMMER: return 16000;  // 夏季白天更长
            case WINTER: return 8000;   // 冬季白天更短
            default: return 12000;      // 春秋正常
        }
    }
}