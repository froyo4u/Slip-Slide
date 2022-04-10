package tk.meowmc.slippery.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import tk.meowmc.slippery.config.SlipperyConfig;

@Mixin(Block.class)
public abstract class BlockMixin extends AbstractBlock {
    public BlockMixin(Settings settings) {
        super(settings);
    }

    /**
     * @author me
     * @reason because
     */
    @Overwrite
    public float getSlipperiness() {
        return slipperiness + SlipperyConfig.get().slideValue;
    }
}
