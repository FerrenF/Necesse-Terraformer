package constructors.item;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import necesse.engine.GameState;
import necesse.engine.input.Control;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.world.GameClock;
import necesse.engine.world.WorldSettings;
import necesse.entity.Entity;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.SortedDrawable;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.ItemInteractAction;
import necesse.inventory.item.TickItem;
import necesse.inventory.item.miscItem.PouchItem;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;
import necesse.inventory.item.upgradeUtils.UpgradableItem;
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
	
	public Map<Shape, ShapeSelection> shapes = new HashMap<Shape, ShapeSelection>();
	
	protected boolean shapes_initialized = false;
	
	private long lastActivationEventTime;
	protected boolean active = false;
	
	protected SortedDrawable highlightDraw;
	
	protected LevelTile[][] currentlyHighlightedTiles;
	protected ShapeSelection currentShape;
	
	protected int maxPlacementRange = 14;
	protected int maxShapeSize = 10;
	protected int minShapeSize = 1;
	protected int shapeSize = 4;
	
	public ConstructorItem() {
		
		super();	
		
		initializeShapes();
		this.stackSize = 1;			
		this.attackCooldownTime = new IntUpgradeValue().setBaseValue(100);			
		this.rarity = Rarity.UNIQUE;
		
		this.setShape(Shape.SQUARE);		
		
		updateShapes();

	}
	
	private void updateShapes() {
		this.setShapeMaxSize(maxShapeSize);
		this.setShapeSize(shapeSize);
		this.updateContainerForm();	
	}
	
	protected abstract void updateContainerForm();
	
	private void setShapeSize(int i) {
		this.shapeSize = i;
	}
	
	private void setShapeMaxSize(int i) {
		this.maxShapeSize = i;
	}
	
	public int getCurrentShapeSize() {
		return this.shapeSize;
	}
	
	public void modShapeSize(int mod) {
		this.shapeSize += mod;
		this.shapeSize = Math.min(this.maxShapeSize, Math.max(this.minShapeSize, this.shapeSize));
		updateShapes();			
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
		
		public int maxSize() 	{	return item != null ? item.maxShapeSize 	: 10;		}
		
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
