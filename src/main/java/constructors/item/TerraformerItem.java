package constructors.item;
import java.util.ArrayList;

import constructors.ConstructorsMod;
import constructors.container.TerraformerContainer;
import constructors.drawables.ConstructorTileDrawable;
import constructors.form.TerraformerContainerForm;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryAddConsumer;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventorySlot;
import necesse.inventory.item.Item;
import necesse.inventory.item.placeableItem.tileItem.TileItem;
import necesse.inventory.recipe.Ingredient;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.LevelTile;
import necesse.level.maps.TilePosition;

public class TerraformerItem extends ConstructorItem {
		
		public static final boolean SR_NO_MODIFY = true;	
		public TerraformerItem() {
			super();
			TerraformerContainerForm.playerTerraformer = this;	
			this.maxPlacementRange.setBaseValue(12).setUpgradedValue(1.0F, 18);
			this.maxShapeSize.setBaseValue(6).setUpgradedValue(1.0F, 10);
		}
		
		@Override
		public void initializeShapes() {
			if(shapes_initialized) return;
			shapes_initialized = true;
			
			shapes.put(Shape.SQUARE, new ShapeSelectionSquare(this));
			shapes.put(Shape.LINE_BOX, new ShapeSelectionLineBox(this));
			shapes.put(Shape.CHECKERBOARD, new ShapeSelectionCheckerboard(this));
			shapes.put(Shape.LINE, new ShapeSelectionLine(this, LineDirection.VERTICAL));
			shapes.put(Shape.CIRCLE, new ShapeSelectionCircle(this));
			shapes.put(Shape.RING, new ShapeSelectionRing(this));
		}
		
