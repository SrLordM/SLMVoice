package dev.srlordm.slmvoice.mixin.owo_ui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.srlordm.slmvoice.util.UtilKt;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.UIComponent;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Debug(export = true)
@Mixin(value = OwoUIGraphics.class, remap = false)
public class OwODrawContextMixin {
	@WrapOperation(method = "drawInspector", at = @At(value = "INVOKE", target = "Ljava/lang/Class;getSimpleName()Ljava/lang/String;"))
	private static String getAccurateName(Class<? extends UIComponent> cls, Operation<String> original) {
		return UtilKt.accurateName(cls);
	}
}
