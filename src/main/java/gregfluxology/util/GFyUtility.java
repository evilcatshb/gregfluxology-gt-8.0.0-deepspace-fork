package gregfluxology.util;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.capability.compat.FeCompat;
import com.gregtechceu.gtceu.common.block.CableBlock;
import net.minecraft.world.level.block.Block;

public class GFyUtility {

    /**
     * Safely cast a Long to an Int without overflow.
     *
     * @param v The Long value to cast to an Int.
     * @return v, cast to Int, or Integer.MAX_VALUE if it would overflow.
     */
    public static int safeCastLongToInt(long v) {
        return v > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) v;
    }

    public static int safeConvertEUToFE(long eu) {
        return safeCastLongToInt(eu * FeCompat.ratio(false));
    }

    public static boolean isGTMachine(Block block) {
        return block instanceof MetaMachineBlock || block instanceof CableBlock;
    }
}
