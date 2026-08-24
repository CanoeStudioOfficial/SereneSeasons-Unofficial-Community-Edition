package sereneseasons.mixin.client;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sereneseasons.config.SeasonsConfig;
import sereneseasons.season.SeasonalCelestialAngle;

@Mixin(World.class)
public abstract class WorldMixin
{
    @Inject(
            method = "getSunBrightness",
            at = @At("HEAD"),
            cancellable = true
    )
    public void injectSeasonalSunBrightness(float partialTicks, CallbackInfoReturnable<Float> cir)
    {
        World world = (World) (Object) this;
        if (!SeasonsConfig.isDimensionWhitelisted(world.provider.getDimension())) return;

        cir.setReturnValue(SeasonalCelestialAngle.applySunBrightness(world, world.getSunBrightnessBody(partialTicks)));
    }

    @Inject(
            method = "getStarBrightness",
            at = @At("HEAD"),
            cancellable = true
    )
    public void injectSeasonalStarBrightness(float partialTicks, CallbackInfoReturnable<Float> cir)
    {
        World world = (World) (Object) this;
        if (!SeasonsConfig.isDimensionWhitelisted(world.provider.getDimension())) return;

        cir.setReturnValue(SeasonalCelestialAngle.applyStarBrightness(world, world.getStarBrightnessBody(partialTicks)));
    }
}