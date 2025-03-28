package voidBucket.patch;

import java.util.ArrayList;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.friendly.human.ElderHumanMob;
import necesse.level.maps.levelData.villageShops.ShopItem;
import necesse.level.maps.levelData.villageShops.VillageShopsData;
import net.bytebuddy.asm.Advice;

@ModMethodPatch(target = ElderHumanMob.class, name = "getShopItems", arguments = {VillageShopsData.class, ServerClient.class})
public class ElderShopPatch {
	
	@Advice.OnMethodExit
    static void onExit(@Advice.This ElderHumanMob th, 
            @Advice.Argument(0) VillageShopsData data, 
            @Advice.Argument(1) ServerClient client,  // Fixed argument index
            @Advice.Return(readOnly = false) ArrayList<ShopItem> out) {	      
		
		out.add(ShopItem.item("terraformer", 2500));		
    }
}
