/*******************************************************************************
 * Copyright 2016, the Biomes O' Plenty Team
 * 
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 * 
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/
package sereneseasons.season;

import com.google.common.base.Preconditions;
import sereneseasons.api.config.SeasonsOption;
import sereneseasons.api.config.SyncedConfig;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.Season;

public final class SeasonTime implements ISeasonState
{
    public static final SeasonTime ZERO = new SeasonTime(0);
    public final int time;
    
    // OPTIMIZATION: Cache config values to prevent repeated lookups
    private static int cachedDayDuration = -1;
    private static int cachedSubSeasonDuration = -1;
    
    public SeasonTime(int time)
    {
        Preconditions.checkArgument(time >= 0, "Time cannot be negative!");
        this.time = time;
    }

    @Override
    public int getDayDuration()
    {
        if (cachedDayDuration == -1)
        {
            cachedDayDuration = SyncedConfig.getIntValue(SeasonsOption.DAY_DURATION);
        }
        return cachedDayDuration;
    }

    @Override
    public int getSubSeasonDuration()
    {
        if (cachedSubSeasonDuration == -1)
        {
            cachedSubSeasonDuration = getDayDuration() * SyncedConfig.getIntValue(SeasonsOption.SUB_SEASON_DURATION);
        }
        return cachedSubSeasonDuration;
    }

    @Override
    public int getSeasonDuration()
    {
        return getSubSeasonDuration() * 3;
    }

    @Override
    public int getCycleDuration()
    {
        return getSubSeasonDuration() * Season.SubSeason.VALUES.length;
    }
    
    @Override
    public int getSeasonCycleTicks() 
    {
        return this.time;
    }

    @Override
    public int getDay()
    {
        return this.time / getDayDuration();
    }

    @Override
    public Season.SubSeason getSubSeason()
    {
        int index = (this.time / getSubSeasonDuration()) % Season.SubSeason.VALUES.length;
        return Season.SubSeason.VALUES[index];
    }

    @Override
    public Season getSeason()
    {
        return this.getSubSeason().getSeason();
    }

    @Override
    public Season.TropicalSeason getTropicalSeason()
    {
        int index = ((((this.time / getSubSeasonDuration()) + 11) / 2) + 5) % Season.TropicalSeason.VALUES.length;
        return Season.TropicalSeason.VALUES[index];
    }
    
    // Call this when config changes to refresh cached values
    public static void invalidateCache()
    {
        cachedDayDuration = -1;
        cachedSubSeasonDuration = -1;
    }
}