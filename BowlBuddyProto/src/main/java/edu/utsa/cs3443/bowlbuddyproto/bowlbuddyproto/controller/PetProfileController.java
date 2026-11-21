package edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.controller;

import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.HelloApplication;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model.Meal;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model.Pet;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model.SessionManager;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model.User;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.data.DataManager;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Displays the selected pet's profile.
 */
public class PetProfileController {

    @FXML private Label petNameLabel;
    @FXML private Label animalTypeLabel;
    @FXML private Label breedLabel;
    @FXML private Label ageLabel;
    @FXML private Label weightLabel;
    @FXML private Label calorieGoalLabel;
    @FXML private Label mealsPerDayLabel;
    @FXML private Label medicationLabel;
    @FXML private Label notesLabel;
    @FXML private Label caloriesTodayLabel;
    @FXML private ListView<Meal> mealListView;
    @FXML private Label messageLabel;

    private final DataManager dataManager = new DataManager();

    @FXML
    private void initialize() {
        SessionManager session = SessionManager.getInstance();
        User user = session.getCurrentUser();
        Pet pet = session.getCurrentPet();

        if (user == null || pet == null
                || !pet.getUserId().equals(user.getUserId())) {
            messageLabel.setText("Please select one of your pets.");
            return;
        }

        petNameLabel.setText(pet.getName());
        animalTypeLabel.setText(pet.getAnimalType());
        breedLabel.setText(pet.getBreed());
        ageLabel.setText(pet.getAgeYears() + " years");
        weightLabel.setText(pet.getWeightKg() + " kg");
        calorieGoalLabel.setText(pet.getDailyCalorieGoal() + " kcal");
        mealsPerDayLabel.setText(
                Integer.toString(pet.getMealsPerDay()));
        medicationLabel.setText(pet.getMedication());
        notesLabel.setText(pet.getNotes());

        loadTodaysMeals(pet);
    }

    /**
     * Loads today's meals and shows the calorie total against the daily goal.
     */
    private void loadTodaysMeals(Pet pet) {
        try {
            mealListView.getItems().setAll(
                    dataManager.loadMealsForPet(pet.getPetId(), LocalDate.now()));
        } catch (IOException exception) {
            messageLabel.setText(
                    "Unable to load meals: " + exception.getMessage());
            exception.printStackTrace();
            return;
        }

        int caloriesToday = 0;
        for (Meal meal : mealListView.getItems()) {
            caloriesToday += meal.getCalories();
        }

        caloriesTodayLabel.setText(caloriesToday + " of "
                + pet.getDailyCalorieGoal() + " kcal eaten today");
    }

    /**
     * Opens the Log Meal form for the pet selected in the current session.
     */
    @FXML
    private void handleLogMeal() {
        try {
            Parent logMealView = FXMLLoader.load(
                    HelloApplication.class.getResource(
                            "view/log-meal-view.fxml"));

            petNameLabel.getScene().setRoot(logMealView);
        } catch (IOException exception) {
            messageLabel.setText("Unable to open the Log Meal form.");
            exception.printStackTrace();
        }
    }

    /**
     * Confirms removal of the highlighted meal and deletes its CSV record.
     */
    @FXML
    private void handleRemoveMeal() {
        Pet pet = SessionManager.getInstance().getCurrentPet();
        Meal selectedMeal = mealListView.getSelectionModel().getSelectedItem();

        if (pet == null || selectedMeal == null) {
            messageLabel.setText("Please select a meal first.");
            return;
        }

        // Require confirmation before deleting the meal's record.
        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Remove " + selectedMeal + " from today's meals? "
                        + "This cannot be undone.",
                ButtonType.CANCEL,
                ButtonType.OK);

        confirmation.setTitle("Remove Meal");
        confirmation.setHeaderText("Remove this meal?");
        confirmation.initOwner(petNameLabel.getScene().getWindow());

