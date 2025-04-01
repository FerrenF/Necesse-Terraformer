package constructors.drawables;

import java.awt.Color;
import java.util.function.BiFunction;
import java.util.function.Function;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.LevelTile;
import necesse.level.maps.light.GameLight;

public class ConstructorTileDrawable<T> extends SortedDrawable{
	
		public static enum TileHighlightType{
			OUT_OF_RANGE,
			SAMPLER_TILE,
			PAINTABLE_TILE,
			ALREADY_PAINTED_TILE
		}
	
		public static GameTexture highlightTexture;
		private Level _level;
		private GameCamera _camera;
		private LevelTile[][] targetTiles;
		private PlayerMob perspective;
		private int inBucketID = -1;
		private int maxRange;
		
		public TileDrawableOptions perTileDrawStep;
		
		private BiFunction<T, Integer, Boolean> isTilePred;
		private Function<LevelTile, T> targetTileComparisonObjectGetter;
		
		public ConstructorTileDrawable(Level _level,
				PlayerMob perspective,
				GameCamera _camera,
				LevelTile[][] tiles,
				int inBucketID,
				int maxRange,
				Function<LevelTile, T> targetTileComparisonObjectGetter,
				BiFunction<T, Integer, Boolean> isCurrentTile) {
			
		
			this._level = _level;
			this._camera = _camera;
			this.targetTiles = tiles;
			this.perspective = perspective;
			this.maxRange = maxRange;
			this.inBucketID = inBucketID;
			this.isTilePred = isCurrentTile;
			this.targetTileComparisonObjectGetter = targetTileComparisonObjectGetter;
		}
		
		
		public ConstructorTileDrawable highlightTexture(GameTexture texture) {
			highlightTexture = texture;
			return this;
		}
		
		@Override
		public void draw(TickManager arg0) {
			
			for(int x=0;x<targetTiles.length;x++) {
				for(int y=0;y<targetTiles[x].length;y++) {
					
					LevelTile targetTile = targetTiles[x][y];
					if(targetTile==null) continue;
					int tileX = targetTile.tileX;
					int tileY = targetTile.tileY;	
					
					
					TileHighlightType highlightType = TileHighlightType.ALREADY_PAINTED_TILE;
					
					if(x == 0 && y == 0) {
						highlightType = TileHighlightType.SAMPLER_TILE;
					}
					
					int dx = tileX - perspective.getTileX();
					int dy = tileY - perspective.getTileY();
					
					if((dx * dx + dy * dy) > (maxRange * maxRange)) {
						highlightType = TileHighlightType.OUT_OF_RANGE;
					}	else {
						
						if(this.inBucketID != -1) {							
							if(this.isTilePred.apply(targetTileComparisonObjectGetter.apply(targetTile), this.inBucketID)){
								highlightType = TileHighlightType.PAINTABLE_TILE;
							}
						}
						
					}				
									
					GameLight light = this._level.getLightLevel(tileX, tileY);
					int drawX = this._camera.getTileDrawX(tileX);
					int drawY = this._camera.getTileDrawY(tileY);
					GameTexture texture =  highlightTexture;
					
					Color mult = null;
					if(highlightType == TileHighlightType.SAMPLER_TILE) mult = Color.YELLOW;
					if(highlightType == TileHighlightType.OUT_OF_RANGE) mult = Color.RED;
					if(highlightType == TileHighlightType.PAINTABLE_TILE) mult = Color.GREEN;
					
					TextureDrawOptionsEnd m = texture.initDraw().size(32, 32).light(light).alpha(.8F);
					
					if(mult != null) {
						m.colorMult(mult);
					}
				
					m.draw(drawX, drawY);
					
					if(this.perTileDrawStep!=null) {
						this.perTileDrawStep.draw(_level, perspective, targetTile, highlightType);
					}
				}
			}
			
			
		}

		@Override
		public int getPriority() {
			return Integer.MAX_VALUE;
		}
	
		public interface TileDrawableOptions{
			void draw(Level level, PlayerMob perspective, LevelTile tile, TileHighlightType highlightType);
		}
}
