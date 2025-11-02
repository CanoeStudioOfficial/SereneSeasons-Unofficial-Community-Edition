/*******************************************************************************
 * Copyright 2016, the Biomes O' Plenty Team
 * 
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 * 
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/
package sereneseasons.asm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableMap;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

@IFMLLoadingPlugin.Name("SSLoadingPlugin")
public class SSLoadingPlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {

    private static final Map<String, Supplier<Boolean>> mixinConfigs = ImmutableMap.copyOf(new LinkedHashMap<String, Supplier<Boolean>>()
    {
        {
            put("mixins.sereneseasons.json", () -> true);
        }
    });

    @Override
    public List<String> getMixinConfigs() {
        return new ArrayList<>(mixinConfigs.keySet());
    }

    @Override
    public boolean shouldMixinConfigQueue(String mixinConfig) {
        Supplier<Boolean> sidedSupplier = mixinConfigs.get(mixinConfig);
        if (sidedSupplier != null) {
            return sidedSupplier.get();
        }
        return true;
    }
    @Override
    public String[] getASMTransformerClass()
    {
        return new String[] {};
    }

    @Override
    public String getModContainerClass()
    {
        return null;
    }

    @Override
    public String getSetupClass()
    {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) 
    {
    }

    @Override
    public String getAccessTransformerClass()
    {
        return null;
    }
}
