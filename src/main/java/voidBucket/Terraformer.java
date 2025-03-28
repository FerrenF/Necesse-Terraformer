package voidBucket;

import java.util.Collection;

import necesse.engine.input.Control;
import necesse.engine.input.InputID;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modLoader.annotations.ModEntry;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.res.ResourceEncoder;
import voidBucket.container.TerraformerContainer;
import voidBucket.form.TerraformerContainerForm;
import voidBucket.item.TerraformerItem;

@ModEntry
public class Terraformer {
		
	public static int TERRAFORMER_CONTAINER;
	public static int TERRAFORMER_ITEM;
    public void init() {
    	
    	TERRAFORMER_CONTAINER = ContainerRegistry.registerContainer((client, uniqueSeed, content) -> {
        	 
			return new TerraformerContainerForm<TerraformerContainer>(client,					
					new TerraformerContainer(client.getClient(), uniqueSeed, content));	
		}, (client, uniqueSeed, content, serverObject) -> {
			return new TerraformerContainer(client, uniqueSeed, content);
		});
         
    	TERRAFORMER_ITEM = ItemRegistry.registerItem("terraformer", new TerraformerItem(), 5000, true);
    	
    	Control.addModControl(new Control(InputID.KEY_PERIOD, "terraformersizeup", new necesse.engine.localization.message.LocalMessage("terraformer", "terraformercontrolsizeup")));
    	Control.addModControl(new Control(InputID.KEY_COMMA,  "terraformersizedown", new necesse.engine.localization.message.LocalMessage("terraformer", "terraformercontrolsizedown")));

    }
    public void initResources() {
    	TerraformerItem.highlightTexture = GameTexture.fromFile("tiles/tile_highlight");
    	ResourceEncoder.addModResources(LoadedMod.getRunningMod());
    }
}
