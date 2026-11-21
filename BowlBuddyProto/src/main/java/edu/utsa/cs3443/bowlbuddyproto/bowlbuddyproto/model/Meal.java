package edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model;

import java.time.LocalDate;

/**
 * Represents one logged meal for a pet.
 * Stores the recipe name and calories at the time the meal was logged,
 * so later recipe edits or deletions do not change past meals.
 */
public class Meal {

    private final String mealId;
    private final String petId;
    private final LocalDate date;
    private final String recipeId;
    private final String recipeName;
    private final int grams;
    private final int calories;

    /**
     * Creates a meal using the fields from one CSV record.
     */
    public Meal(String mealId, String petId, LocalDate date,
                String recipeId, String recipeName, int grams, int calories) {
        this.mealId = mealId;
        this.petId = petId;
        this.date = date;
        this.recipeId = recipeId;
        this.recipeName = recipeName;
        this.grams = grams;
        this.calories = calories;
    }

    public String getMealId() { return mealId; }
    public String getPetId() { return petId; }
    public LocalDate getDate() { return date; }
    public String getRecipeId() { return recipeId; }
    public String getRecipeName() { return recipeName; }
    public int getGrams() { return grams; }
    public int getCalories() { return calories; }

    /**
     * Shows the recipe, portion, and calories when it appears in a list.
     */
    @Override
    public String toString() {
        return recipeName + " — " + grams + " g — " + calories + " kcal";
    }
}
