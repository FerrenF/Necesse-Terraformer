package constructors.item;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

import necesse.engine.GameState;
import necesse.engine.input.Control;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.util.GameBlackboard;
import necesse.engine.world.GameClock;
import necesse.engine.world.WorldSettings;
import necesse.entity.Entity;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameColor;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.ItemInteractAction;
import necesse.inventory.item.ItemStatTip;
import necesse.inventory.item.ItemStatTipList;
import necesse.inventory.item.LocalMessageDoubleItemStatTip;
import necesse.inventory.item.TickItem;
import necesse.inventory.item.miscItem.PouchItem;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;
import necesse.inventory.item.upgradeUtils.UpgradableItem;
import necesse.inventory.item.upgradeUtils.UpgradedItem;
import necesse.inventory.recipe.Ingredient;
import necesse.level.maps.Level;
import necesse.level.maps.LevelTile;
import necesse.level.maps.TilePosition;

public abstract class ConstructorItem extends PouchItem implements TickItem, UpgradableItem, ItemInteractAction {

	public static enum Shape {
		SQUARE,
		CHECKERBOARD,
		LINE,
		CIRCLE,
		RING, LINE_BOX
	}
	
	public static enum LineDirection {
        HORIZONTAL, 
        VERTICAL,
        DIAGONAL_TL_BR,
        DIAGONAL_TR_BL
    }
	
	public static final int MAX_UPGRADE_TIER = 5;
	
	public Map<Shape, ShapeSelection> shapes = new HashMap<Shape, ShapeSelection>();
	
	protected boolean shapes_initialized = false;
	
	private long lastActivationEventTime;
	protected boolean active = false;
	
	protected SortedDrawable highlightDraw;
	
	protected LevelTile[][] currentlyHighlightedTiles;
	protected ShapeSelection currentShape;
	
	protected IntUpgradeValue maxPlacementRange;
	protected IntUpgradeValue maxShapeSize;
	protected int minShapeSize = 1;
	protected int shapeSize = 4;
	
	public ConstructorItem() {
		
		super();	
		
		initializeShapes();
		
		this.maxPlacementRange = new IntUpgradeValue(12, 0.5F);
		this.maxShapeSize = new IntUpgradeValue(6, 0.4F);
		
		this.stackSize = 1;			
		this.attackCooldownTime = new IntUpgradeValue().setBaseValue(100);			
		this.rarity = Rarity.UNIQUE;
		
		this.setShape(Shape.SQUARE);		
		
		updateShapes(null);

	}

