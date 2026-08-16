package sereneseasons.season;

import net.minecraft.world.World;
import sereneseasons.api.config.SeasonsOption;
import sereneseasons.api.config.SyncedConfig;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.config.SeasonsConfig;
import sereneseasons.init.ModConfig;

/**
 * Calculates the seasonal sun/moon angle without changing Minecraft's world time.
 *
 * <p>All four seasons use the sunrise/sunset windows used by the reference
 * Harvest Festival implementation.</p>
 */
public final class SeasonalCelestialAngle
{
    private static final int TICKS_PER_DAY = 24000;
    private static final float SUNRISE_ANGLE = 0.757F;
    private static final float MIDDAY_ANGLE = 1.0F;
    private static final float SUNSET_ANGLE = 0.2425F;
    private static final float MIDNIGHT_ANGLE = 0.5F;

    private SeasonalCelestialAngle()
    {
    }

    public static float calculate(World world, long worldTime, float partialTicks)
    {
        Season season = getSeason(world);
        return season == null
                ? calculateVanilla(worldTime, partialTicks)
                : calculate(season, worldTime, partialTicks);
    }

    /**
     * This overload is deliberately independent from a World so the angle
     * calculation can be checked without constructing a Minecraft world.
     */
    public static float calculate(Season season, long worldTime, float partialTicks)
    {
        if (season == null)
        {
            return calculateVanilla(worldTime, partialTicks);
        }

        int sunrise;
        int sunset;
        switch (season)
        {
            case SPRING:
                sunrise = 6000;
                sunset = 20500;
                break;
            case SUMMER:
                sunrise = 5000;
                sunset = 21500;
                break;
            case AUTUMN:
                sunrise = 7000;
                sunset = 19000;
                break;
            case WINTER:
                sunrise = 8000;
                sunset = 16500;
                break;
            default:
                return calculateVanilla(worldTime, partialTicks);
        }

        return calculateSeasonal(worldTime, partialTicks, sunrise, sunset);
    }

    public static boolean isEnabled(World world)
    {
        return world != null
                && ModConfig.seasons != null
                && SyncedConfig.optionsToSync.containsKey(SeasonsOption.CHANGE_DAYLIGHT_WITH_SEASONS.getOptionName())
                && SyncedConfig.getBooleanValue(SeasonsOption.CHANGE_DAYLIGHT_WITH_SEASONS)
                && SeasonsConfig.isDimensionWhitelisted(world.provider.getDimension());
    }

    public static float applySunBrightness(World world, float brightness)
    {
        return getSeason(world) == Season.SUMMER ? brightness * 1.25F : brightness;
    }

    public static float applyStarBrightness(World world, float brightness)
    {
        return getSeason(world) == Season.WINTER ? brightness * 1.25F : brightness;
    }

    private static Season getSeason(World world)
    {
        if (!isEnabled(world) || SeasonHelper.dataProvider == null)
        {
            return null;
        }

        ISeasonState seasonState = SeasonHelper.getSeasonState(world);
        return seasonState == null ? null : seasonState.getSeason();
    }

    private static float calculateSeasonal(long worldTime, float partialTicks, int sunrise, int sunset)
    {
        int midday = (sunrise + sunset) / 2;
        int midnight = (sunrise + sunset + TICKS_PER_DAY) / 2;

        // Minecraft's world time is six thousand ticks behind the clock used
        // by the reference implementation. Normalize the value so summer's
        // early sunrise also wraps correctly at the end of the Minecraft day.
        float time = positiveModulo(worldTime + 6000L - sunrise, TICKS_PER_DAY) + sunrise + partialTicks;

        if (time < midday)
        {
            return convertRange(sunrise, midday - 1, SUNRISE_ANGLE, MIDDAY_ANGLE, time);
        }
        else if (time < sunset)
        {
            return convertRange(midday, sunset - 1, 0.0F, SUNSET_ANGLE, time);
        }
        else if (time < midnight)
        {
            return convertRange(sunset, midnight - 1, SUNSET_ANGLE, MIDNIGHT_ANGLE, time);
        }

        return convertRange(midnight, sunrise + TICKS_PER_DAY, MIDNIGHT_ANGLE, SUNRISE_ANGLE, time);
    }

    private static float calculateVanilla(long worldTime, float partialTicks)
    {
        int timeOfDay = (int) positiveModulo(worldTime, TICKS_PER_DAY);
        float angle = ((float) timeOfDay + partialTicks) / TICKS_PER_DAY - 0.25F;

        if (angle < 0.0F)
        {
            ++angle;
        }

        if (angle > 1.0F)
        {
            --angle;
        }

        float smoothedAngle = 1.0F - (float) ((Math.cos((double) angle * Math.PI) + 1.0D) / 2.0D);
        return angle + (smoothedAngle - angle) / 3.0F;
    }

    private static float convertRange(float oldMin, float oldMax, float newMin, float newMax, float value)
    {
        float range = oldMax - oldMin;
        if (range == 0.0F)
        {
            return newMin;
        }

        return ((value - oldMin) * (newMax - newMin)) / range + newMin;
    }

    private static long positiveModulo(long value, long modulus)
    {
        long result = value % modulus;
        return result < 0L ? result + modulus : result;
    }
}