        // Make Cancel the default to prevent accidental confirmation.
        ((Button) confirmation.getDialogPane().lookupButton(ButtonType.OK))
                .setDefaultButton(false);
        ((Button) confirmation.getDialogPane().lookupButton(ButtonType.CANCEL))
                .setDefaultButton(true);

        // Closing the dialog has the same effect as clicking Cancel.
        ButtonType result = confirmation.showAndWait()
                .orElse(ButtonType.CANCEL);

        if (result != ButtonType.OK) {
            return;
        }

        try {
            dataManager.removeMeal(selectedMeal.getMealId(), pet.getPetId());
        } catch (IOException exception) {
            messageLabel.setText(
                    "Unable to remove meal: " + exception.getMessage());
            exception.printStackTrace();
            return;
        }

        // Reload so the list and calorie total both reflect the removal.
        messageLabel.setText("");
        loadTodaysMeals(pet);
    }

    @FXML
    private void handleBackToPets() {
        try {
            Parent petListView = FXMLLoader.load(
                    HelloApplication.class.getResource(
                            "view/pet-list-view.fxml"));

            petNameLabel.getScene().setRoot(petListView);

        } catch (IOException exception) {
            messageLabel.setText("Unable to open your pet list.");
            exception.printStackTrace();
        }
    }

    /**
     * Opens the edit form for the pet selected in the current session.
     */
    @FXML
    private void handleEditPet() {
        try {
            Parent editPetView = FXMLLoader.load(
                    HelloApplication.class.getResource(
                            "view/edit-pet-view.fxml"));

            petNameLabel.getScene().setRoot(editPetView);
        } catch (IOException exception) {
            messageLabel.setText("Unable to open the Edit Pet form.");
            exception.printStackTrace();
        }
    }

    /**
     * Confirms removal of the current pet, deletes its CSV record,
     * and returns to the pet list.
     */
    @FXML
    private void handleRemovePet() {
        SessionManager session = SessionManager.getInstance();
        User user = session.getCurrentUser();
        Pet pet = session.getCurrentPet();

        // Only allow removal of a pet owned by the logged-in user.
        if (user == null || pet == null
                || !pet.getUserId().equals(user.getUserId())) {
            messageLabel.setText("Please select one of your pets first.");
            return;
        }

        // Require confirmation before deleting the pet's record.
        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Remove " + pet.getName()
                        + " from your pets? This cannot be undone.",
                ButtonType.CANCEL,
                ButtonType.OK);

        confirmation.setTitle("Remove Pet");
        confirmation.setHeaderText("Remove " + pet.getName() + "?");
        confirmation.initOwner(petNameLabel.getScene().getWindow());

        // Cast the dialog controls to Button to configure the default action.
        Button okButton = (Button) confirmation.getDialogPane()
                .lookupButton(ButtonType.OK);

        Button cancelButton = (Button) confirmation.getDialogPane()
                .lookupButton(ButtonType.CANCEL);

// Make Cancel the default to prevent accidental confirmation.
        okButton.setDefaultButton(false);
        cancelButton.setDefaultButton(true);

        // Closing the dialog has the same effect as clicking Cancel.
        ButtonType result = confirmation.showAndWait()
                .orElse(ButtonType.CANCEL);

        if (result != ButtonType.OK) {
            return;
        }

        // Delegate all file access to DataManager.
        try {
            DataManager dataManager = new DataManager();
            dataManager.removePet(pet.getPetId(), user.getUserId());
        } catch (IOException exception) {
            messageLabel.setText(
                    "Unable to remove pet: " + exception.getMessage());
            exception.printStackTrace();
            return;
        }

        // Clear the selected pet only after deletion succeeds.
        session.setCurrentPet(null);

        // Reload the list so the removed pet no longer appears.
        try {
            Parent petListView = FXMLLoader.load(
                    HelloApplication.class.getResource(
                            "view/pet-list-view.fxml"));

            petNameLabel.getScene().setRoot(petListView);
        } catch (IOException exception) {
            messageLabel.setText(
                    "Pet removed. Click Back to My Pets to return.");
            exception.printStackTrace();
        }
    }


}