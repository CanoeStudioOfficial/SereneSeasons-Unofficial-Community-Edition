package sereneseasons.init;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import sereneseasons.core.SereneSeasons;
import sereneseasons.item.ItemSeasonClock;
import sereneseasons.util.inventory.CreativeTabSS;

import static sereneseasons.api.SSItems.season_clock;
import static sereneseasons.api.SSItems.ss_icon;

public class ModItems
{
    public static void init()
    {
    	registerItems();
    }

    public static void registerItems()
    {

    	ss_icon = registerItem(new Item(), "ss_icon", null);


        season_clock = registerItem(new ItemSeasonClock(), "season_clock");
    }

    public static Item registerItem(Item item, String name)
    {
        return registerItem(item, name, CreativeTabSS.instance);
    }

    public static Item registerItem(Item item, String name, CreativeTabs tab)
    {
        item.setTranslationKey(name);


        if (tab != null)
        {
            item.setCreativeTab(tab);
        }

        item.setRegistryName(new ResourceLocation(SereneSeasons.MOD_ID, name));
        ForgeRegistries.ITEMS.register(item);
        SereneSeasons.proxy.registerItemSided(item);

        return item;
    }
}