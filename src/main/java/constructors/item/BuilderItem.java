package constructors.item;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import constructors.ConstructorsMod;
import constructors.container.BuilderContainer;
import constructors.drawables.ConstructorTileDrawable;
import constructors.drawables.ConstructorTileDrawable.TileDrawableOptions;
import constructors.drawables.ConstructorTileDrawable.TileHighlightType;
import constructors.form.BuilderContainerForm;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryAddConsumer;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventorySlot;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemStatTipList;
import necesse.inventory.item.placeableItem.objectItem.ObjectItem;
import necesse.inventory.item.upgradeUtils.UpgradedItem;
import necesse.inventory.recipe.Ingredient;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.ObjectPlaceOption;
import necesse.level.gameObject.WallObject;
import necesse.level.maps.Level;
import necesse.level.maps.LevelTile;
import necesse.level.maps.TilePosition;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.light.LightManager;

public class BuilderItem extends ConstructorItem {
		
		public static final boolean SR_NO_MODIFY = true;	
						
		public BuilderItem() {
			super();	
			BuilderContainerForm.playerTerraformer = this;	
			
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
			tooltips.add(Localization.translate("builder", "buildertip1"));
			tooltips.add(Localization.translate("builder", "buildertip2"));
			tooltips.add(Localization.translate("builder", "buildertip3"));
			tooltips.add(Localization.translate("builder", "buildertip4"));
			tooltips.add(Localization.translate("builder", "buildertip5"));
			tooltips.add(Localization.translate("builder", "buildertip6"));
			tooltips.add(Localization.translate("builder", "buildertip7"));
			tooltips.add(super.getPreEnchantmentTooltips(item, perspective, blackboard));
			return tooltips;
		}
				
