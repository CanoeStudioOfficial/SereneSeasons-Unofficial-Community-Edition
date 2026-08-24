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
    
    public SeasonTime(int time)
    {
        Preconditions.checkArgument(time >= 0, "Time cannot be negative!");
        this.time = time;
    }

    @Override
    public int getDayDuration()
    {
        // BUG FIX: Removed caching. SyncedConfig values may not be ready during
        // class loading, causing permanently cached incorrect values (0).
        // Direct lookup is safe and fast enough for this use case.
        int value = SyncedConfig.getIntValue(SeasonsOption.DAY_DURATION);
        return value > 0 ? value : 24000; // Fallback to vanilla day length
    }

    @Override
    public int getSubSeasonDuration()
    {
        int dayDuration = getDayDuration();
        int subSeasonDays = SyncedConfig.getIntValue(SeasonsOption.SUB_SEASON_DURATION);
        return dayDuration * (subSeasonDays > 0 ? subSeasonDays : 7);
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
        int subSeasonDuration = getSubSeasonDuration();
        if (subSeasonDuration <= 0) return Season.SubSeason.VALUES[0];
        
        int index = (this.time / subSeasonDuration) % Season.SubSeason.VALUES.length;
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
        int subSeasonDuration = getSubSeasonDuration();
        if (subSeasonDuration <= 0) return Season.TropicalSeason.VALUES[0];
        
        int index = ((((this.time / subSeasonDuration) + 11) / 2) + 5) % Season.TropicalSeason.VALUES.length;
        return Season.TropicalSeason.VALUES[index];
    }
}
