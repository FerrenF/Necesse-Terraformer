package constructors.container;

import necesse.inventory.container.ContainerTransferResult;
import necesse.inventory.container.SlotIndexRange;

import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.entity.mobs.PlayerMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.customAction.EmptyCustomAction;
import necesse.inventory.container.item.ItemInventoryContainer;
import necesse.inventory.container.slots.ContainerSlot;
import necesse.inventory.item.placeableItem.tileItem.TileItem;


public class TerraformerContainer extends ItemInventoryContainer {
	public final EmptyCustomAction clearMaterialSlot;
	public static int currentContainerID = -1;
	public static TerraformerContainer instance;
	
	public TerraformerContainer(NetworkClient client, int uniqueSeed, Packet content) {
		super(client, uniqueSeed, content);
		instance = this;
		this.clearMaterialSlot = (EmptyCustomAction) this.registerAction(new EmptyCustomAction() {
			protected void run() {				
				ContainerSlot ingredientSlot = TerraformerContainer.this.getSlot(1);
				InventoryItem item = ingredientSlot.getItem();
				
				if (item != null) {
					PlayerMob player = this.getContainer().client.playerMob;
					player.getInv().addItemsDropRemaining(item, "addback", player, false, true);
					ingredientSlot.setItem((InventoryItem) null);
				}
				
				
			}
		});
	}
	
	@Override
	public ContainerTransferResult transferToSlots(ContainerSlot slot, Iterable<SlotIndexRange> ranges, int amount,	String purpose) {
		String error = null;
		InventoryItem slotItem = slot.getItem();
		
		if(!(slotItem.item instanceof TileItem)) {
			error = "Must be a tile.";
			return new ContainerTransferResult(amount, error);
		}
		return super.transferToSlots(slot, ranges, amount, purpose);
	}
	

}
