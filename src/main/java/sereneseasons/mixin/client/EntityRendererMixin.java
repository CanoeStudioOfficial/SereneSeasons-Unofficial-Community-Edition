package sereneseasons.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import sereneseasons.config.SeasonsConfig;
import sereneseasons.season.SeasonASMHelper;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin implements IResourceManagerReloadListener {
    @Shadow @Final private Minecraft mc;

    @Redirect(
            method = "renderRainSnow",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/biome/Biome;canRain()Z"
            )
    )
    private boolean modifyCanRain(Biome instance) {
        if (!SeasonsConfig.isDimensionWhitelisted(this.mc.world.provider.getDimension())) return instance.canRain();
        return SeasonASMHelper.shouldRenderRainSnow(this.mc.world, instance);
    }

    @Redirect(
            method = "renderRainSnow",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/biome/Biome;getEnableSnow()Z"
            )
    )
    private boolean redirectGetEnableSnow(Biome biome) {
        if (!SeasonsConfig.isDimensionWhitelisted(this.mc.world.provider.getDimension())) return biome.getEnableSnow();
        return false;
    }

    @Redirect(
            method = "renderRainSnow",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/biome/Biome;getTemperature(Lnet/minecraft/util/math/BlockPos;)F"
            )
    )
    private float redirectGetTemperature(Biome instance, BlockPos blockPos) {
        if (!SeasonsConfig.isDimensionWhitelisted(this.mc.world.provider.getDimension())) return instance.getTemperature(blockPos);
        return SeasonASMHelper.getFloatTemperature(this.mc.world, instance, blockPos);
    }

    @Redirect(
            method = "addRainParticles",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/biome/Biome;canRain()Z"
            )
    )
    public boolean redirectParticlesCanRain(Biome instance) {
        if (!SeasonsConfig.isDimensionWhitelisted(this.mc.world.provider.getDimension())) return instance.canRain();
        return SeasonASMHelper.shouldAddRainParticles(this.mc.world, instance);
    }

    @Redirect(
            method = "addRainParticles",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/biome/Biome;getTemperature(Lnet/minecraft/util/math/BlockPos;)F"
            )
    )
    public float redirectParticlesTemperature(Biome instance, BlockPos blockPos) {
        if (!SeasonsConfig.isDimensionWhitelisted(this.mc.world.provider.getDimension())) return instance.getTemperature(blockPos);
        return SeasonASMHelper.getFloatTemperature(this.mc.world, instance, blockPos);
    }
}