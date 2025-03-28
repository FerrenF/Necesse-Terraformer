package voidBucket.item;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import necesse.engine.GameState;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Control;
import necesse.engine.input.InputEvent;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.world.GameClock;
import necesse.engine.world.WorldSettings;
import necesse.entity.Entity;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryAddConsumer;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventorySlot;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemInteractAction;
import necesse.inventory.item.ItemStatTipList;
import necesse.inventory.item.TickItem;
import necesse.inventory.item.miscItem.InternalInventoryItemInterface;
import necesse.inventory.item.miscItem.PouchItem;
import necesse.inventory.item.placeableItem.tileItem.TileItem;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;
import necesse.inventory.item.upgradeUtils.UpgradableItem;
import necesse.inventory.item.upgradeUtils.UpgradedItem;
import necesse.level.maps.Level;
import necesse.level.maps.LevelTile;
import necesse.level.maps.TilePosition;
import necesse.level.maps.light.GameLight;
import voidBucket.Terraformer;
import voidBucket.container.TerraformerContainer;
import voidBucket.form.TerraformerContainerForm;

public class TerraformerItem extends PouchItem implements TickItem,UpgradableItem,ItemInteractAction {
	
		public static enum Shape {
			SQUARE,
			LINE,
			CIRCLE,
			RING
		}
		public static enum LineDirection {
	        HORIZONTAL, VERTICAL, DIAGONAL_TL_BR, DIAGONAL_TR_BL
	    }
		
		private static boolean shapes_initialized = false;
		public static GameTexture highlightTexture;
		public static Shape lastShapeSelection;
		public static final boolean SR_NO_MODIFY = true;		
		public static InputEvent savedDefaultInputEventUp;
		public static InputEvent savedDefaultInputEventDown;
		public boolean active = false;
		private SortedDrawable highlightDraw;
		
		private LevelTile[][] currentlyHighlightedTiles;
		public ShapeSelection currentShape;
		private int bucketRange = 14;
		private int maxShapeSize = 10;
		private int minShapeSize = 1;
		private int currentShapeSize = 4;
		
		public static Map<Shape, ShapeSelection> shapes = new HashMap<Shape, ShapeSelection>();
		public TerraformerItem() {
			super();		
			
			initializeShapes();
			this.stackSize = 1;
			
			this.attackCooldownTime = new IntUpgradeValue().setBaseValue(100);			
			this.rarity = Rarity.UNIQUE;	
			
			TerraformerContainerForm.playerTerraformer = this;
			if(lastShapeSelection!=null) {
				this.setShape(lastShapeSelection);
			}
			else {
				this.setShape(Shape.SQUARE);
			}			
			
			updateShapes();
		}
		
		private void updateShapes() {
			this.setShapesMaxSizes(maxShapeSize);
			this.setShapesSizes(currentShapeSize);
			
			if(TerraformerContainerForm.instance!=null && !TerraformerContainerForm.instance.isDisposed()) {
				TerraformerContainerForm.instance.updateShapeSizeLabel(currentShapeSize);
			}			
		}
		
		private void setShapesSizes(int i) {
			this.currentShapeSize = i;
			shapes.forEach((key, value)->{
				value.setSize(i);
			});
		}
		
		private void setShapesMaxSizes(int i) {
			this.maxShapeSize = i;
			shapes.forEach((key, value)->{
				value.setMaxSize(i);
			});
		}

		public static void initializeShapes() {
			if(shapes_initialized) return;
			shapes_initialized = true;
			
			shapes.put(Shape.SQUARE, new ShapeSelectionSquare(2));
			shapes.put(Shape.LINE, new ShapeSelectionLine(2, LineDirection.VERTICAL));
			shapes.put(Shape.CIRCLE, new ShapeSelectionCircle(2));
			shapes.put(Shape.RING, new ShapeSelectionRing(2));
		}
		
		public void setShape(Shape shapeID) {
			this.currentShape = shapes.get(shapeID);	
			this.minShapeSize = currentShape.minSize; // dictated by shape algorithm
		}