		@Override
		public InventoryItem onAttack(Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight,
				InventoryItem me, ItemAttackSlot slot, int animAttack, int seed, GNDItemMap mapContent) {		
					
			
			ArrayList<ObjectItem> replacedObjects = new ArrayList<ObjectItem>();	
			if(currentlyHighlightedTiles!=null) {
				
				LevelTile[][] cloneTiles = this.getSelectedTiles();
				this.clearOutOfRangeTiles(cloneTiles,(PlayerMob)attackerMob, this.maxPlacementRange.getValue(this.getUpgradeTier(me)));
				
				int objectsExpended = 0;
				for(int i=0;i<cloneTiles.length;i++) {
					for(int j=0;j<cloneTiles[i].length;j++) {
						
						LevelTile targetTile = cloneTiles[i][j];
						if(targetTile==null) {
							continue;
						}
						
						ObjectItem highlightedObjectItem = level.objectLayer.getObject(targetTile.tileX, targetTile.tileY).getObjectItem();						
						ObjectItem objectInBucket = null;
						
							objectInBucket = getCurrentObject(me);
							if(objectInBucket != null) {		
								int numTilesInBucket = getCurrentObjectAmount(me);
								if(numTilesInBucket >= objectsExpended) {
									
									if(highlightedObjectItem == null || (highlightedObjectItem.getID() != objectInBucket.getID())) {	
										
										ObjectPlaceOption po = objectInBucket.getBestPlaceOption(level,								
												targetTile.tileX * 32, 
												targetTile.tileY * 32, 
												BuilderItem.this.getObjectInvItem(me),
												
												(PlayerMob)attackerMob);
										
												boolean canPlace = true;		
												String checkResult = objectInBucket.getObject().canPlace(level, po.tileX*32, po.tileY*32, po.rotation, true) ;
												canPlace = canPlace && checkResult != "liquid" && checkResult != "shore";
												
												if(po != null && canPlace){			
													
													if(level.isServer()) {
														if(highlightedObjectItem != null &&
																highlightedObjectItem.getObject().getID() != ObjectRegistry.getObjectID("air")) {
															replacedObjects.add(highlightedObjectItem);											
														}											
													objectsExpended+=1;		
													objectInBucket.getObject().placeObject(level, po.tileX, po.tileY, po.rotation, true);	
													level.sendObjectUpdatePacket(po.tileX, po.tileY);
												}								
										}
									}
								}	
						}				
						
					}
				}
				
				if(level.isServer()) {
					if(objectsExpended > 0) {
						this.removeObjectsFromBucket(me, objectsExpended);
					}
					if(replacedObjects.size() > 0 && attackerMob instanceof PlayerMob) {
						PlayerMob p = (PlayerMob)attackerMob;
						replacedObjects.forEach((tile)->{
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
				
				LevelTile[][] cloneTiles = this.getSelectedTiles();
				this.clearOutOfRangeTiles(cloneTiles,(PlayerMob)attackerMob, this.maxPlacementRange.getValue(this.getUpgradeTier(me)));
				int addCurrentObjects = 0;
				for(int i=0;i<cloneTiles.length;i++) {
					for(int j=0;j<cloneTiles[i].length;j++) {
						
						LevelTile targetTile = cloneTiles[i][j];

						if(targetTile==null) {
							continue;
						}			
						GameObject gameObjOnTile = level.objectLayer.getObject(targetTile.tileX, targetTile.tileY);
						ObjectItem objectOnTile = gameObjOnTile.getObjectItem();
							
						if(!hasObject(me)) {
							
							if(level.isServer()) {
								if(objectOnTile.getID() != ObjectRegistry.getObjectID("air")) { 
									InventoryItem newItem = new InventoryItem(objectOnTile);
									newItem.setAmount(1);
									setObject(level, p, me, newItem);	
								}
							}
							level.objectLayer.setObject(targetTile.tileX,targetTile.tileY, 0);
							level.sendObjectUpdatePacket(targetTile.tileX,targetTile.tileY);
						}
						else if((objectOnTile.getID() == getCurrentObject(me).getID())) {		
							if(level.isServer()) { 
								addCurrentObjects += 1;
							}
							level.objectLayer.setObject(targetTile.tileX,targetTile.tileY, 0);		
							level.sendObjectUpdatePacket(targetTile.tileX,targetTile.tileY);
						}		
					}
				}		
				if(level.isServer()) {
					if(addCurrentObjects > 0) {
						int remainder = addCurrentObjectAmount(me, addCurrentObjects);
						if(remainder > 0) {
							InventoryItem newItem = new InventoryItem(this.getCurrentObject(me));
							newItem.setAmount(remainder);
							p.getInv().addItem(newItem, true, "give", (InventoryAddConsumer) null);
						}
					}
				}
			}
			return me;
		}
				
		@SuppressWarnings("unchecked")
		public void onMouseHoverTile(InventoryItem me, GameCamera camera, PlayerMob perspective, int mouseX, int mouseY,
				TilePosition pos, boolean isDebug) {
			
			if(active && currentShape != null && 
					perspective!=null && perspective.getLevel() != null) {
				
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
				
					final ObjectItem objectInBucket;
					int objID = -1;
					if(hasObject(me)) {
						objectInBucket = getCurrentObject(me);
						objID = objectInBucket.getObject().getID();
					}
					else {
						objectInBucket = null;
					}		
				
				this.highlightDraw = new ConstructorTileDrawable<GameObject>(
						
						perspective.getLevel(),
						perspective,
						camera,
						currentlyHighlightedTiles,
						objID,
						this.maxPlacementRange.getValue(this.getUpgradeTier(me)),
						
							(lvObj)->{
								return lvObj.level.objectLayer.getObject(lvObj.tileX, lvObj.tileY);
							},
							
							(lvObj, tgtObj)->{
								return lvObj.getID() != tgtObj;
							}
						);	
				
				((ConstructorTileDrawable<GameObject>)highlightDraw).perTileDrawStep = new TileDrawableOptions() {

					@Override
					public void draw(Level level, PlayerMob perspective, LevelTile tile, TileHighlightType highlightType) {
						
						if(tile == null || objectInBucket == null) return;
						if(highlightType == TileHighlightType.OUT_OF_RANGE || highlightType == TileHighlightType.ALREADY_PAINTED_TILE) return;
						
					
						ObjectPlaceOption po = objectInBucket.getBestPlaceOption(level,								
								tile.tileX * 32, 
								tile.tileY * 32, 
								BuilderItem.this.getObjectInvItem(me),
								perspective);
						
						boolean canPlace = true;		
						String checkResult = objectInBucket.getObject().canPlace(level, po.tileX, po.tileY, po.rotation, true) ;
						canPlace = canPlace && checkResult != "liquid" && checkResult != "shore";
						
						if (po != null && canPlace) {		
							float alpha = 0.5F;						
							po.object.drawMultiTilePreview(level, po.tileX, po.tileY, po.rotation, alpha, perspective, camera);
						}
					}
					
				};
				highlightDraw.draw(perspective.getLevel().tickManager());
			}
			
		}
		
		public void wallDrawPreview(InventoryItem me, WallObject src, Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player,
				GameCamera camera) {
			SharedTextureDrawOptions options = new SharedTextureDrawOptions(WallObject.generatedWallTexture);
			this.addWallDrawOptions(me, src, options, level, tileX, tileY, level.lightManager.newLight(150.0F), (TickManager) null,
					camera, player);
			options.forEachDraw((w) -> {
				w.alpha(0.5F);
			}).draw();
		}
		
		public void addWallDrawOptions(InventoryItem me, WallObject src, SharedTextureDrawOptions options, Level level, int tileX, int tileY,
				GameLight lightOverride, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
			Performance.record(tickManager, "wallSetup", () -> {
				int drawX = camera.getTileDrawX(tileX);
				int drawY = camera.getTileDrawY(tileY);
				GameObject[] adj = level.getAdjacentObjects(tileX, tileY);

				// Retrieve the currently selected LevelTiles
				LevelTile[][] selectedTiles = BuilderItem.this.getSelectedTiles();
				List<GameObject> adjList = new ArrayList<>(Arrays.asList(adj));

				// Iterate through selected tiles to add matching adjacent objects
				for (LevelTile[] tileRow : selectedTiles) {
				    for (LevelTile tile : tileRow) {
				        if (tile == null) continue;

				        int deltaX = Math.abs(tile.tileX - tileX);
				        int deltaY = Math.abs(tile.tileY - tileY);

				        // Ensure only direct neighbors are considered (not diagonals)
				        if ((deltaX == 1 && deltaY == 0) || (deltaY == 1 && deltaX == 0)) {
				            GameObject obj = level.getObject(tile.tileX, tile.tileY);
				           // if (obj instanceof WallObject && obj.getClass().equals(src.getClass())) {
				                adjList.add(obj);
				           // }
				        }
				    }
				}

				// Convert list back to an array
				adj = adjList.toArray(new GameObject[0]);
		        
				boolean allIsSameWall = true;
				boolean[] sameWall = new boolean[adj.length];
				boolean forceDrawTop = false;
				boolean forceRemoveBot = false;

				for (int i = 0; i < adj.length; ++i) {
					GameObject adjObject = adj[i];
					boolean connectedWall = src.isConnectedWall(adjObject);
					sameWall[i] = connectedWall;
					allIsSameWall = allIsSameWall && connectedWall;
					if (connectedWall) {
						if (i == 1) {
							if (adjObject instanceof WallObject && ((WallObject) adjObject).isWallDrawingTop()) {
								forceDrawTop = true;
							}
						} else if (i == 6 && adjObject instanceof WallObject
								&& ((WallObject) adjObject).isWallDrawingTop()) {
							forceRemoveBot = true;
						}
					}
				}

				float alpha = 1.0F;
				if (perspective != null && !Settings.hideUI && !Settings.hideCursor) {
					Rectangle alphaRec = new Rectangle(tileX * 32 - 16, tileY * 32 - 32, 64, 48);
					if (perspective.getCollision().intersects(alphaRec)) {
						alpha = 0.5F;
					} else if (alphaRec.contains(camera.getX() + WindowManager.getWindow().mousePos().sceneX,
							camera.getY() + WindowManager.getWindow().mousePos().sceneY)) {
						alpha = 0.5F;
					}
				}

				GameLight[] lights;
				if (lightOverride == null) {
					Point[] var10003 = Level.adjacentGettersWithCenter;
					LightManager var10004 = level.lightManager;
					Objects.requireNonNull(var10004);
					lights = (GameLight[]) level.getRelative(tileX, tileY, var10003, var10004::getLightLevelWall, (x$0) -> {
						return new GameLight[x$0];
					});
				} else {
					lights = new GameLight[9];
					Arrays.fill(lights, lightOverride);
				}

				GameTextureSection wallTexture = src.wallTexture.getDamagedTexture(src, level, tileX, tileY);
				src.addWallDrawOptions(options, wallTexture, drawX, drawY, lights, alpha, sameWall, allIsSameWall,
						forceDrawTop, forceRemoveBot);
			});
		}
		
		private LevelTile[][] getSelectedTiles() {
			return this.currentlyHighlightedTiles.clone();
		}

		public boolean isConnectedWall(WallObject me, GameObject them) {
			return them == me || me.connectedWalls.contains(them.getID());
		}
		
		protected void updateContainerForm() {
			if(BuilderContainerForm.instance!=null && !BuilderContainerForm.instance.isDisposed()) {
				BuilderContainerForm.instance.updateShapeSizeLabel(shapeSize);
			}		
		}
		@Override
		protected void openContainer(ServerClient client, PlayerInventorySlot inventorySlot) {
			PacketOpenContainer p = new PacketOpenContainer(ConstructorsMod.BUILDER_CONTAINER,
					BuilderContainer.getContainerContent(this, inventorySlot));
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
			return arg0 instanceof ObjectItem;
		}

		@Override
		public boolean isValidRequestType(Item.Type type) {
			return false;
		}
		
		public InventoryItem getObjectInvItem(InventoryItem me) {
			if(hasObject(me)) { 		
				Inventory _me = this.getInternalInventory(me);
				return _me.getItem(0);
				}
			return null;
		}
		
		public boolean hasObject(InventoryItem me) {			
			Inventory _me = this.getInternalInventory(me);
			return (_me.getItemSlot(0) != null) && !_me.isSlotClear(0);		
		}
		
		public ObjectItem getCurrentObject(InventoryItem me) {			
			if(this.hasObject(me)) {
				return (ObjectItem)(getObjectInvItem(me).item);
			}
			return null;
		}
		
		public int getCurrentObjectAmount(InventoryItem me) {
			if(this.hasObject(me)) {
				Inventory _me = this.getInternalInventory(me);
				return _me.getAmount(0);
			}
			return 0;
		}	
		
		public void setCurrentObjectAmount(InventoryItem me, int newAmount) {
			if(this.hasObject(me)) {			
				Inventory _me = this.getInternalInventory(me);
				 _me.setAmount(0, newAmount);
				 this.saveInternalInventory(me, _me);
			}
		}
		
		public int addCurrentObjectAmount(InventoryItem me, int addAmount) {
		    if (this.hasObject(me)) {
		        Inventory _me = this.getInternalInventory(me);
		        int limit = _me.getItemStackLimit(0, getObjectInvItem(me));
		        int currentAmount = getCurrentObjectAmount(me);

		        // Calculate how much can be added without exceeding the limit
		        int newAmount = Math.min(currentAmount + addAmount, limit);
		        int overTheLimit = (currentAmount + addAmount) - limit;
		        overTheLimit = Math.max(0, overTheLimit);

		        // Update stored amount with the correct new value
		        setCurrentObjectAmount(me, newAmount);

		        return overTheLimit; // Return remainder that couldn't be added
		    }
		    return -1; 
		}
		
		private void removeObjectsFromBucket(InventoryItem me, int amount) {
			if(this.hasObject(me)) {
				int now = getCurrentObjectAmount(me);
				if(now <= amount) {
					setCurrentObjectAmount(me, 0);
					return;
				}
				setCurrentObjectAmount(me, now - amount);
			}			
		}
		
		public void setObject(Level level, PlayerMob player, InventoryItem me, InventoryItem newItem) {		
			Inventory _me = this.getInternalInventory(me);
			_me.addItem(level, player, newItem, "give", null);		
			 this.saveInternalInventory(me, _me);
		}

		@Override
		protected Ingredient[] getSpecialUpgradeCost(int nextTier) {
			switch(nextTier) {
			case 1: return new Ingredient[]{
					new Ingredient(ObjectRegistry.getObject("woodwall").getObjectItem().getStringID(), 250)
			};
			case 2:return new Ingredient[]{
					new Ingredient(ObjectRegistry.getObject(776).getObjectItem().getStringID(), 250),
					new Ingredient(ObjectRegistry.getObject(797).getObjectItem().getStringID(), 250),
					new Ingredient(ObjectRegistry.getObject(786).getObjectItem().getStringID(), 250),
			};
			case 3:return new Ingredient[]{
					new Ingredient(ObjectRegistry.getObject(827).getObjectItem().getStringID(), 250),
					new Ingredient(ObjectRegistry.getObject(863).getObjectItem().getStringID(), 250),
					new Ingredient(ObjectRegistry.getObject(839).getObjectItem().getStringID(), 250),
			};
			case 4:return new Ingredient[]{
					new Ingredient(ObjectRegistry.getObject(129).getObjectItem().getStringID(), 500)
			};
			default: return new Ingredient[]{new Ingredient("upgradeshard", nextTier * 200)};
		}	
		}	
	}