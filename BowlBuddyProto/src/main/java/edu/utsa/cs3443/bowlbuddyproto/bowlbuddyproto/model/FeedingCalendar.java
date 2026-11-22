package edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class FeedingCalendar {

    private final int month;
    private final int year;
    private final List<Meal> meals;

    /**
     *
     * @param month the month number (1 = January, 12 = December)
     * @param year  the four-digit year
     * @param meals the meals logged during this month
     */
    public FeedingCalendar(int month, int year, List<Meal> meals) {
        this.month = month;
        this.year = year;
        this.meals = new ArrayList<>(meals);
    }

    /**
     *
     * @param date the day to look up
     * @return a list of that day's meals (empty if none)
     */
    public List<Meal> getMealsFor(LocalDate date) {
        List<Meal> mealsForDay = new ArrayList<>();
        for (Meal meal : meals) {
            if (meal.getDate().equals(date)) {
                mealsForDay.add(meal);
            }
        }
        return mealsForDay;
    }

    /**
     *
     * @param date the day to total
     * @return total calories for that day
     */
    public int getTotalCaloriesFor(LocalDate date) {
        int total = 0;
        for (Meal meal : getMealsFor(date)) {
            total += meal.getCalories();
        }
        return total;
    }

    public void addMeal(Meal meal) {
        meals.add(meal);
    }

    public void removeMeal(Meal meal) {
        meals.remove(meal);
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public List<Meal> getMeals() {
        return new ArrayList<>(meals);
    }
}