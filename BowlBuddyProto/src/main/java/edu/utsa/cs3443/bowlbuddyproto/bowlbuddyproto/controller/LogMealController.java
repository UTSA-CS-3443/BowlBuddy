package edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.controller;

import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.HelloApplication;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.data.DataManager;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model.Meal;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model.Pet;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model.Recipe;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model.SessionManager;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Controls the Log Meal form.
 * Calculates calories from the chosen recipe and delegates storage to DataManager.
 */
public class LogMealController {

    // Controls injected from log-meal-view.fxml.
    @FXML private Label titleLabel;
    @FXML private ComboBox<Recipe> recipeComboBox;
    @FXML private TextField gramsField;
    @FXML private DatePicker datePicker;
    @FXML private Label caloriesLabel;
    @FXML private Label messageLabel;

    private final DataManager dataManager = new DataManager();

    // Prevents saving the same meal twice if navigation fails after saving.
    private boolean mealSaved;

    /**
     * Loads the recipe choices and defaults the date to today.
     */
    @FXML
    private void initialize() {
        Pet pet = SessionManager.getInstance().getCurrentPet();
        if (pet != null) {
            titleLabel.setText("Log a Meal for " + pet.getName());
        }

        datePicker.setValue(LocalDate.now());

        // Recalculate the calorie preview whenever the recipe or portion changes.
        recipeComboBox.valueProperty().addListener(
                (observable, previous, selected) -> updateCaloriesPreview());
        gramsField.textProperty().addListener(
                (observable, previous, text) -> updateCaloriesPreview());

        try {
            recipeComboBox.getItems().setAll(dataManager.loadRecipes());

            if (recipeComboBox.getItems().isEmpty()) {
                messageLabel.setText(
                        "Add a recipe to the Recipe Library before logging meals.");
            }
        } catch (IOException exception) {
            messageLabel.setText(
                    "Unable to load recipes: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    /**
     * Shows the calories for the current recipe and portion, when both are valid.
     */
    private void updateCaloriesPreview() {
        Recipe recipe = recipeComboBox.getValue();
        Integer grams = parseGrams();

        if (recipe == null || grams == null) {
            caloriesLabel.setText("Calories: choose a recipe and portion");
            return;
        }

        caloriesLabel.setText("Calories: "
                + Math.round(recipe.caloriesFor(grams)) + " kcal");
    }

    /**
     * Returns the portion as a positive whole number, or null if it is invalid.
     */
    private Integer parseGrams() {
        try {
            int grams = Integer.parseInt(gramsField.getText().trim());
            return grams > 0 ? grams : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * Validates the form and saves a meal for the current pet.
     */
    @FXML
    private void handleSaveMeal() {
        if (mealSaved) {
            messageLabel.setText(
                    "This meal is already saved. Click Cancel to return.");
            return;
        }

        SessionManager session = SessionManager.getInstance();
        User user = session.getCurrentUser();
        Pet pet = session.getCurrentPet();

        // Only allow logging meals for a pet owned by the current user.
        if (user == null || pet == null
                || !pet.getUserId().equals(user.getUserId())) {
            messageLabel.setText("Please select one of your pets first.");
            return;
        }

        Recipe recipe = recipeComboBox.getValue();
        if (recipe == null) {
            messageLabel.setText("Please choose a recipe.");
            return;
        }

        Integer grams = parseGrams();
        if (grams == null) {
            messageLabel.setText(
                    "Enter the portion as a whole number of grams greater than zero.");
            return;
        }

        LocalDate date = datePicker.getValue();
        if (date == null) {
            messageLabel.setText("Please choose a date.");
            return;
        }

        if (date.isAfter(LocalDate.now())) {
            messageLabel.setText("Meals cannot be logged for a future date.");
            return;
        }

        // Store the calories now so later recipe edits do not change this meal.
        Meal meal = new Meal(
                "M-" + UUID.randomUUID(),
                pet.getPetId(),
                date,
                recipe.getRecipeId(),
                recipe.getName(),
                grams,
                (int) Math.round(recipe.caloriesFor(grams)));

        // All CSV access stays in DataManager to maintain MVC separation.
        try {
            dataManager.addMeal(meal);
            mealSaved = true;
        } catch (IOException exception) {
            messageLabel.setText(
                    "Unable to save meal: " + exception.getMessage());
            exception.printStackTrace();
            return;
        }

        returnToProfile();
    }

    /**
     * Returns to the pet profile without saving.
     */
    @FXML
    private void handleCancel() {
        returnToProfile();
    }

    /**
     * Reloads the pet profile in the existing scene.
     */
    private void returnToProfile() {
        try {
            Parent profileView = FXMLLoader.load(
                    HelloApplication.class.getResource(
                            "view/pet-profile-view.fxml"));

            gramsField.getScene().setRoot(profileView);
        } catch (IOException exception) {
            // Distinguish a navigation failure from a failed save.
            messageLabel.setText(mealSaved
                    ? "Meal saved. Unable to open the pet profile. "
                    + "Click Cancel to try again."
                    : "Unable to open the pet profile.");
            exception.printStackTrace();
        }
    }
}
