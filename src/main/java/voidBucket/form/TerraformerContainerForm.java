package voidBucket.form;


import java.awt.Rectangle;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import necesse.gfx.forms.presets.containerComponent.ContainerForm;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Input;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.client.Client;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormDropdownButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormIconButton;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.components.containerSlot.FormContainerMaterialSlot;
import necesse.gfx.forms.components.containerSlot.FormContainerSlot;
import necesse.gfx.forms.components.lists.FormIngredientRecipeList;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.presets.containerComponent.ContainerForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.InputTooltip;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.ui.ButtonColor;
import necesse.gfx.ui.ButtonStateTextures;
import necesse.gfx.ui.GameInterfaceStyle;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.item.ItemInventoryContainer;
import necesse.inventory.container.item.RecipeBookContainer;
import necesse.inventory.item.ItemSearchTester;
import necesse.inventory.item.miscItem.InternalInventoryItemInterface;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import voidBucket.container.TerraformerContainer;
import voidBucket.item.TerraformerItem;
import voidBucket.item.TerraformerItem.Shape;
import voidBucket.item.TerraformerItem.ShapeSelection;

public class TerraformerContainerForm<T extends TerraformerContainer> extends ContainerForm<T> {
	public static TerraformerItem playerTerraformer;
	public static TerraformerContainerForm instance;
	public FormTextInput searchFilter;
	public FormContainerSlot materialSlot;
	public FormIngredientRecipeList ingredientList;
	public int itemID;

	private FormIconButton iconMinusComponent;
	private FormIconButton iconPlusComponent;
	private FormLocalLabel shapeSizeLabelText;
	private FormDropdownButton shapeSelecter;
	public TerraformerContainerForm(Client client, final T container) {		
		super(client, 300, 100, container);
		instance = this;
		GameInterfaceStyle DefaultGameInterfaceStyle = GameInterfaceStyle.getStyle(GameInterfaceStyle.defaultPath);
		ButtonStateTextures bst_Plus = new ButtonStateTextures(DefaultGameInterfaceStyle,"button_plus");
		ButtonStateTextures bst_Minus = new ButtonStateTextures(DefaultGameInterfaceStyle,"button_minus");
		
		this.addComponent(new FormLocalLabel(
				(GameMessage) new StaticMessage("Terraformer Settings"),
				new FontOptions(20), -1, 10, 10));
		
		this.addComponent(
				this.materialSlot = new FormContainerSlot(client,
						this.container,	
						((ItemInventoryContainer) this.container).INVENTORY_START,
						this.getWidth() - 60, this.getHeight() - 50));

		
		FormContentBox sizeAdjustmentWrapperBox = new FormContentBox(this.getX()+30, 50, 200, 20);		
		FormFlow flow = new FormFlow();
		FormLocalLabel adjustmentAreaText = new FormLocalLabel("terraformer", "shapeadjustment", new FontOptions(12), 40, 5, 0, 100);
		iconMinusComponent = new FormIconButton(0, 0, bst_Minus, 20, 20, new LocalMessage("terraformer", "decreasesize"));
		iconMinusComponent.onClicked((event)->{
			if(playerTerraformer == null) return;
			playerTerraformer.modShapeSize(-1);
		});
		
		shapeSizeLabelText = new FormLocalLabel("shapesize", "0", new FontOptions(16), 0, 0, 0, 20);
		iconPlusComponent = new FormIconButton(0, 0, bst_Plus, 20, 20, new LocalMessage("terraformer", "increasesize"));
		iconPlusComponent.onClicked((event)->{
			if(playerTerraformer == null) return;
			playerTerraformer.modShapeSize(1);
		});
		
		
		this.shapeSelecter = new FormDropdownButton(25, this.getBoundingBox().height-30, FormInputSize.SIZE_16,
				ButtonColor.BASE, 200, new LocalMessage("terraformer", "shapeslector"));		
		
		for(Entry<Shape, ShapeSelection> s : TerraformerItem.shapes.entrySet()) {
			shapeSelecter.options.add(new LocalMessage("terraformer.shapes", s.getValue().shapeName), ()->{
				playerTerraformer.setShape(s.getKey());
			});
		}
		
		sizeAdjustmentWrapperBox.addComponent(flow.nextX(adjustmentAreaText));
		sizeAdjustmentWrapperBox.addComponent(flow.nextX((FormIconButton)iconMinusComponent, 10));
		sizeAdjustmentWrapperBox.addComponent(flow.nextX(shapeSizeLabelText));
		sizeAdjustmentWrapperBox.addComponent(flow.nextX((FormIconButton)iconPlusComponent, 10));
		this.addComponent(sizeAdjustmentWrapperBox);
		this.addComponent(shapeSelecter);
		updateShapeSizeLabel(playerTerraformer.getCurrentShapeSize());
	}
	
	public void updateShapeSizeLabel(int currentShapeSize) {
		if(playerTerraformer == null)return;
		this.shapeSizeLabelText.setText(String.valueOf(currentShapeSize));
	}

	public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
		
		

		super.draw(tickManager, perspective, renderBox);
	}
}
