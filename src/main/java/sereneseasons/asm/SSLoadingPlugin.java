/*******************************************************************************
 * Copyright 2016, the Biomes O' Plenty Team
 *
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 *
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/
package sereneseasons.asm;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.Name("SSLoadingPlugin")
public class SSLoadingPlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {



    private static final List<String> MIXIN_CONFIGS = Collections.singletonList("mixins.sereneseasons.json");

    @Override
    public List<String> getMixinConfigs() {
        return MIXIN_CONFIGS;
    }

    @Override
    public boolean shouldMixinConfigQueue(String mixinConfig) {

        return true;
    }

    @Override
    public String[] getASMTransformerClass() {


        return new String[0];
    }

    @Override
    public String getModContainerClass() { return null; }

    @Override
    public String getSetupClass() { return null; }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() { return null; }
}