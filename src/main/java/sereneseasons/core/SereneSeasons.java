package sereneseasons.core;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sereneseasons.Tags;
import sereneseasons.command.SSCommand;
import sereneseasons.init.*;
import sereneseasons.proxy.CommonProxy;

import java.io.File;

@Mod(modid = Tags.MOD_ID, version = Tags.VERSION, name = Tags.MOD_NAME, dependencies = "required-after:mixinbooter@[7.1,);")
public class SereneSeasons
{
    public static final String MOD_NAME = Tags.MOD_NAME;
    public static final String MOD_ID = Tags.MOD_ID;


    @Instance(MOD_ID)
    public static SereneSeasons instance;

    @SidedProxy(clientSide = "sereneseasons.proxy.ClientProxy", serverSide = "sereneseasons.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static Logger logger = LogManager.getLogger(MOD_ID);
    public static File configDirectory;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        configDirectory = new File(event.getModConfigurationDirectory(), "sereneseasons");

        ModConfig.preInit(configDirectory);
        ModBlocks.init();
        ModItems.init();
        ModHandlers.init();

        proxy.registerRenderers();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        proxy.registerEventListeners();
        logger.info(SereneSeasons.MOD_NAME + " is present, enjoy your winter wonderland!");
        ModConfig.init(configDirectory);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {

        proxy.registerPostEventListeners();
    	ModFertility.init();
    	ModHandlers.postInit();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new SSCommand());
    }
}
