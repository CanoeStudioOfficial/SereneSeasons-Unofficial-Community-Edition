package sereneseasons.proxy;

import com.google.common.base.Preconditions;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import sereneseasons.api.ISSBlock;
import sereneseasons.core.SereneSeasons;
import sereneseasons.util.inventory.CreativeTabSS;

public class ClientProxy extends CommonProxy
{
    @Override
    public void registerRenderers()
    {
    }

    @Override
    public void registerItemVariantModel(Item item, String name, int metadata)
    {
        Preconditions.checkNotNull(item, "Cannot register models for null item " + name);
        Preconditions.checkArgument(item != Items.AIR, "Cannot register models for air (" + name + ")");

        ModelLoader.registerItemVariants(item, new ResourceLocation("sereneseasons:" + name));
        ModelLoader.setCustomModelResourceLocation(item, metadata, new ModelResourceLocation(SereneSeasons.MOD_ID + ":" + name, "inventory"));
    }

    @Override
    public void registerBlockSided(Block block)
    {
        if (block instanceof ISSBlock)
        {
            ISSBlock bopBlock = (ISSBlock) block;

            //Register non-rendering properties
            IProperty[] nonRenderingProperties = bopBlock.getNonRenderingProperties();

            if (nonRenderingProperties != null)
            {
                // use a custom state mapper which will ignore the properties specified in the block as being non-rendering
                IStateMapper custom_mapper = (new StateMap.Builder()).ignore(nonRenderingProperties).build();
                ModelLoader.setCustomStateMapper(block, custom_mapper);
            }
        }
    }

    @Override
    public void registerItemSided(Item item)
    {
        // register sub types if there are any
        if (item.getHasSubtypes())
        {
            NonNullList<ItemStack> subItems = NonNullList.create();
            item.getSubItems(CreativeTabSS.instance, subItems);
            for (ItemStack subItem : subItems)
            {
                String subItemName = item.getTranslationKey(subItem);
                subItemName =  subItemName.substring(subItemName.indexOf(".") + 1); // remove 'item.' from the front

                ModelLoader.registerItemVariants(item, new ResourceLocation(SereneSeasons.MOD_ID, subItemName));
                ModelLoader.setCustomModelResourceLocation(item, subItem.getMetadata(), new ModelResourceLocation(SereneSeasons.MOD_ID + ":" + subItemName, "inventory"));
            }
        }
        else
        {
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(SereneSeasons.MOD_ID + ":" + item.delegate.name().getPath(), "inventory"));
        }
    }

    @Override
    public void registerEventListeners() {
        super.registerEventListeners();
    }

    @Override
    public void registerPostEventListeners() {
        super.registerPostEventListeners();
    }
}