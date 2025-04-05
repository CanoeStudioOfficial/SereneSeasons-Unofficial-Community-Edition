package sereneseasons.handler.season;

import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import sereneseasons.api.config.SeasonsOption;
import sereneseasons.api.config.SyncedConfig;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

public class DayNightHandler {
    public static final int BASE_DAY_LENGTH = 24000;
    private static final float[] SEASON_SPEED = new float[4];
    private static final int[] SUNRISE_TIMES = new int[4];
    private static final int[] SUNSET_TIMES = new int[4];

    static {
        // Spring: 正常速度
        SEASON_SPEED[Season.SPRING.ordinal()] = 1.0F;
        SUNRISE_TIMES[Season.SPRING.ordinal()] = 23000;
        SUNSET_TIMES[Season.SPRING.ordinal()] = 12000;

        // Summer: 白天延长25%
        SEASON_SPEED[Season.SUMMER.ordinal()] = 0.8F;
        SUNRISE_TIMES[Season.SUMMER.ordinal()] = 22000;
        SUNSET_TIMES[Season.SUMMER.ordinal()] = 13000;

        // Autumn: 正常速度
        SEASON_SPEED[Season.AUTUMN.ordinal()] = 1.0F;
        SUNRISE_TIMES[Season.AUTUMN.ordinal()] = 23000;
        SUNSET_TIMES[Season.AUTUMN.ordinal()] = 12000;

        // Winter: 白天缩短30%
        SEASON_SPEED[Season.WINTER.ordinal()] = 1.3F;
        SUNRISE_TIMES[Season.WINTER.ordinal()] = 20000;
        SUNSET_TIMES[Season.WINTER.ordinal()] = 14000;
    }

    private int speedCounter = 0;

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (!event.world.isRemote && event.phase == TickEvent.Phase.END) {
            Season currentSeason = SeasonHelper.getSeasonState(event.world).getSeason();
            int seasonIndex = currentSeason.ordinal();

            if (SyncedConfig.getBooleanValue(SeasonsOption.ADVANCE_SEASON_WHILE_SLEEPING)) {
                adjustDayNightCycle(event.world, seasonIndex);
            }
        }
    }

    private void adjustDayNightCycle(World world, int seasonIndex) {
        speedCounter++;
        float speedModifier = SEASON_SPEED[seasonIndex];
        int speedInterval = (int) (5 / speedModifier);

        if (speedCounter >= speedInterval) {
            long worldTime = world.getWorldTime();
            int dayTime = (int) (worldTime % 24000);

            // 跳过夜晚时段
            if (dayTime > SUNSET_TIMES[seasonIndex] && dayTime < SUNRISE_TIMES[seasonIndex]) {
                world.setWorldTime(worldTime + (long) (speedModifier * 40));
            }

            speedCounter = 0;
        }
    }

    public static int getSunriseTime(Season season) {
        return SUNRISE_TIMES[season.ordinal()];
    }

    public static int getSunsetTime(Season season) {
        return SUNSET_TIMES[season.ordinal()];
    }
    public static float[] getSeasonSpeed() {
        return SEASON_SPEED;
    }
}