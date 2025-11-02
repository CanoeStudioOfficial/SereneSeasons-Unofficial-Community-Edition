package sereneseasons.proxy;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import sereneseasons.handler.SnowRecalculationHandler;

public class CommonProxy
{
    public void registerRenderers() {}
    public void registerItemVariantModel(Item item, String name, int metadata) {}
    public void registerBlockSided(Block block) {}
    public void registerItemSided(Item item) {}

    public void init(){

    }

    public void registerEventListeners(){
        MinecraftForge.EVENT_BUS.register(new SnowRecalculationHandler());
    }

    public void registerPostEventListeners(){

    }
}