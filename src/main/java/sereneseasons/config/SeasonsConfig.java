/*******************************************************************************
 * Copyright 2014-2017, the Biomes O' Plenty Team
 *
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/
package sereneseasons.config;

import sereneseasons.api.config.SeasonsOption;
import sereneseasons.core.SereneSeasons;
import sereneseasons.init.ModConfig;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class SeasonsConfig extends ConfigHandler
{
    public static final String TIME_SETTINGS = "Time Settings";
    public static final String WEATHER_SETTINGS = "Weather Settings";
    public static final String AESTHETIC_SETTINGS = "Aesthetic Settings";
    public static final String DIMENSION_SETTINGS = "Dimension Settings";

    public boolean generateSnow;
    public boolean generateIce;
    public boolean changeWeatherFrequency;
    
    public boolean changeGrassColour;
    public boolean changeFoliageColour;
    public boolean changeBirchColour;
    
    public String[] whitelistedDimensions;

    // OPTIMIZATION: Cache parsed dimension IDs to prevent massive GC pressure and CPU waste.
    // The original code parsed Strings to Integers EVERY TIME isDimensionWhitelisted was called (thousands of times per second).
    private static Set<Integer> whitelistedDimensionsCache = new HashSet<>();

    public SeasonsConfig(File configFile)
    {
        super(configFile, "Seasons Settings");
    }

    @Override
    protected void loadConfiguration()
    {
        try
        {
            addSyncedValue(SeasonsOption.DAY_DURATION, 24000, TIME_SETTINGS, "The duration of a Minecraft day in ticks", 20, Integer.MAX_VALUE);
            addSyncedValue(SeasonsOption.SUB_SEASON_DURATION, 7, TIME_SETTINGS, "The duration of a sub season in days", 1, Integer.MAX_VALUE);
            addSyncedValue(SeasonsOption.STARTING_SUB_SEASON, 5, TIME_SETTINGS, "The starting sub season for new worlds.  0 = Random, 1 - 3 = Early/Mid/Late Spring, 4 - 6 = Early/Mid/Late Summer, 7 - 9 = Early/Mid/Late Autumn, 10 - 12 = Early/Mid/Late Winter", 0, 12);
            addSyncedValue(SeasonsOption.PROGRESS_SEASON_WHILE_OFFLINE, true, TIME_SETTINGS, "If the season should progress on a server with no players online");
            addSyncedValue(SeasonsOption.ADVANCE_SEASON_WHILE_SLEEPING, true, TIME_SETTINGS, "If the season should progress if all players are sleeping");
            addSyncedValue(SeasonsOption.CHANGE_DAYLIGHT_WITH_SEASONS, true, TIME_SETTINGS, "If sunrise, sunset and seasonal sky brightness should change with the season");

            generateSnow = config.getBoolean("Generate Snow", WEATHER_SETTINGS, true, "Generate snow during the Winter season");
            generateIce = config.getBoolean("Generate Ice", WEATHER_SETTINGS, true, "Generate ice during the Winter season");
            changeWeatherFrequency = config.getBoolean("Change Weather Frequency", WEATHER_SETTINGS, true, "Change the frequency of rain/snow/storms based on the season");
            
            // Client-only. The server shouldn't get to decide these.
            changeGrassColour = config.getBoolean("Change Grass Colour Seasonally", AESTHETIC_SETTINGS, true, "Change the grass colour based on the current season");
            changeFoliageColour = config.getBoolean("Change Foliage Colour Seasonally", AESTHETIC_SETTINGS, true, "Change the foliage colour based on the current season");
            changeBirchColour = config.getBoolean("Change Birch Colour Seasonally", AESTHETIC_SETTINGS, true, "Change the birch colour based on the current season");
        
            whitelistedDimensions = config.getStringList("Whitelisted Dimensions", DIMENSION_SETTINGS, new String[] { "0" }, "Seasons will only apply to dimensons listed here");
            
            // OPTIMIZATION: Rebuild the cache whenever the config is loaded or reloaded
            whitelistedDimensionsCache.clear();
            for (String dimStr : whitelistedDimensions)
            {
                try 
                {
                    whitelistedDimensionsCache.add(Integer.parseInt(dimStr.trim()));
                } 
                catch (NumberFormatException e) 
                {
                    SereneSeasons.logger.error("Invalid dimension ID in Seasons Config whitelist: '{}'. Please use numeric IDs.", dimStr);
                }
            }
        }
        catch (Exception e)
        {
            SereneSeasons.logger.error("Serene Seasons has encountered a problem loading seasons.cfg", e);
        }
        finally
        {
            if (config.hasChanged()) config.save();
        }
    }
    
    // OPTIMIZATION: O(1) HashSet lookup instead of O(N) String parsing loop
    public static boolean isDimensionWhitelisted(int dimension)
    {
        return whitelistedDimensionsCache.contains(dimension);
    }
}