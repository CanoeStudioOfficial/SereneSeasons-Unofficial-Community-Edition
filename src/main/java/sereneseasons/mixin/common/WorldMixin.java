package sereneseasons.mixin.common;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.season.SeasonASMHelper;

@Mixin(World.class)
public abstract class WorldMixin {
    @Inject(
            method = "canSnowAt",
            at = @At("HEAD"),
            cancellable = true
    )
    public void rewriteCanSnowAt(BlockPos pos, boolean checkLight, CallbackInfoReturnable<Boolean> cir) {
        World world = (World) (Object) this;
        ISeasonState seasonState = SeasonHelper.getSeasonState(world);
        cir.setReturnValue(SeasonASMHelper.canSnowAtInSeason(world, pos, checkLight, seasonState));
    }

    @Inject(
            method = "canBlockFreeze",
            at = @At("HEAD"),
            cancellable = true
    )
    public void rewriteCanBlockFreeze(BlockPos pos, boolean noWaterAdj, CallbackInfoReturnable<Boolean> cir) {
        World world = (World) (Object) this;
        ISeasonState seasonState = SeasonHelper.getSeasonState(world);
        cir.setReturnValue(SeasonASMHelper.canBlockFreezeInSeason(world, pos, noWaterAdj, seasonState));
    }

    @Inject(
            method = "isRainingAt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getBiome(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/biome/Biome;"
            ),
            cancellable = true
    )
    public void injectIsRainingAt(BlockPos position, CallbackInfoReturnable<Boolean> cir) {
        World world = (World) (Object) this;
        ISeasonState seasonState = SeasonHelper.getSeasonState(world);
        cir.setReturnValue(SeasonASMHelper.isRainingAtInSeason(world, position, seasonState));
    }
}
