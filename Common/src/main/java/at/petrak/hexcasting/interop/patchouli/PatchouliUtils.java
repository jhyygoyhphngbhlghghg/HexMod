package at.petrak.hexcasting.interop.patchouli;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import vazkii.patchouli.api.IVariable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * > no this is a "literally copy these files/parts of file into your mod"
 * > we should put this in patchy but lol
 * > lazy
 * -- Hubry Vazcord
 */
public class PatchouliUtils {
    /** Uses current client level. Prefer {@link #getRecipe(Level, RecipeType, ResourceLocation)} when a level is available (e.g. from Patchouli). */
    @SuppressWarnings("unchecked")
    public static <T extends Recipe<?>> T getRecipe(RecipeType<T> type, ResourceLocation id) {
        Level level = Minecraft.getInstance().level;
        return level == null ? null : getRecipe(level, type, id);
    }

    /** Prefer server recipe manager when level has one (singleplayer); fallback to level's manager and to getAllRecipesFor lookup. */
    @SuppressWarnings("unchecked")
    public static <T extends Recipe<?>> T getRecipe(Level level, RecipeType<T> type, ResourceLocation id) {
        if (level == null) return null;
        RecipeManager manager = (level.getServer() != null) ? level.getServer().getRecipeManager() : level.getRecipeManager();
        T recipe = getRecipeFromManager(type, id, manager);
        if (recipe == null && level.getServer() != null) {
            recipe = getRecipeFromManager(type, id, level.getRecipeManager());
        }
        return recipe;
    }

    /** Look up recipe by id; tries byKey then getAllRecipesFor+filter in case registry keying differs. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends Recipe<?>> T getRecipeFromManager(RecipeType<T> type, ResourceLocation id, RecipeManager manager) {
        T recipe = (T) manager.byKey(id)
            .filter((holder) -> holder.value().getType() == type)
            .map(h -> (T) h.value())
            .orElse(null);
        if (recipe == null) {
            for (RecipeHolder<?> h : (List<RecipeHolder<?>>) (List<?>) manager.getAllRecipesFor((RecipeType) type)) {
                if (h.id().equals(id) && h.value().getType() == type) {
                    return (T) h.value();
                }
            }
        }
        return recipe;
    }

    /**
     * Combines the ingredients, returning the first matching stack of each, then the second stack of each, etc.
     * looping back ingredients that run out of matched stacks, until the ingredients reach the length
     * of the longest ingredient in the recipe set.
     *
     * @param ingredients           List of ingredients in the specific slot
     * @param longestIngredientSize Longest ingredient in the entire recipe
     * @param registries            HolderLookup.Provider for serialization
     * @return Serialized Patchouli ingredient string
     */
    public static IVariable interweaveIngredients(List<Ingredient> ingredients, int longestIngredientSize, HolderLookup.Provider registries) {
        if (ingredients.size() == 1) {
            return IVariable.wrapList(Arrays.stream(ingredients.get(0).getItems()).map(s -> IVariable.from(s, registries)).collect(
                Collectors.toList()), registries);
        }

        ItemStack[] empty = {ItemStack.EMPTY};
        List<ItemStack[]> stacks = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            if (ingredient != null && !ingredient.isEmpty()) {
                stacks.add(ingredient.getItems());
            } else {
                stacks.add(empty);
            }
        }
        List<IVariable> list = new ArrayList<>(stacks.size() * longestIngredientSize);
        for (int i = 0; i < longestIngredientSize; i++) {
            for (ItemStack[] stack : stacks) {
                list.add(IVariable.from(stack[i % stack.length], registries));
            }
        }
        return IVariable.wrapList(list, registries);
    }

    /**
     * Overload of the method above that uses the provided list's longest ingredient size.
     */
    public static IVariable interweaveIngredients(List<Ingredient> ingredients, HolderLookup.Provider registries) {
        return interweaveIngredients(ingredients,
            ingredients.stream().mapToInt(ingr -> ingr.getItems().length).max().orElse(1), registries);
    }
}
