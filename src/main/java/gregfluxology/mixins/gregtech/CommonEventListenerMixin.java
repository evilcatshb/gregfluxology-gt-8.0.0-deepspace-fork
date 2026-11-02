package gregfluxology.mixins.gregtech;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.CommonEventListener;
import gregfluxology.cap.FEToEUProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects after GTCEu's registerBlockEntityCapabilities to attach FEToEUProvider to all block entities.
 * FEToEUProvider.getCapability() immediately returns empty for non-GT machines via MetaMachine.getMachine(),
 * so there is no overhead on unrelated block entities.
 */
@Mixin(value = CommonEventListener.class, remap = false)
public class CommonEventListenerMixin {

    @Inject(method = "registerBlockEntityCapabilities", at = @At("RETURN"))
    private static void attachFEToEUCapability(AttachCapabilitiesEvent<BlockEntity> event, CallbackInfo ci) {
        event.addCapability(GTCEu.id("eu_capability"), new FEToEUProvider(event.getObject()));
    }
}