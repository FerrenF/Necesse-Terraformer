package constructors.patch;

import java.util.ArrayList;
import java.util.function.Predicate;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import net.bytebuddy.asm.Advice;


@ModMethodPatch(target = Recipes.class, name = "getDefaultRecipes", arguments = {})
public class DefaultRecipeUpgradeStationPatch {
	
		public static final Predicate<Recipe> REMOVE_UPGRADESTATION = 
		    recipe -> "upgradestation".equals(recipe.resultStringID);
		@Advice.OnMethodExit
	    public static void onExit(@Advice.Return(readOnly = false) ArrayList<Recipe> out) {	      
			out.removeIf(REMOVE_UPGRADESTATION);
			out.add(0,new Recipe("upgradestation", 1, RecipeTechRegistry.TUNGSTEN_WORKSTATION,
					Recipes.ingredientsFromScript("{{demonicbar, 20}, {tungstenbar, 10}, {quartz, 3}}")));
	    }
	}
