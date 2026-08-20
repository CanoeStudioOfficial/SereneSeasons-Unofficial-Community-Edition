/*******************************************************************************
 * Copyright 2016, the Biomes O' Plenty Team
 * 
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 * 
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/
package sereneseasons.util;


import net.minecraft.client.Minecraft;
import net.minecraft.world.biome.Biome;
import sereneseasons.api.season.ISeasonColorProvider;
import sereneseasons.api.season.Season;
import sereneseasons.config.BiomeConfig;
import sereneseasons.config.SeasonsConfig;
import sereneseasons.init.ModConfig;

public class SeasonColourUtil 
{
    // OPTIMIZATION: Reusable array to prevent massive Garbage Collection (GC) pressure
    // from creating new float[] arrays for every single block rendered.
    private static final ThreadLocal<float[]> HSB_VALUES = ThreadLocal.withInitial(() -> new float[3]);

    // OPTIMIZATION: Replaced float division/multiplication with faster integer math.
    // (c1 * c2) / 255 is mathematically equivalent for 0-255 ranges but much faster.
    public static int multiplyColours(int colour1, int colour2)
    {
        return (colour1 * colour2) / 255;
    }

    public static int overlayBlendChannel(int underColour, int overColour)
    {
        int retVal;
        if (underColour < 128)
        {
            retVal = multiplyColours(2 * underColour, overColour);
        }
        else
        {
            retVal = multiplyColours(2 * (255 - underColour), 255 - overColour);
            retVal = 255 - retVal;
        }
        return retVal;
    }
    
    public static int overlayBlend(int underColour, int overColour)
    {
        int r = overlayBlendChannel((underColour >> 16) & 255, (overColour >> 16) & 255);
        int g = overlayBlendChannel((underColour >> 8) & 255, (overColour >> 8) & 255);
        int b = overlayBlendChannel(underColour & 255, overColour & 255);
        
        return (r & 255) << 16 | (g & 255) << 8 | (b & 255);
    }
    
    public static int saturateColour(int colour, float saturationMultiplier)
    {
        // OPTIMIZATION: Early exit if no saturation change is needed
        if (saturationMultiplier == 1.0F) return colour;

        int r = (colour >> 16) & 255;
        int g = (colour >> 8) & 255;
        int b = colour & 255;

        // OPTIMIZATION: Use java.awt.Color with reusable ThreadLocal array instead of org.lwjgl.util.Color
        // which creates new objects and arrays every time, causing micro-stutters.
        float[] hsb = HSB_VALUES.get();
        java.awt.Color.RGBtoHSB(r, g, b, hsb);
        
        hsb[1] *= saturationMultiplier;
        if (hsb[1] > 1.0F) hsb[1] = 1.0F;
        if (hsb[1] < 0.0F) hsb[1] = 0.0F;
        
        return java.awt.Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
    }
    
    public static int applySeasonalGrassColouring(ISeasonColorProvider colorProvider, Biome biome, int originalColour)
    {
        // BUG FIX: Prevent NullPointerException (NPE) when player is null (e.g., in main menu or loading screens)
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || !BiomeConfig.enablesSeasonalEffects(biome) || !SeasonsConfig.isDimensionWhitelisted(mc.player.dimension))
            return originalColour;

        int overlay = colorProvider.getGrassOverlay();
        float saturationMultiplier = colorProvider.getGrassSaturationMultiplier();
        if (!ModConfig.seasons.changeGrassColour)
    	{
        	overlay = Season.SubSeason.MID_SUMMER.getGrassOverlay();
            saturationMultiplier = Season.SubSeason.MID_SUMMER.getGrassSaturationMultiplier();
    	}
        // BUG FIX: Use bitmask to ignore alpha channel when checking for white overlay
        int newColour = ((overlay & 0xFFFFFF) == 0xFFFFFF) ? originalColour : overlayBlend(originalColour, overlay);
        return (saturationMultiplier != 1.0F && saturationMultiplier != -1.0F) ? saturateColour(newColour, saturationMultiplier) : newColour;
    }
    
    public static int applySeasonalFoliageColouring(ISeasonColorProvider colorProvider, Biome biome, int originalColour)
    {
        // BUG FIX: Prevent NullPointerException (NPE) when player is null
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || !BiomeConfig.enablesSeasonalEffects(biome) || !SeasonsConfig.isDimensionWhitelisted(mc.player.dimension))
            return originalColour;

        int overlay = colorProvider.getFoliageOverlay();
        float saturationMultiplier = colorProvider.getFoliageSaturationMultiplier();
        if (!ModConfig.seasons.changeFoliageColour)
    	{
        	overlay = Season.SubSeason.MID_SUMMER.getFoliageOverlay();
            saturationMultiplier = Season.SubSeason.MID_SUMMER.getFoliageSaturationMultiplier();
    	}
        // BUG FIX: Use bitmask to ignore alpha channel when checking for white overlay
        int newColour = ((overlay & 0xFFFFFF) == 0xFFFFFF) ? originalColour : overlayBlend(originalColour, overlay);
        return (saturationMultiplier != 1.0F && saturationMultiplier != -1.0F) ? saturateColour(newColour, saturationMultiplier) : newColour;
    }
}