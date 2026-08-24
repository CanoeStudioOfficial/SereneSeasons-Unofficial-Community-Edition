/*******************************************************************************
 * Copyright 2014-2017, the Biomes O' Plenty Team
 *
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/
package sereneseasons.util.config;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import sereneseasons.core.SereneSeasons;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class JsonUtil
{
    public static final Gson SERIALIZER = new GsonBuilder().setPrettyPrinting().create();

    public static <T> T getOrCreateConfigFile(File configDir, String configName, T defaults, Type type)
    {
        File configFile = new File(configDir, configName);


        if (!configFile.exists())
        {
            writeFile(configFile, defaults);
        }

        try
        {

            String jsonContent = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
            return SERIALIZER.fromJson(jsonContent, type);
        }
        catch (Exception e)
        {
            SereneSeasons.logger.error("Error parsing config from json: " + configFile.toString(), e);
        }

        return null;
    }

    protected static boolean writeFile(File outputFile, Object obj)
    {
        try
        {

            String jsonContent = SERIALIZER.toJson(obj);
            FileUtils.write(outputFile, jsonContent, StandardCharsets.UTF_8);
            return true;
        }
        catch (Exception e)
        {
            SereneSeasons.logger.error("Error writing config file " + outputFile.getAbsolutePath() + ": " + e.getMessage());
            return false;
        }
    }
}