		public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
			ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);			
			tooltips.add(Localization.translate("terraformer", "terraformertip1"));
			tooltips.add(Localization.translate("terraformer", "terraformertip2"));
			tooltips.add(Localization.translate("terraformer", "terraformertip3"));
			tooltips.add(Localization.translate("terraformer", "terraformertip4"));
			tooltips.add(Localization.translate("terraformer", "terraformertip5"));
			tooltips.add(Localization.translate("terraformer", "terraformertip6"));
			tooltips.add(Localization.translate("terraformer", "terraformertip7"));
			return tooltips;
		}
		
		public InventoryItem onAttack(Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight,
				InventoryItem me, ItemAttackSlot slot, int animAttack, int seed, GNDItemMap mapContent) {		
			
			
			ArrayList<TileItem> replacedTiles = new ArrayList<TileItem>();	
			if(currentlyHighlightedTiles!=null) {
				LevelTile[][] cloneTiles = currentlyHighlightedTiles.clone();	
				this.clearOutOfRangeTiles(cloneTiles,(PlayerMob)attackerMob, this.bucketRange);
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
										}
										
										level.tileLayer.setTile(targetTile.tileX, targetTile.tileY, tileInBucket.tileID);																		
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
		

		private void clearOutOfRangeTiles(LevelTile[][] cloneTiles, PlayerMob perspective, int _range) {
			for(int i=0;i<cloneTiles.length;i++) {
				for(int j=0;j<cloneTiles[i].length;j++) {
					LevelTile targetTile = cloneTiles[i][j];
					if(targetTile==null) continue;
					int tileX = targetTile.tileX;
					int tileY = targetTile.tileY;	
										
					int dx = tileX - perspective.getTileX();
					int dy = tileY - perspective.getTileY();
					if((dx * dx + dy * dy) > (_range * _range)) {
						cloneTiles[i][j]=null;
					}
				}
			}
			
		}

		public InventoryItem onLevelInteract(Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight,
				InventoryItem me, ItemAttackSlot slot, int seed, GNDItemMap mapContent) {			
			if(!(attackerMob instanceof PlayerMob)) return me;
			PlayerMob p = (PlayerMob)attackerMob;
			if(currentlyHighlightedTiles!=null) {
				LevelTile[][] cloneTiles = currentlyHighlightedTiles.clone();	
				this.clearOutOfRangeTiles(cloneTiles,(PlayerMob)attackerMob, this.bucketRange);
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
							}
							level.tileLayer.setTile(targetTile.tileX, targetTile.tileY, targetTile.tile.getDestroyedTile());
						}
						else if((targetTile.tile.getID() == getCurrentTile(me).tileID)) {							
							if(level.isServer()) { 
								addTiles += 1;
							}
							level.tileLayer.setTile(targetTile.tileX, targetTile.tileY, targetTile.tile.getDestroyedTile());						
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
		
		public boolean canLevelInteract(Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item) {
			return true;
		}

		public void onMouseHoverTile(InventoryItem me, GameCamera camera, PlayerMob perspective, int mouseX, int mouseY,
				TilePosition pos, boolean isDebug) {
			
			if(active && perspective!=null) {
				
				if(this.currentShape.shapeID == Shape.LINE) {
					ShapeSelectionLine cl = ((ShapeSelectionLine)this.currentShape);
					if(!perspective.isAttacking) {
						if(perspective.getDir() == 0 || perspective.getDir() == 2) {
							if(cl.direction!=LineDirection.VERTICAL) cl.direction = LineDirection.VERTICAL;			
						}
						else {
							if(cl.direction!=LineDirection.HORIZONTAL) cl.direction = LineDirection.HORIZONTAL;
						}
					}
					
				}
				
				currentlyHighlightedTiles = this.currentShape.getTilesAround(perspective, pos);
				TileItem tileInBucket = null;
				if(hasTile(me)) {
					tileInBucket = getCurrentTile(me);
				}
				
				this.highlightDraw = new TerraformerHighlightTileDrawable(
						perspective.getLevel(),
						perspective,
						camera,
						currentlyHighlightedTiles,
						tileInBucket,
						this.bucketRange);	
				
				highlightDraw.draw(perspective.getLevel().tickManager());
			}
			
		}
		
	
		
		private long last_switch;
		private boolean isSizeUpPressed = false;
		private boolean isSizeDownPressed = false;

		@Override
		public void tick(Inventory arg0, int arg1, InventoryItem me, GameClock arg3, GameState arg4, Entity arg5,
		                 WorldSettings arg6, Consumer<InventoryItem> arg7) {
		    super.tick(arg0, arg1, me, arg3, arg4, arg5, arg6, arg7);
		    
		    if (!(arg5 instanceof PlayerMob)) return;
		   
		    PlayerMob p = (PlayerMob) arg5;
		    if (p.getSelectedItem() == null) return;
		    
		    boolean itemSelected = p.getSelectedItem().item == this;
		    long currentTime = arg3.getTime();

		    // Handle activation toggle with a delay
		    if ((currentTime - last_switch) / 1000.0 > 0.5) {
		        if (itemSelected && !this.active) {
		            this.active = true;
		            last_switch = currentTime;
		            this.onActivateEvent();
		        }
		        if (!itemSelected && this.active) {
		            this.active = false;
		            last_switch = currentTime;
		            this.onDeactivateEvent();
		        }
		    }

		    // Handle key press events
		    boolean sizeUpPressed = Control.getControl("terraformersizeup").isDown() || Control.getControl("terraformersizeup").isPressed();
		    boolean sizeDownPressed = Control.getControl("terraformersizedown").isDown() || Control.getControl("terraformersizedown").isPressed();

		    if (sizeUpPressed && !isSizeUpPressed) {
		        modShapeSize(1);
		        isSizeUpPressed = true;
		    } else if (!sizeUpPressed) {
		        isSizeUpPressed = false;
		    }

		    if (sizeDownPressed && !isSizeDownPressed) {
		        modShapeSize(-1);
		        isSizeDownPressed = true;
		    } else if (!sizeDownPressed) {
		        isSizeDownPressed = false;
		    }
		}
		
		public void modShapeSize(int mod) {
			this.currentShapeSize += mod;
			this.currentShapeSize = Math.min(this.maxShapeSize, Math.max(this.minShapeSize, this.currentShapeSize));
			updateShapes();			
		}
		
		private void onDeactivateEvent() {			
			
		}

		private void onActivateEvent() {		
			
		}
		
		public class TerraformerHighlightTileDrawable extends SortedDrawable{

			private Level _level;
			private GameCamera _camera;
			private LevelTile[][] targetTiles;
			private PlayerMob perspective;
			private TileItem bucketTile;
			private int range;
			public TerraformerHighlightTileDrawable(Level _level, PlayerMob perspective, GameCamera _camera, LevelTile[][] tiles, TileItem bucketTile, int range) {
				this._level = _level;
				this._camera = _camera;
				this.targetTiles = tiles;
				this.perspective = perspective;
				this.range = range;
				this.bucketTile = bucketTile;
			}
			
			@Override
			public void draw(TickManager arg0) {
				
				for(int x=0;x<targetTiles.length;x++) {
					for(int y=0;y<targetTiles[x].length;y++) {
						
						LevelTile targetTile = targetTiles[x][y];
						if(targetTile==null) continue;
						int tileX = targetTile.tileX;
						int tileY = targetTile.tileY;	
						
						
						int highlightType = 0;
						
						if(x == 0 && y == 0) {
							highlightType = 3;
						}
						int dx = tileX - perspective.getTileX();
						int dy = tileY - perspective.getTileY();
						if((dx * dx + dy * dy) > (range * range)) {
							highlightType = 2;
						}
						else {
							
							if(this.bucketTile != null) {
								if(targetTile.tile.getID() != bucketTile.getID()) {
									highlightType = 1;
								}
							}
							
						}				
										
						GameLight light = this._level.getLightLevel(tileX, tileY);
						int drawX = this._camera.getTileDrawX(tileX);
						int drawY = this._camera.getTileDrawY(tileY);
						GameTexture texture =  highlightTexture;
						
						Color mult = null;
						if(highlightType == 3) mult = Color.YELLOW;
						if(highlightType == 2) mult = Color.RED;
						if(highlightType == 1) mult = Color.GREEN;
						
						TextureDrawOptionsEnd m = texture.initDraw().size(32, 32).light(light).alpha(.8F);
						if(mult != null) {
							m.colorMult(mult);
						}
						m.draw(drawX, drawY);
					}
				}
				
				
			}

			@Override
			public int getPriority() {
				return Integer.MAX_VALUE;
			}
		}
		
		public static abstract class ShapeSelection {			
			public final Shape shapeID;
			public final String shapeName; 
			protected int shapeSize;
			protected int maxSize;
			protected int minSize;
			public ShapeSelection(int size, Shape shapeID, String shapeName) {
				this.shapeSize = size;
				this.maxSize = 3;
				this.minSize = 1;
				this.shapeID = shapeID;
				this.shapeName = shapeName;
			}
			public void setSize(int i) {
				this.shapeSize = Math.min(maxSize, Math.max(minSize, i));
			}
			public void setMaxSize(int i) {
				this.maxSize = i;
			}
			public void setMinSize(int i) {
				this.minSize = i;
			}			
			public abstract LevelTile[][] getTilesAround(PlayerMob player, TilePosition p);
			
			public int getShapeSize() {
				return this.shapeSize;
			}
		}
		
		public static class ShapeSelectionSquare extends ShapeSelection{		
			
			public ShapeSelectionSquare(int size) {
				super(size, Shape.SQUARE, "square");				
			}

			@Override
			public LevelTile[][] getTilesAround(PlayerMob player, TilePosition p) {
			    Level l = player.getLevel();
			    if (shapeSize == 0) return new LevelTile[0][0];

			    LevelTile[][] shape = new LevelTile[shapeSize][shapeSize];

			    int startX = p.tileX - (shapeSize / 2);
			    int startY = p.tileY - (shapeSize / 2);
			    int endX = p.tileX + (shapeSize / 2) + (shapeSize % 2 == 0 ? -1 : 0);
			    int endY = p.tileY + (shapeSize / 2) + (shapeSize % 2 == 0 ? -1 : 0);

			    int _x = 0;
			    for (int x = startX; x <= endX; x++) { 
			        int _y = 0;
			        for (int y = startY; y <= endY; y++) { 
			            shape[_x][_y] = l.getLevelTile(x, y);
			            _y++;
			        }
			        _x++;
			    }
			    return shape;
			}
			
		}
		
		public static class ShapeSelectionCircle extends ShapeSelection {

		    public ShapeSelectionCircle(int size) {
		        super(size, Shape.CIRCLE, "circle");
		        this.minSize = 3;
		        this.maxSize = Math.max(maxSize, 3);
		    }

		    @Override
		    public LevelTile[][] getTilesAround(PlayerMob player, TilePosition p) {
		        Level l = player.getLevel();
		        if (shapeSize < 3) return new LevelTile[0][0];

		        int radius = shapeSize / 2;
		        int diameter = radius * 2 + 1;

		        LevelTile[][] shape = new LevelTile[diameter][diameter];

		        int centerX = p.tileX;
		        int centerY = p.tileY;

		        for (int x = -radius; x <= radius; x++) {
		            for (int y = -radius; y <= radius; y++) {
		                int worldX = centerX + x;
		                int worldY = centerY + y;

		                // Check if within circle equation
		                double distance = Math.sqrt(x * x + y * y);
		                if (distance <= radius) {
		                    int _x = x + radius;
		                    int _y = y + radius;

		                    if (_x >= 0 && _x < diameter && _y >= 0 && _y < diameter) {
		                        shape[_x][_y] = l.getLevelTile(worldX, worldY);
		                    }
		                }
		            }
		        }

		        return shape;
		    }
		}
		
		public static class ShapeSelectionRing extends ShapeSelection {

		    public ShapeSelectionRing(int size) {
		        super(size, Shape.RING, "ring");
		        this.minSize = 3; // Ring must have a minimum size to exist
		        this.maxSize = Math.max(maxSize, 3);
		    }

		    @Override
		    public LevelTile[][] getTilesAround(PlayerMob player, TilePosition p) {
		        Level l = player.getLevel();
		        if (shapeSize < 3) return new LevelTile[0][0];

		        int radius = shapeSize / 2;
		        int innerRadius = Math.max(1, radius - 1); // Ensures a hollow center
		        int diameter = radius * 2 + 1;

		        LevelTile[][] shape = new LevelTile[diameter][diameter];

		        int centerX = p.tileX;
		        int centerY = p.tileY;

		        for (int x = -radius; x <= radius; x++) {
		            for (int y = -radius; y <= radius; y++) {
		                int worldX = centerX + x;
		                int worldY = centerY + y;

		                // Compute distance from the center
		                double distance = Math.sqrt(x * x + y * y);

		                // Include only tiles between inner and outer radius
		                if (distance <= radius && distance >= innerRadius) {
		                    int _x = x + radius;
		                    int _y = y + radius;

		                    if (_x >= 0 && _x < diameter && _y >= 0 && _y < diameter) {
		                        shape[_x][_y] = l.getLevelTile(worldX, worldY);
		                    }
		                }
		            }
		        }

		        return shape;
		    }
		}
		
		public static class ShapeSelectionLine extends ShapeSelection {		  

		    private LineDirection direction;

		    public ShapeSelectionLine(int size, LineDirection direction) {
		        super(size, Shape.LINE, "line");
		        this.direction = direction;
		    }

		    @Override
		    public LevelTile[][] getTilesAround(PlayerMob player, TilePosition p) {
		        Level l = player.getLevel();
		        if (shapeSize == 0) return new LevelTile[0][0];

		        // Define array size properly based on direction
		        LevelTile[][] shape;
		        if (direction == LineDirection.HORIZONTAL) {
		            shape = new LevelTile[1][shapeSize]; // 1 row, shapeSize columns
		        } else if (direction == LineDirection.VERTICAL) {
		            shape = new LevelTile[shapeSize][1]; // shapeSize rows, 1 column
		        } else {
		            shape = new LevelTile[shapeSize][shapeSize]; // Diagonal case
		        }

		        int startX = p.tileX;
		        int startY = p.tileY;

		        for (int i = 0; i < shapeSize; i++) {
		            int x = startX;
		            int y = startY;

		            switch (direction) {
		                case HORIZONTAL:
		                    x = startX - (shapeSize / 2) + i;
		                    y = startY;
		                    if (i >= 0 && i < shapeSize) shape[0][i] = l.getLevelTile(x, y);
		                    break;

		                case VERTICAL:
		                    x = startX;
		                    y = startY - (shapeSize / 2) + i;
		                    if (i >= 0 && i < shapeSize) shape[i][0] = l.getLevelTile(x, y);
		                    break;

		                case DIAGONAL_TL_BR:
		                    x = startX - (shapeSize / 2) + i;
		                    y = startY - (shapeSize / 2) + i;
		                    if (i >= 0 && i < shapeSize) shape[i][i] = l.getLevelTile(x, y);
		                    break;

		                case DIAGONAL_TR_BL:
		                    x = startX + (shapeSize / 2) - i;
		                    y = startY - (shapeSize / 2) + i;
		                    if (i >= 0 && i < shapeSize) shape[i][shapeSize - i - 1] = l.getLevelTile(x, y);
		                    break;
		            }
		        }

		        return shape;
		    }

		    public void setDirection(LineDirection direction) {
		        this.direction = direction;
		    }

		    public LineDirection getDirection() {
		        return direction;
		    }
		}
		
	
		protected void openContainer(ServerClient client, PlayerInventorySlot inventorySlot) {
			PacketOpenContainer p = new PacketOpenContainer(Terraformer.TERRAFORMER_CONTAINER,
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

		@Override
		public void addUpgradeStatTips(ItemStatTipList arg0, InventoryItem arg1, InventoryItem arg2,
				ItemAttackerMob arg3, ItemAttackerMob arg4) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public UpgradedItem getUpgradedItem(InventoryItem arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		public int getCurrentShapeSize() {
			return this.currentShapeSize;
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
	}