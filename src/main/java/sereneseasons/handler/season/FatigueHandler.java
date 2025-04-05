package sereneseasons.handler.season;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FatigueHandler {
    public static final Potion FATIGUE_EFFECT = MobEffects.MINING_FATIGUE;

    @SubscribeEvent
    public void onSleepInterrupt(PlayerSleepInBedEvent event) {
        if (event.getResultStatus() != null) {
            applyFatigueEffect(event.getEntityPlayer());
        }
    }

    private void applyFatigueEffect(EntityPlayer player) {
        if (!player.world.isRemote) {
            player.addPotionEffect(new PotionEffect(FATIGUE_EFFECT, 6000, 1));
        }
    }
}