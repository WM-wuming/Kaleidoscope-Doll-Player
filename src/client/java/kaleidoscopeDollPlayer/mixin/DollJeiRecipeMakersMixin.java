package kaleidoscopeDollPlayer.mixin;

import com.kaleidoscopedoll.compat.jei.DollFunctionRecipeMaker;
import com.kaleidoscopedoll.compat.jei.EntityDollRecipeMaker;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import kaleidoscopeDollPlayer.registry.ModItems;
import net.minecraft.world.item.Item;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(value = {DollFunctionRecipeMaker.class, EntityDollRecipeMaker.class}, remap = false)
public class DollJeiRecipeMakersMixin {
    @ModifyExpressionValue(method = "createRecipes", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
            target = "Lcom/kaleidoscopedoll/event/ModRegisterEvent;DOLL_ITEMS:Ljava/util/Set;"))
    private static Set<Item> kaleidoscopeDollPlayer$includePlayerDoll(Set<Item> dolls) {
        Set<Item> jeiDolls = new LinkedHashSet<>(dolls);
        jeiDolls.add(ModItems.PLAYER_DOLL);
        return jeiDolls;
    }
}
