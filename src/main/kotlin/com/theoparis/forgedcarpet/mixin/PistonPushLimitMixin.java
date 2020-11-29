package com.theoparis.forgedcarpet.mixin;

import com.theoparis.forgedcarpet.config.Config;
import net.minecraft.block.PistonBlockStructureHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = PistonBlockStructureHelper.class, priority = 420)  // piston push limit is important for carpet
public class PistonPushLimitMixin {
    @ModifyConstant(method = "addBlockLine", constant = @Constant(intValue = 12), expect = 3)
    private int pushLimit(int original) {
        return Config.CONFIG != null ? Config.CONFIG.getPistonPushLimit() : 12;
    }
}