	public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective,
			GameBlackboard blackboard) {
		ListGameTooltips tooltips = new ListGameTooltips();
		ItemAttackerMob equippedMob = (ItemAttackerMob) blackboard.get(ItemAttackerMob.class, "equippedMob",
				perspective);
		if (equippedMob == null) {
			equippedMob = (ItemAttackerMob) blackboard.get(ItemAttackerMob.class, "perspective", perspective);
		}

		if (equippedMob == null) {
			equippedMob = perspective;
		}
		tooltips.add(new necesse.gfx.gameTooltips.SpacerGameTooltip(12));
		this.addStatTooltips(tooltips, item, (InventoryItem) blackboard.get(InventoryItem.class, "compareItem"),
				blackboard.getBoolean("showDifference"), blackboard.getBoolean("forceAdd"),
				(ItemAttackerMob) equippedMob);
		tooltips.add(new necesse.engine.localization.message.LocalMessage("constructor.ui","itemupgradeable"));
		return tooltips;
	}

	public ListGameTooltips getPostEnchantmentTooltips(InventoryItem item, PlayerMob perspective,
			GameBlackboard blackboard) {
		return new ListGameTooltips();
	}

	public final ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
		ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
		tooltips.add(this.getPreEnchantmentTooltips(item, perspective, blackboard));
		//tooltips.add(this.getEnchantmentTooltips(item));
		tooltips.add(this.getPostEnchantmentTooltips(item, perspective, blackboard));
		return tooltips;
	}
	
	public String getCanBeUpgradedError(InventoryItem item) {			
		return this.getUpgradeTier(item) >= (float) MAX_UPGRADE_TIER
				? Localization.translate("constructor.ui", "itemupgradelimit")
				: null;
	}

	@Override
	public void addUpgradeStatTips(ItemStatTipList list, InventoryItem lastItem, InventoryItem upgradedItem,
			ItemAttackerMob perspective, ItemAttackerMob statPerspective) {
		
		ItemStatTip tierTip = (new LocalMessageDoubleItemStatTip("item", "tier", "tiernumber",
				(double) this.getUpgradeTier(upgradedItem), 2)).setCompareValue((double) this.getUpgradeTier(lastItem))
				.setToString((tier) -> {
					int floorTier = (int) tier;
					double percentAdd = tier - (double) floorTier;
					return percentAdd != 0.0
							? floorTier + " (+" + (int) (percentAdd * 100.0) + "%)"
							: String.valueOf(floorTier);
				});		
		list.add(Integer.MIN_VALUE, tierTip);
		this.addStatTooltips(list, upgradedItem, lastItem, perspective, true);
		
	}
	
	public final void addStatTooltips(ListGameTooltips tooltips, InventoryItem currentItem, InventoryItem lastItem,
			boolean showDifference, boolean forceAdd, ItemAttackerMob perspective) {
		ItemStatTipList list = new ItemStatTipList();
		this.addStatTooltips(list, currentItem, lastItem, perspective, forceAdd);
		Iterator<ItemStatTip> var8 = list.iterator();
		while (var8.hasNext()) {
			ItemStatTip itemStatTip = (ItemStatTip) var8.next();
			tooltips.add(itemStatTip.toTooltip((Color) GameColor.GREEN.color.get(), (Color) GameColor.RED.color.get(),
					(Color) GameColor.YELLOW.color.get(), showDifference));
		}

	}
	
	public void addToolTierTip(ItemStatTipList list, InventoryItem currentItem, InventoryItem lastItem,
			boolean forceAdd) {
		float tier = this.getUpgradeTier(currentItem);
		float lastTier = lastItem == null ? tier : this.getUpgradeTier(lastItem);
		if (tier != lastTier || forceAdd) {
			LocalMessageDoubleItemStatTip tip = new LocalMessageDoubleItemStatTip("itemtooltip", "tooltier", "value", (double) tier,
					1);
			if (lastItem != null) {
				tip.setCompareValue((double) lastTier);
			}

			list.add(60, tip);
		}

	}

	public void addMaxRangeTip(ItemStatTipList list, InventoryItem currentItem, InventoryItem lastItem,
			Mob perspective, boolean forceAdd) {
		
		int currentMaxRange = this.maxPlacementRange.getValue(this.getUpgradeTier(currentItem));
		LocalMessageDoubleItemStatTip tip = new LocalMessageDoubleItemStatTip("constructor.itemtooltip", "rangetip", "value",
			currentMaxRange, 1);
		
		if (lastItem != null) {
			int lastMaxRange = this.maxPlacementRange.getValue(this.getUpgradeTier(lastItem));
			tip.setCompareValue(lastMaxRange);
		}

		list.add(250, tip);
	}
	
	public void addMaxSizeTip(ItemStatTipList list, InventoryItem currentItem, InventoryItem lastItem,
			Mob perspective, boolean forceAdd) {
		
		int currentMaxSize = this.maxShapeSize.getValue(this.getUpgradeTier(currentItem));
		LocalMessageDoubleItemStatTip tip = new LocalMessageDoubleItemStatTip("constructor.itemtooltip", "sizetip", "value",
				currentMaxSize, 1);
		
		if (lastItem != null) {
			int lastMaxSize = this.maxShapeSize.getValue(this.getUpgradeTier(lastItem));
			tip.setCompareValue(lastMaxSize);
		}

		list.add(250, tip);
	}
	
	public void addStatTooltips(ItemStatTipList list, InventoryItem currentItem, InventoryItem lastItem,
			ItemAttackerMob perspective, boolean forceAdd) {
		
		this.addMaxRangeTip(list, currentItem, lastItem, perspective, forceAdd);
		this.addMaxSizeTip(list, currentItem, lastItem, perspective, forceAdd);
	}
	
	
	
	protected int getNextUpgradeTier(InventoryItem item) {
		int currentTier = (int) item.item.getUpgradeTier(item);
		int nextTier = currentTier + 1;
		
		float baseSizeValue = this.maxShapeSize.getValue(0.0F);
		float nextTierValue = this.maxShapeSize.getValue((float) nextTier);
		
		if (nextTier == 1 && baseSizeValue < nextTierValue) {
			return nextTier;
		} else {
			while (baseSizeValue / nextTierValue > 1.0F - this.maxShapeSize.defaultLevelIncreaseMultiplier / 4.0F
					&& nextTier < currentTier + 100) {
				++nextTier;
				nextTierValue = this.maxShapeSize.getValue((float) nextTier);
			}

			return nextTier;
		}
	}
	
	@Override
	public UpgradedItem getUpgradedItem(InventoryItem item) {
		
		int nextTier = this.getNextUpgradeTier(item);
		InventoryItem upgradedItem = item.copy();
		upgradedItem.item.setUpgradeTier(upgradedItem, (float) nextTier);

		return new UpgradedItem(item, upgradedItem, this.getSpecialUpgradeCost(nextTier));
	}
	
	protected abstract Ingredient[] getSpecialUpgradeCost(int nextTier);

	protected float getTier1CostPercent(InventoryItem item) {
		return this.maxShapeSize.getValue(0.0F) / this.maxShapeSize.getValue(1.0F);
	}
	
	private void updateShapes(InventoryItem _me) {
		this.setShapeMaxSize(_me != null ? maxShapeSize.getValue(this.getUpgradeTier(_me)) : maxShapeSize.defaultValue);
		this.setShapeSize(shapeSize);
		this.updateContainerForm();	
	}
	
	protected abstract void updateContainerForm();
	
	private void setShapeSize(int i) {
		this.shapeSize = i;
	}
	
	private void setShapeMaxSize(int i) {
		this.maxShapeSize.setBaseValue(i);
	}
	
	public int getCurrentShapeSize() {
		return this.shapeSize;
	}
	
	public void modShapeSize(int mod, InventoryItem _me) {
		this.shapeSize += mod;
		this.shapeSize = Math.min(this.maxShapeSize.getValue(this.getUpgradeTier(_me)), Math.max(this.minShapeSize, this.shapeSize));
		updateShapes(_me);			
	}
	
	public abstract void initializeShapes();
	
	public void setShape(Shape shapeID) {
		this.currentShape = shapes.get(shapeID);	
	}

	protected void onDeactivateEvent() {}

	protected void onActivateEvent() {}
	
	protected void clearOutOfRangeTiles(LevelTile[][] cloneTiles, PlayerMob perspective, int _range) {
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
	
	@Override
	public abstract InventoryItem onAttack(Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight,
			InventoryItem me, ItemAttackSlot slot, int animAttack, int seed, GNDItemMap mapContent);		

	@Override
	public abstract InventoryItem onLevelInteract(Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight,
			InventoryItem me, ItemAttackSlot slot, int seed, GNDItemMap mapContent);
	
	@Override
	public boolean canLevelInteract(Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item) {
		return true;
	}
	
	@Override
	public abstract void onMouseHoverTile(InventoryItem me, GameCamera camera, PlayerMob perspective, int mouseX, int mouseY,
			TilePosition pos, boolean isDebug);
	
	
	
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
	    if ((currentTime - lastActivationEventTime) / 1000.0 > 0.5) {
	        if (itemSelected && !this.active) {
	            this.active = true;
	            lastActivationEventTime = currentTime;
	            this.onActivateEvent();
	        }
	        if (!itemSelected && this.active) {
	            this.active = false;
	            lastActivationEventTime = currentTime;
	            this.onDeactivateEvent();
	        }
	    }
	    
	    // Handle key press events
	    boolean sizeUpPressed = Control.getControl("terraformersizeup").isDown() || Control.getControl("terraformersizeup").isPressed();
	    boolean sizeDownPressed = Control.getControl("terraformersizedown").isDown() || Control.getControl("terraformersizedown").isPressed();

	    if (sizeUpPressed && !isSizeUpPressed) {
	        modShapeSize(1, me);
	        isSizeUpPressed = true;
	    } else if (!sizeUpPressed) {
	        isSizeUpPressed = false;
	    }

	    if (sizeDownPressed && !isSizeDownPressed) {
	        modShapeSize(-1, me);
	        isSizeDownPressed = true;
	    } else if (!sizeDownPressed) {
	        isSizeDownPressed = false;
	    }
	}
	
	public static abstract class ShapeSelection {			
		public final Shape shapeID;
		public final String shapeName; 

		protected ConstructorItem item;
		public ShapeSelection(ConstructorItem item, Shape shapeID, String shapeName) {
			this.shapeID = shapeID;
			this.shapeName = shapeName;
			this.item = item;
		}
		
		public int shapeSize() 	{	return item != null ? item.shapeSize : 3;		}
		
		public int maxSize(InventoryItem _me) 	{	return item != null ? item.maxShapeSize.getValue(item.getUpgradeTier(_me)) 	: 10;		}
		
		public int minSize() 	{	return item != null ? item.minShapeSize 	: 1;		}
			
		public abstract LevelTile[][] getTilesAround(PlayerMob player, TilePosition p);
		
	}
	
	public static class ShapeSelectionSquare extends ShapeSelection{		
		
		public ShapeSelectionSquare(ConstructorItem item) {		super(item, Shape.SQUARE, "square");	}

		@Override
		public LevelTile[][] getTilesAround(PlayerMob player, TilePosition p) {
			
			int currentSize = shapeSize();
		    if (shapeSize() == 0 || player == null) return new LevelTile[0][0];
		    
		    Level l = player.getLevel();
		    LevelTile[][] shape = new LevelTile[currentSize][currentSize];

		    int startX = p.tileX - (currentSize / 2);
		    int startY = p.tileY - (currentSize / 2);
		    int endX = p.tileX + (currentSize / 2) + (currentSize % 2 == 0 ? -1 : 0);
		    int endY = p.tileY + (currentSize / 2) + (currentSize % 2 == 0 ? -1 : 0);

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
	
	public static class ShapeSelectionLineBox extends ShapeSelection {        
	    
	    public ShapeSelectionLineBox(ConstructorItem item) {        
	        super(item, Shape.LINE_BOX, "linebox");    
	    }

	    @Override
	    public LevelTile[][] getTilesAround(PlayerMob player, TilePosition p) {
	        int currentSize = shapeSize();
	        if (currentSize == 0 || player == null) return new LevelTile[0][0];
	        
	        Level l = player.getLevel();
	        LevelTile[][] shape = new LevelTile[currentSize][currentSize];

	        int startX = p.tileX - (currentSize / 2);
	        int startY = p.tileY - (currentSize / 2);
	        int endX = p.tileX + (currentSize / 2) + (currentSize % 2 == 0 ? -1 : 0);
	        int endY = p.tileY + (currentSize / 2) + (currentSize % 2 == 0 ? -1 : 0);

	        int _x = 0;
	        for (int x = startX; x <= endX; x++) { 
	            int _y = 0;
	            for (int y = startY; y <= endY; y++) { 
	                // Only select border tiles (top, bottom, left, right)
	                if (x == startX || x == endX || y == startY || y == endY) {
	                    shape[_x][_y] = l.getLevelTile(x, y);
	                } else {
	                    shape[_x][_y] = null; // Keep the inner part empty
	                }
	                _y++;
	            }
	            _x++;
	        }
	        return shape;
	    }
	}

	
	public static class ShapeSelectionCheckerboard extends ShapeSelection {
	    
	    public ShapeSelectionCheckerboard(ConstructorItem item) {
	        super(item, Shape.CHECKERBOARD, "checkerboard");
	    }

	    @Override
	    public LevelTile[][] getTilesAround(PlayerMob player, TilePosition p) {
	        int currentSize = shapeSize();
	        if (currentSize == 0 || player == null) return new LevelTile[0][0];
	        
	        Level l = player.getLevel();
	        LevelTile[][] shape = new LevelTile[currentSize][currentSize];
	        
	        int startX = p.tileX - (currentSize / 2);
	        int startY = p.tileY - (currentSize / 2);
	        int endX = p.tileX + (currentSize / 2) + (currentSize % 2 == 0 ? -1 : 0);
	        int endY = p.tileY + (currentSize / 2) + (currentSize % 2 == 0 ? -1 : 0);
	        
	        int _x = 0;
	        for (int x = startX; x <= endX; x++) {
	            int _y = 0;
	            for (int y = startY; y <= endY; y++) {
	                if ((_x + _y) % 2 == 0) {
	                    shape[_x][_y] = l.getLevelTile(x, y);
	                } else {
	                    shape[_x][_y] = null;
	                }
	                _y++;
	            }
	            _x++;
	        }
	        return shape;
	    }
	}

	public static class ShapeSelectionCircle extends ShapeSelection {

	    public ShapeSelectionCircle(ConstructorItem item) {
	        super(item, Shape.CIRCLE, "circle");	  
	    }
	    
	    @Override
	    public LevelTile[][] getTilesAround(PlayerMob player, TilePosition p) {
	    	
	    	int currentSize = shapeSize();
	        if (shapeSize() < 3) return new LevelTile[0][0];	        
	        
		    Level l = player.getLevel();
	        int radius = currentSize / 2;
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

	    public ShapeSelectionRing(ConstructorItem item) {
	        super(item, Shape.RING, "ring");
	    }
	    
	    @Override
	    public int minSize() 	{	return this.item != null ? Math.max(3, this.item.minShapeSize) 	: 3;		}

	    @Override
	    public LevelTile[][] getTilesAround(PlayerMob player, TilePosition p) {
	    	
	    	int currentSize = shapeSize();
	        if (currentSize < 3) return new LevelTile[0][0];
	        Level l = player.getLevel();

	        int radius = currentSize / 2;
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

	    public ShapeSelectionLine(ConstructorItem item, LineDirection direction) {
	        super(item, Shape.LINE, "line");
	        this.direction = direction;
	    }

	    @Override
	    public LevelTile[][] getTilesAround(PlayerMob player, TilePosition p) {
	    	
	    	int currentSize = shapeSize();
	        if (currentSize == 0) return new LevelTile[0][0];	        
	        Level l = player.getLevel();
	        
	        LevelTile[][] shape;
	        if (direction == LineDirection.HORIZONTAL) {
	            shape = new LevelTile[1][currentSize]; 
	        } else if (direction == LineDirection.VERTICAL) {
	            shape = new LevelTile[currentSize][1]; 
	        } else {
	            shape = new LevelTile[currentSize][currentSize]; 
	        }

	        int startX = p.tileX;
	        int startY = p.tileY;

	        for (int i = 0; i < currentSize; i++) {
	            int x = startX;
	            int y = startY;

	            switch (direction) {
	                case HORIZONTAL:
	                    x = startX - (currentSize / 2) + i;
	                    y = startY;
	                    if (i >= 0 && i < currentSize) shape[0][i] = l.getLevelTile(x, y);
	                    break;

	                case VERTICAL:
	                    x = startX;
	                    y = startY - (currentSize / 2) + i;
	                    if (i >= 0 && i < currentSize) shape[i][0] = l.getLevelTile(x, y);
	                    break;

	                case DIAGONAL_TL_BR:
	                    x = startX - (currentSize / 2) + i;
	                    y = startY - (currentSize / 2) + i;
	                    if (i >= 0 && i < currentSize) shape[i][i] = l.getLevelTile(x, y);
	                    break;

	                case DIAGONAL_TR_BL:
	                    x = startX + (currentSize / 2) - i;
	                    y = startY - (currentSize / 2) + i;
	                    if (i >= 0 && i < currentSize) shape[i][currentSize - i - 1] = l.getLevelTile(x, y);
	                    break;
	            }
	        }

	        return shape;
	    }

	    public ShapeSelectionLine setDirection(LineDirection direction) {
	        this.direction = direction;
	        return this;
	    }

	    public LineDirection getDirection() {
	        return direction;
	    }
	}
}