		@Override
		public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective,
				GameBlackboard blackboard) {			
			ListGameTooltips tooltips = new ListGameTooltips();		
			tooltips.add(Localization.translate("terraformer", "terraformertip1"));
			tooltips.add(Localization.translate("terraformer", "terraformertip2"));
			tooltips.add(Localization.translate("terraformer", "terraformertip3"));
			tooltips.add(Localization.translate("terraformer", "terraformertip4"));
			tooltips.add(Localization.translate("terraformer", "terraformertip5"));
			tooltips.add(Localization.translate("terraformer", "terraformertip6"));
			tooltips.add(Localization.translate("terraformer", "terraformertip7"));
			tooltips.add(super.getPreEnchantmentTooltips(item, perspective, blackboard));
			return tooltips;
		}
				
		@Override
		public InventoryItem onAttack(Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight,
				InventoryItem me, ItemAttackSlot slot, int animAttack, int seed, GNDItemMap mapContent) {		
					
			ArrayList<TileItem> replacedTiles = new ArrayList<TileItem>();	
			if(currentlyHighlightedTiles!=null) {
				LevelTile[][] cloneTiles = currentlyHighlightedTiles.clone();	
				this.clearOutOfRangeTiles(cloneTiles,(PlayerMob)attackerMob, this.maxPlacementRange.getValue(this.getUpgradeTier(me)));
				int tilesExpended = 0;
				for(int i=0;i<cloneTiles.length;i++) {
					for(int j=0;j<cloneTiles[i].length;j++) {
						
						LevelTile targetTile = cloneTiles[i][j];
						if(targetTile==null) {
							continue;
						}
						
						TileItem highlightedTileItem = targetTile.tile.getTileItem();						
						TileItem tileInBucket = null;
						
							tileInBucket = getCurrentTile(me);
							if(tileInBucket != null) {		
								int numTilesInBucket = getCurrentTileAmount(me);
								if(numTilesInBucket >= tilesExpended) {
									
									if(highlightedTileItem.getID() != tileInBucket.getID()) {	
										
										if(level.isServer()) {
											if(highlightedTileItem.getID() != TileRegistry.dirtID) {
												replacedTiles.add(highlightedTileItem);											
											}											
											tilesExpended+=1;		
											
											level.sendTileChangePacket(level.getServer(), targetTile.tileX, targetTile.tileY, tileInBucket.tileID);
											level.tileLayer.setIsPlayerPlaced(targetTile.tileX, targetTile.tileY, true);					
										}		
									
												
									}
								}	
						}				
						
					}
				}
				
				if(level.isServer()) {
					if(tilesExpended > 0) {
						this.removeTilesFromBucket(me, tilesExpended);
					}
					if(replacedTiles.size() > 0 && attackerMob instanceof PlayerMob) {
						PlayerMob p = (PlayerMob)attackerMob;
						replacedTiles.forEach((tile)->{
							p.getInv().addItem(new InventoryItem(tile), true, "give", (InventoryAddConsumer) null);
						});					
						
					}
				}
			}
			
			return me;
		}		

		@Override
		public InventoryItem onLevelInteract(Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight,
				InventoryItem me, ItemAttackSlot slot, int seed, GNDItemMap mapContent) {			
			if(!(attackerMob instanceof PlayerMob)) return me;
			PlayerMob p = (PlayerMob)attackerMob;
			if(currentlyHighlightedTiles!=null) {
				LevelTile[][] cloneTiles = currentlyHighlightedTiles.clone();	
				this.clearOutOfRangeTiles(cloneTiles,(PlayerMob)attackerMob, this.maxPlacementRange.getValue(this.getUpgradeTier(me)));
				int addTiles = 0;
				for(int i=0;i<cloneTiles.length;i++) {
					for(int j=0;j<cloneTiles[i].length;j++) {
						
						LevelTile targetTile = cloneTiles[i][j];
						if(targetTile==null) {
							continue;
						}			
						if(targetTile.tile.getID() == TileRegistry.dirtID){
							continue;  // we dont pick up dirt in these parts
						}						
						if(!hasTile(me)) {
							
							if(level.isServer()) {
								InventoryItem newItem = new InventoryItem(targetTile.tile.getTileItem());
								newItem.setAmount(1);
								setTile(level, p, me, newItem);		
								
								level.sendTileChangePacket(level.getServer(), targetTile.tileX, targetTile.tileY, targetTile.tile.getDestroyedTile());
								level.tileLayer.setIsPlayerPlaced(targetTile.tileX, targetTile.tileY, true);
							}											
						}
						else if((targetTile.tile.getID() == getCurrentTile(me).tileID)) {		
							
							if(level.isServer()) { 
								addTiles += 1;
								
								level.sendTileChangePacket(level.getServer(), targetTile.tileX, targetTile.tileY, targetTile.tile.getDestroyedTile());
								level.tileLayer.setIsPlayerPlaced(targetTile.tileX, targetTile.tileY, true);								
							}
							
						}		
					}
				}		
				if(level.isServer()) {
					if(addTiles > 0) {
						int remainder = addCurrentTileAmount(me, addTiles);
						if(remainder > 0) {
							InventoryItem newItem = new InventoryItem(this.getCurrentTile(me));
							newItem.setAmount(remainder);
							p.getInv().addItem(newItem, true, "give", (InventoryAddConsumer) null);
						}
					}
				}
			}
			return me;
		}
				
		public void onMouseHoverTile(InventoryItem me, GameCamera camera, PlayerMob perspective, int mouseX, int mouseY,
				TilePosition pos, boolean isDebug) {
			
			if(active && currentShape != null &&
					perspective!=null  && perspective.getLevel() != null) {
				
				if(this.currentShape.shapeID == Shape.LINE) {
					ShapeSelectionLine cl = ((ShapeSelectionLine)this.currentShape);
					if(!perspective.isAttacking) {
						if(perspective.getDir() == 0 || perspective.getDir() == 2) {
							if(cl.getDirection()!=LineDirection.VERTICAL) cl.setDirection(LineDirection.VERTICAL);			
						}
						else {
							if(cl.getDirection()!=LineDirection.HORIZONTAL) cl.setDirection(LineDirection.HORIZONTAL);
						}
					}
					
				}
				
				currentlyHighlightedTiles = this.currentShape.getTilesAround(perspective, pos);
				TileItem tileInBucket = null;
				int tileID = -1;
				if(hasTile(me)) {
					tileInBucket = getCurrentTile(me);
					tileID = tileInBucket.tileID;
				}
			
				
				
				
				this.highlightDraw = new ConstructorTileDrawable<GameTile>(
						perspective.getLevel(),
						perspective,
						camera,
						currentlyHighlightedTiles,
						tileID,
						this.maxPlacementRange.getValue(this.getUpgradeTier(me)),
						
							(lvTile)->{
								return lvTile.tile;
							},
							
							(lvTile, tgTileID)->{
								return lvTile.getID() == tgTileID;
							}
						);	
				
				highlightDraw.draw(perspective.getLevel().tickManager());
			}
			
		}
		
		protected void updateContainerForm() {
			if(TerraformerContainerForm.instance!=null && !TerraformerContainerForm.instance.isDisposed()) {
				TerraformerContainerForm.instance.updateShapeSizeLabel(shapeSize);
			}		
		}
		@Override
		protected void openContainer(ServerClient client, PlayerInventorySlot inventorySlot) {
			PacketOpenContainer p = new PacketOpenContainer(ConstructorsMod.TERRAFORMER_CONTAINER,
					TerraformerContainer.getContainerContent(this, inventorySlot));
					ContainerRegistry.openAndSendContainer(client, p);
		}
		
		@Override
		public int getInternalInventorySize() {		
			return 1;
		}

		@Override
		public boolean isValidPouchItem(InventoryItem arg0) {			
			return isValidRequestItem(arg0.item);
		}

		@Override
		public boolean isValidRequestItem(Item arg0) {
			return arg0 instanceof TileItem;
		}

		@Override
		public boolean isValidRequestType(Item.Type type) {
			return false;
		}
		
		
		
		public InventoryItem getTileInvItem(InventoryItem me) {
			if(hasTile(me)) { 		
				Inventory _me = this.getInternalInventory(me);
				return _me.getItem(0);
				}
			return null;
		}
		
		public boolean hasTile(InventoryItem me) {			
			Inventory _me = this.getInternalInventory(me);
			return (_me.getItemSlot(0) != null) && !_me.isSlotClear(0);		
		}
		
		public TileItem getCurrentTile(InventoryItem me) {			
			if(this.hasTile(me)) {
				return (TileItem)(getTileInvItem(me).item);
			}
			return null;
		}
		
		public int getCurrentTileAmount(InventoryItem me) {
			if(this.hasTile(me)) {
				Inventory _me = this.getInternalInventory(me);
				return _me.getAmount(0);
			}
			return 0;
		}	
		
		public void setCurrentTileAmount(InventoryItem me, int newAmount) {
			if(this.hasTile(me)) {			
				Inventory _me = this.getInternalInventory(me);
				 _me.setAmount(0, newAmount);
				 this.saveInternalInventory(me, _me);
			}
		}
		
		public int addCurrentTileAmount(InventoryItem me, int addAmount) {
		    if (this.hasTile(me)) {
		        Inventory _me = this.getInternalInventory(me);
		        int limit = _me.getItemStackLimit(0, getTileInvItem(me));
		        int currentAmount = getCurrentTileAmount(me);

		        // Calculate how much can be added without exceeding the limit
		        int newAmount = Math.min(currentAmount + addAmount, limit);
		        int overTheLimit = (currentAmount + addAmount) - limit;
		        overTheLimit = Math.max(0, overTheLimit);

		        // Update stored amount with the correct new value
		        setCurrentTileAmount(me, newAmount);

		        return overTheLimit; // Return remainder that couldn't be added
		    }
		    return -1; 
		}
		
		private void removeTilesFromBucket(InventoryItem me, int tilesExpended) {
			if(this.hasTile(me)) {
				int now = getCurrentTileAmount(me);
				if(now <= tilesExpended) {
					setCurrentTileAmount(me, 0);
					return;
				}
				setCurrentTileAmount(me, now - tilesExpended);
			}			
		}
		
		public void setTile(Level level, PlayerMob player, InventoryItem me, InventoryItem newItem) {		
			Inventory _me = this.getInternalInventory(me);
			_me.addItem(level, player, newItem, "give", null);		
			 this.saveInternalInventory(me, _me);
		}

		@Override
		protected Ingredient[] getSpecialUpgradeCost(int nextTier) {
			
			switch(nextTier) {
				case 1: return new Ingredient[]{
						new Ingredient(TileRegistry.getTile(TileRegistry.grassID).getTileItem().getStringID(), 250),
						new Ingredient(TileRegistry.getTile(TileRegistry.snowID).getTileItem().getStringID(), 250),
						new Ingredient(TileRegistry.getTile(TileRegistry.sandID).getTileItem().getStringID(), 250),
						new Ingredient(TileRegistry.getTile(TileRegistry.plainsGrassID).getTileItem().getStringID(), 250),
						new Ingredient(TileRegistry.getTile(TileRegistry.swampGrassID).getTileItem().getStringID(), 250)										
				};
				case 2: return new Ingredient[]{
						new Ingredient(TileRegistry.getTile(TileRegistry.rockID).getTileItem().getStringID(), 250),
						new Ingredient(TileRegistry.getTile(TileRegistry.sandstoneID).getTileItem().getStringID(), 250),
						new Ingredient(TileRegistry.getTile(TileRegistry.swampRockID).getTileItem().getStringID(), 250),
						new Ingredient(TileRegistry.getTile(TileRegistry.graniteRockID).getTileItem().getStringID(), 250),
						new Ingredient(TileRegistry.getTile(TileRegistry.snowRockID).getTileItem().getStringID(), 250)										
				};
				case 3: return new Ingredient[]{
						new Ingredient(TileRegistry.getTile(TileRegistry.deepRockID).getTileItem().getStringID(), 250),
						new Ingredient(TileRegistry.getTile(TileRegistry.deepSandstoneID).getTileItem().getStringID(), 250),
						new Ingredient(TileRegistry.getTile(TileRegistry.deepSwampRockID).getTileItem().getStringID(), 250),
						new Ingredient(TileRegistry.getTile(TileRegistry.deepStoneFloorID).getTileItem().getStringID(), 250),
						new Ingredient(TileRegistry.getTile(TileRegistry.deepSnowRockID).getTileItem().getStringID(), 250)										
				};
				case 4: return new Ingredient[]{
						new Ingredient(TileRegistry.getTile(TileRegistry.dungeonFloorID).getTileItem().getStringID(), 250),
						new Ingredient(TileRegistry.getTile(TileRegistry.lavaID).getTileItem().getStringID(), 100)									
				};
				default: return new Ingredient[]{new Ingredient("upgradeshard", nextTier * 200)};
			}		
		}	
	}
