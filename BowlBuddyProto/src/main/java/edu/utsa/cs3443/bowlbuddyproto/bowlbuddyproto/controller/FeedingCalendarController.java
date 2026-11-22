package edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.controller;

import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.HelloApplication;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.data.DataManager;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model.FeedingCalendar;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model.Meal;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model.Pet;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextAlignment;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shows a monthly feeding calendar for the pet selected in the session.
 * Each day shows its calorie total, and clicking a day lists its meals.
 */
public class FeedingCalendarController {

    @FXML private Label petNameLabel;
    @FXML private Label monthLabel;
    @FXML private GridPane calendarGrid;
    @FXML private Label selectedDayLabel;
    @FXML private Label dayTotalLabel;
    @FXML private ProgressBar dayProgressBar;
    @FXML private ListView<Meal> dayMealListView;
    @FXML private Label messageLabel;

    private final DataManager dataManager = new DataManager();
    private Pet pet;
    private YearMonth currentMonth = YearMonth.now();
    private FeedingCalendar calendar;

    /** Loads the current pet and shows this month. */
    @FXML
    private void initialize() {
        pet = SessionManager.getInstance().getCurrentPet();

        if (pet == null) {
            messageLabel.setText("No pet selected.");
            return;
        }

        petNameLabel.setText(pet.getName() + "'s Feeding Calendar");
        showMonth();
    }

    /** Loads the meals for the current month and redraws the calendar. */
    private void showMonth() {
        monthLabel.setText(
                currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.US)
                        + " " + currentMonth.getYear());

        try {
            calendar = loadCalendar(currentMonth);
            messageLabel.setText("");
        } catch (IOException exception) {
            messageLabel.setText("Unable to load meals: " + exception.getMessage());
            calendar = new FeedingCalendar(currentMonth.getMonthValue(),
                    currentMonth.getYear(), new ArrayList<>());
        }

        buildGrid();

        // Select today if we're viewing this month, otherwise the 1st.
        LocalDate today = LocalDate.now();
        if (YearMonth.from(today).equals(currentMonth)) {
            showDay(today);
        } else {
            showDay(currentMonth.atDay(1));
        }
    }

    /** Builds a FeedingCalendar from every meal logged in the given month. */
    private FeedingCalendar loadCalendar(YearMonth month) throws IOException {
        List<Meal> meals = new ArrayList<>();

        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            meals.addAll(dataManager.loadMealsForPet(
                    pet.getPetId(), month.atDay(day)));
        }

        return new FeedingCalendar(month.getMonthValue(), month.getYear(), meals);
    }

    /** Draws the weekday headers and one button per day of the month. */
    private void buildGrid() {
        calendarGrid.getChildren().clear();

        String[] weekdays = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < weekdays.length; i++) {
            Label header = new Label(weekdays[i]);
            header.setStyle("-fx-font-weight: bold;");
            calendarGrid.add(header, i, 0);
        }

        // Sunday = 0, Monday = 1, ... so day 1 lands in the right column.
        int offset = currentMonth.atDay(1).getDayOfWeek().getValue() % 7;

        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            LocalDate date = currentMonth.atDay(day);
            int calories = calendar.getTotalCaloriesFor(date);

            String text = Integer.toString(day);
            if (calories > 0) {
                text += "\n" + calories + " kcal";
            }

            Button dayButton = new Button(text);
            dayButton.setPrefSize(90, 60);
            dayButton.setTextAlignment(TextAlignment.CENTER);
            dayButton.setStyle(styleFor(date, calories));
            dayButton.setOnAction(event -> showDay(date));

            int cell = offset + day - 1;
            calendarGrid.add(dayButton, cell % 7, cell / 7 + 1);
        }
    }

    /** Picks a colour: white = no meals, green = within goal, red = over goal. */
    private String styleFor(LocalDate date, int calories) {
        String background;
        if (calories == 0) {
            background = "white";
        } else if (calories <= pet.getDailyCalorieGoal()) {
            background = "#DDEBDD";
        } else {
            background = "#F8D7DA";
        }

        String border = date.equals(LocalDate.now())
                ? "-fx-border-color: #284D3B; -fx-border-width: 2;"
                : "-fx-border-color: #CCCCCC;";

        return "-fx-background-color: " + background + "; " + border;
    }

    /** Shows the selected day's meals, total, and progress toward the goal. */
    private void showDay(LocalDate date) {
        int total = calendar.getTotalCaloriesFor(date);
        int goal = pet.getDailyCalorieGoal();

        selectedDayLabel.setText(
                date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")));
        dayTotalLabel.setText(total + " of " + goal + " kcal");
        dayMealListView.getItems().setAll(calendar.getMealsFor(date));

        double progress = goal > 0 ? (double) total / goal : 0;
        dayProgressBar.setProgress(Math.min(progress, 1.0));
        dayProgressBar.setStyle(progress > 1.0
                ? "-fx-accent: #B42318;"
                : "-fx-accent: #284D3B;");
    }

    @FXML
    private void handlePreviousMonth() {
        currentMonth = currentMonth.minusMonths(1);
        showMonth();
    }

    @FXML
    private void handleNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        showMonth();
    }

    /** Returns to the selected pet's profile. */
    @FXML
    private void handleBackToProfile() {
        try {
            Parent profileView = FXMLLoader.load(
                    HelloApplication.class.getResource(
                            "view/pet-profile-view.fxml"));
            petNameLabel.getScene().setRoot(profileView);
        } catch (IOException exception) {
            messageLabel.setText("Unable to return to the pet profile.");
            exception.printStackTrace();
        }
    }
}