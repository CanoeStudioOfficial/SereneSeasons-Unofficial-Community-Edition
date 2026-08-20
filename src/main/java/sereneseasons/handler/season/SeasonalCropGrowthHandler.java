package sereneseasons.handler.season;

import net.minecraft.block.Block;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockReed;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import sereneseasons.api.SSBlocks;
import sereneseasons.config.FertilityConfig;
import sereneseasons.init.ModFertility;

public class SeasonalCropGrowthHandler
{
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onItemTooltipAdded(ItemTooltipEvent event)
	{
		ModFertility.setupTooltips(event);
	}

	@SubscribeEvent
	public void onCropGrowth(BlockEvent.CropGrowEvent event)
	{
		Block plant = event.getState().getBlock();
		boolean isFertile = ModFertility.isCropFertile(plant.getRegistryName().toString(), event.getWorld(), event.getPos());
		
		if (FertilityConfig.general_category.seasonal_crops && !isFertile && !isGreenhouseGlassAboveBlock(event.getWorld(), event.getPos()))
		{
			if (FertilityConfig.general_category.crops_break && !(plant instanceof BlockGrass) && !(plant instanceof BlockReed))
			{
				event.getWorld().destroyBlock(event.getPos(), true);
			}
			else
			{
				event.setResult(Event.Result.DENY);
			}
		}
	}

	@SubscribeEvent
	public void onApplyBonemeal(BonemealEvent event)
	{
		Block plant = event.getBlock().getBlock();
		boolean isFertile = ModFertility.isCropFertile(plant.getRegistryName().toString(), event.getWorld(), event.getPos());
		
		if (FertilityConfig.general_category.seasonal_crops && !isFertile && !isGreenhouseGlassAboveBlock(event.getWorld(), event.getPos()))
		{
			if (FertilityConfig.general_category.crops_break && !(plant instanceof BlockGrass) && !(plant instanceof BlockReed))
			{
				event.getWorld().destroyBlock(event.getPos(), true);
			}
			
			event.setCanceled(true);
		}
	}

	private boolean isGreenhouseGlassAboveBlock(World world, BlockPos cropPos)
	{
		int maxHeight = FertilityConfig.general_category.greenhouse_glass_max_height;
		int maxY = cropPos.getY() + maxHeight;
		
		// OPTIMIZATION: Reusable BlockPos to prevent massive GC pressure during crop growth
		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(cropPos);
		
		for (int y = cropPos.getY() + 1; y <= maxY; y++)
		{
			mutablePos.setY(y);
			// OPTIMIZATION: Blocks are singletons, using == is faster and safer than .equals()
			if (world.getBlockState(mutablePos).getBlock() == SSBlocks.greenhouse_glass)
			{
				return true;
			}
		}
		
		return false;
	}
}