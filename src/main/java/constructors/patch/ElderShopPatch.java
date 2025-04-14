package constructors.patch;

import necesse.engine.modLoader.annotations.ModConstructorPatch;
import necesse.entity.mobs.friendly.human.ElderHumanMob;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import net.bytebuddy.asm.Advice;

@ModConstructorPatch(target = ElderHumanMob.class, arguments = {})
public class ElderShopPatch {
	
	@Advice.OnMethodExit
    static void onExit(@Advice.This ElderHumanMob th) {	      
		th.shop.addSellingItem("terraformer",  new SellingShopItem().setRandomPrice(3000, 5000));
		th.shop.addSellingItem("builder",  new SellingShopItem().setRandomPrice(3000, 5000));
    }
}
