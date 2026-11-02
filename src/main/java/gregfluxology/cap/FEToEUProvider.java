// Copyright (C) 2018 DBot
// MIT License - see LICENSE file
package gregfluxology.cap;

import com.gregtechceu.gtceu.api.capability.GTCapability;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.compat.FeCompat;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.common.pipelike.cable.EnergyNetHandler;
import gregfluxology.GFyConfig;
import gregfluxology.util.GFyUtility;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Attaches a FE-input wrapper to GT machine block entities, allowing Forge Energy sources
 * to push power INTO GT machines.
 *
 * IMPORTANT: We must NOT use CapabilityCompatProvider / getUpvalueCapability() here.
 * That calls the block entity's own getCapability(), which would re-enter CapabilityDispatcher
 * and hit GTCEu's EUToFEProvider, which calls getUpvalueCapability(ForgeCapabilities.ENERGY),
 * which re-enters us — infinite mutual recursion -> StackOverflowError.
 *
 * Instead we query the IEnergyContainer directly via MetaMachine.getCapability(), which
 * goes straight to the machine's trait list and never touches CapabilityDispatcher.
 */
public class FEToEUProvider implements ICapabilityProvider {

    private final BlockEntity blockEntity;

    public FEToEUProvider(BlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        if (capability != ForgeCapabilities.ENERGY) {
            return LazyOptional.empty();
        }

        // Get the MetaMachine - if this is not a GT machine BE, bail out immediately.
        MetaMachine machine = MetaMachine.getMachine(blockEntity.getLevel(), blockEntity.getBlockPos());
        if (machine == null) {
            return LazyOptional.empty();
        }

        // Query IEnergyContainer directly from the machine traits - bypasses CapabilityDispatcher
        // entirely and avoids the EUToFEProvider <-> FEToEUProvider infinite recursion.
        LazyOptional<IEnergyContainer> energyContainer =
                MetaMachine.getCapability(machine, GTCapability.CAPABILITY_ENERGY_CONTAINER, facing);

        return energyContainer.isPresent()
                ? ForgeCapabilities.ENERGY.orEmpty(capability,
                        LazyOptional.of(() -> new FEEnergyWrapper(energyContainer.resolve().get(), facing)))
                : LazyOptional.empty();
    }

    public static class FEEnergyWrapper implements IEnergyStorage {

        private final IEnergyContainer energyContainer;
        private final Direction facing;

        public FEEnergyWrapper(IEnergyContainer energyContainer, Direction facing) {
            this.energyContainer = energyContainer;
            this.facing = facing;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!canReceive()) return 0;

            if (maxReceive == 1 && simulate) {
                // Damn you mekanism - probe for whether any energy can be received
                return energyContainer.getEnergyCanBeInserted() > 0L ? 1 : 0;
            }

            long maxIn = maxReceive / FeCompat.ratio(true);
            long missing = energyContainer.getEnergyCanBeInserted();
            long voltage = energyContainer.getInputVoltage();
            maxIn = Math.min(missing, maxIn);
            long maxAmp = Math.min(energyContainer.getInputAmperage(), maxIn / voltage);

            if (GFyConfig.ignoreCableCapacity && energyContainer instanceof EnergyNetHandler) {
                // Ignore cable amperage cap (cables may burn - same behaviour as base CEu)
                maxIn = maxReceive / FeCompat.ratio(true);
                maxAmp = maxIn / voltage;
            }

            if (maxAmp < 1L) return 0;

            if (!simulate) {
                maxAmp = energyContainer.acceptEnergyFromNetwork(facing, voltage, maxAmp);
            }

            return GFyUtility.safeConvertEUToFE(maxAmp * voltage);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0; // EU->FE extraction is handled by GTCEu's own EUToFEProvider
        }

        @Override
        public int getEnergyStored() {
            return GFyUtility.safeConvertEUToFE(energyContainer.getEnergyStored());
        }

        @Override
        public int getMaxEnergyStored() {
            return GFyUtility.safeConvertEUToFE(energyContainer.getEnergyCapacity());
        }

        @Override
        public boolean canExtract() {
            return false; // EU->FE extraction handled by GTCEu
        }

        @Override
        public boolean canReceive() {
            return energyContainer.inputsEnergy(this.facing);
        }
    }
}