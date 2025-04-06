package constructors.form;


import java.awt.Rectangle;
import java.util.Map.Entry;

import constructors.container.BuilderContainer;
import constructors.item.ConstructorItem;
import constructors.item.ConstructorItem.Shape;
import constructors.item.ConstructorItem.ShapeSelection;
import necesse.gfx.forms.presets.containerComponent.ContainerForm;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.client.Client;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormDropdownButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormIconButton;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.components.containerSlot.FormContainerSlot;
import necesse.gfx.forms.components.lists.FormIngredientRecipeList;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import necesse.gfx.ui.ButtonStateTextures;
import necesse.gfx.ui.GameInterfaceStyle;
import necesse.inventory.container.item.ItemInventoryContainer;

public class BuilderContainerForm<T extends BuilderContainer> extends ContainerForm<T> {
	public static ConstructorItem playerTerraformer;
	public static BuilderContainerForm instance;
	public FormTextInput searchFilter;
	public FormContainerSlot materialSlot;
	public FormIngredientRecipeList ingredientList;
	public int itemID;

	private FormIconButton iconMinusComponent;
	private FormIconButton iconPlusComponent;
	private FormLocalLabel shapeSizeLabelText;
	private FormDropdownButton shapeSelecter;
	public BuilderContainerForm(Client client, final T container) {		
		super(client, 300, 100, container);
		instance = this;
		GameInterfaceStyle DefaultGameInterfaceStyle = GameInterfaceStyle.getStyle(GameInterfaceStyle.defaultPath);
		ButtonStateTextures bst_Plus = new ButtonStateTextures(DefaultGameInterfaceStyle,"button_plus");
		ButtonStateTextures bst_Minus = new ButtonStateTextures(DefaultGameInterfaceStyle,"button_minus");
		
		this.addComponent(new FormLocalLabel(
				(GameMessage) new StaticMessage("Builder Settings"),
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
			playerTerraformer.modShapeSize(-1, container.getInventoryItem());
		});
		
		shapeSizeLabelText = new FormLocalLabel("shapesize", "0", new FontOptions(16), 0, 0, 0, 20);
		iconPlusComponent = new FormIconButton(0, 0, bst_Plus, 20, 20, new LocalMessage("terraformer", "increasesize"));
		iconPlusComponent.onClicked((event)->{
			if(playerTerraformer == null) return;
			playerTerraformer.modShapeSize(1, container.getInventoryItem());
		});
		
		
		this.shapeSelecter = new FormDropdownButton(25, this.getBoundingBox().height-30, FormInputSize.SIZE_16,
				ButtonColor.BASE, 200, new LocalMessage("terraformer", "shapeslector"));		
		
		for(Entry<Shape, ShapeSelection> s : playerTerraformer.shapes.entrySet()) {
			shapeSelecter.options.add(new LocalMessage("constructor.shapes", s.getValue().shapeName), ()->{
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
