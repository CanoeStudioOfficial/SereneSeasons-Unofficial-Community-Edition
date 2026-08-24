package sereneseasons.init;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.config.BiomeConfig;
import sereneseasons.config.FertilityConfig;
import sereneseasons.config.SeasonsConfig;

import java.util.HashMap;
import java.util.HashSet;

public class ModFertility
{
	private static final HashSet<String> springPlants = new HashSet<>();
	private static final HashSet<String> summerPlants = new HashSet<>();
	private static final HashSet<String> autumnPlants = new HashSet<>();
	private static final HashSet<String> winterPlants = new HashSet<>();
	private static final HashSet<String> allListedPlants = new HashSet<>();

	private static final HashMap<String, Integer> seedSeasons = new HashMap<>();

	public static void init()
	{
		initSeasonCrops(FertilityConfig.seasonal_fertility.spring_crops, springPlants, 1);
		initSeasonCrops(FertilityConfig.seasonal_fertility.summer_crops, summerPlants, 2);
		initSeasonCrops(FertilityConfig.seasonal_fertility.autumn_crops, autumnPlants, 4);
		initSeasonCrops(FertilityConfig.seasonal_fertility.winter_crops, winterPlants, 8);
	}

	public static boolean isCropFertile(String cropName, World world, BlockPos pos)
	{
		Biome biome = world.getBiome(pos);

		if (BiomeConfig.disablesCrops(biome))
		{
			return false;
		}

		if (!FertilityConfig.general_category.seasonal_crops
				|| !SeasonsConfig.isDimensionWhitelisted(world.provider.getDimension())
				|| !BiomeConfig.enablesSeasonalEffects(biome))
		{
			return true;
		}

		if (BiomeConfig.usesTropicalSeasons(biome))
		{
			return summerPlants.contains(cropName) || !allListedPlants.contains(cropName);
		}
		else
		{
			float temp = biome.getTemperature(pos);
			if (temp < 0.15F)
			{
				return winterPlants.contains(cropName);
			}

			Season season = SeasonHelper.getSeasonState(world).getSeason();
			switch (season)
			{
				case SPRING: return springPlants.contains(cropName);
				case SUMMER: return summerPlants.contains(cropName);
				case AUTUMN: return autumnPlants.contains(cropName);
				case WINTER: return winterPlants.contains(cropName);
			}

			if (!allListedPlants.contains(cropName))
			{
				return season != Season.WINTER || FertilityConfig.general_category.ignore_unlisted_crops;
			}
		}

		return false;
	}

	private static void initSeasonCrops(String [] seeds, HashSet<String> cropSet, int bitmask)
	{
		for (String seed : seeds)
		{
			ResourceLocation rl = new ResourceLocation(seed);
			Item item = ForgeRegistries.ITEMS.getValue(rl);

			if (item instanceof IPlantable)
			{
				net.minecraft.block.state.IBlockState plantState = ((IPlantable) item).getPlant(null, null);
				if (plantState != null)
				{
					String plantName = plantState.getBlock().getRegistryName().toString();
					cropSet.add(plantName);

					if (bitmask != 0) allListedPlants.add(plantName);
					else continue;

					seedSeasons.merge(seed, bitmask, (oldVal, newVal) -> oldVal | newVal);
				}
			}
			else
			{
				Block block = ForgeRegistries.BLOCKS.getValue(rl);

				if (block != null && block != Blocks.AIR)
				{
					String plantName = block.getRegistryName().toString();
					cropSet.add(plantName);

					if (bitmask != 0) allListedPlants.add(plantName);
					else continue;

					seedSeasons.merge(seed, bitmask, (oldVal, newVal) -> oldVal | newVal);
				}
			}
		}
	}

	@SideOnly(Side.CLIENT)
	public static void setupTooltips(ItemTooltipEvent event)
	{
		if (FertilityConfig.general_category.crop_tooltips && FertilityConfig.general_category.seasonal_crops)
		{
			ResourceLocation rl = event.getItemStack().getItem().getRegistryName();
			if (rl == null) return;

			String name = rl.toString();
			Integer maskObj = seedSeasons.get(name);

			if (maskObj != null)
			{
				int mask = maskObj;
				event.getToolTip().add(I18n.format("tooltip.sereneseasons.fertile_season"));

				if ((mask & 15) == 15)
				{
					event.getToolTip().add(TextFormatting.LIGHT_PURPLE + I18n.format("tooltip.sereneseasons.season.all"));
				}
				else
				{
					if ((mask & 1) != 0) event.getToolTip().add(TextFormatting.GREEN + I18n.format("tooltip.sereneseasons.season.spring"));
					if ((mask & 2) != 0) event.getToolTip().add(TextFormatting.YELLOW + I18n.format("tooltip.sereneseasons.season.summer"));
					if ((mask & 4) != 0) event.getToolTip().add(TextFormatting.GOLD + I18n.format("tooltip.sereneseasons.season.autumn"));
					if ((mask & 8) != 0) event.getToolTip().add(TextFormatting.AQUA + I18n.format("tooltip.sereneseasons.season.winter"));
				}
			}
		}
	}
}
