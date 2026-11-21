package edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.data;

import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model.Meal;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model.User;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model.PasswordUtil;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model.Pet;
import edu.utsa.cs3443.bowlbuddyproto.bowlbuddyproto.model.Recipe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages application data and CSV file operations.
 */
public class DataManager {

    private final Path usersFile;
    private final List<User> users;

    public DataManager() {
        usersFile = Path.of("data", "users.csv");
        users = new ArrayList<>();
    }

    public List<User> getUsers() {
        return List.copyOf(users);
    }
    /**
     * Loads the supplied user CSV into memory.
     * The Password column contains the sample passwords.
     * PasswordHash is supported for files saved with hashed passwords.
     */
    public void loadUsers() throws IOException {
        List<String> lines =
                Files.readAllLines(usersFile, StandardCharsets.UTF_8);

        if (lines.isEmpty()) {
            throw new IOException("The user CSV is empty.");
        }

        // Remove a possible UTF-8 byte order mark from the header.
        String header = lines.get(0);
        if (header.startsWith("\uFEFF")) {
            header = header.substring(1);
        }

        boolean containsHashes;

        if (header.equals(
                "UserId,First Name,Last Name,Email,Username,Password")) {
            containsHashes = false;
        } else if (header.equals(
                "UserId,First Name,Last Name,Email,Username,PasswordHash")) {
            containsHashes = true;
        } else {
            throw new IOException("The user CSV has an unexpected header.");
        }

        List<User> loadedUsers = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);

            if (line.isBlank()) {
                continue;
            }

            // This initial loader supports the supplied unquoted CSV.
            if (line.contains("\"")) {
                throw new IOException(
                        "Quoted CSV fields are not supported yet. Row " + (i + 1));
            }

            // Parse quoted fields so names containing commas remain supported.
            String[] fields = parseCsvLine(line);

            if (fields.length != 6) {
                throw new IOException(
                        "Expected six columns at row " + (i + 1));
            }

            String passwordHash = containsHashes
                    ? fields[5]
                    : PasswordUtil.hashPassword(fields[5]);

            User user = new User(
                    fields[0].trim(),
                    fields[1].trim(),
                    fields[2].trim(),
                    fields[3].trim(),
                    fields[4].trim(),
                    passwordHash
            );

            loadedUsers.add(user);
        }

        // Replace the current list only after loading succeeds.
        users.clear();
        users.addAll(loadedUsers);
    }
    /**
     * Loads pets belonging to the specified user.
     */
    public List<Pet> loadPetsForUser(String userId) throws IOException {
        Path petsFile = Path.of("data", "pets.csv");
        List<String> lines =
                Files.readAllLines(petsFile, StandardCharsets.UTF_8);

        if (lines.isEmpty()) {
            throw new IOException("The pet CSV is empty.");
        }

        String header = lines.get(0);
        if (header.startsWith("\uFEFF")) {
            header = header.substring(1);
        }

        String expectedHeader =
                "PetId,UserId,Animal Type,Name,Breed,Age (yrs),"
                        + "Weight (kg),Daily Calorie Goal,Meals Per Day,"
                        + "Medication,Notes";

        if (!header.equals(expectedHeader)) {
            throw new IOException("The pet CSV has an unexpected header.");
        }

        List<Pet> pets = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);

            if (line.isBlank()) {
                continue;
            }

            String[] fields = parseCsvLine(line);



            if (fields.length != 11) {
                throw new IOException(
                        "Expected eleven columns at row " + (i + 1));
            }

            // Only include pets owned by this user.
            if (!fields[1].trim().equals(userId)) {
                continue;
            }

            try {
                Pet pet = new Pet(
                        fields[0].trim(),
                        fields[1].trim(),
                        fields[2].trim(),
                        fields[3].trim(),
                        fields[4].trim(),
                        Integer.parseInt(fields[5].trim()),
                        Float.parseFloat(fields[6].trim()),
                        Integer.parseInt(fields[7].trim()),
                        Integer.parseInt(fields[8].trim()),
                        fields[9].trim(),
                        fields[10].trim()
                );

                pets.add(pet);

            } catch (NumberFormatException exception) {
                throw new IOException(
                        "Invalid number in pet CSV at row " + (i + 1),
                        exception);
            }
        }

        return pets;
    }
    /**
     * Reads a CSV row, preserving commas inside quoted fields
     * and converting doubled quotes into literal quotes.
     */
    private String[] parseCsvLine(String line) throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);

            if (character == '"') {
                if (insideQuotes
                        && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {

                    field.append('"');
                    i++;
                } else {
                    insideQuotes = !insideQuotes;
                }

            } else if (character == ',' && !insideQuotes) {
                fields.add(field.toString());
                field.setLength(0);

            } else {
                field.append(character);
            }
        }

        if (insideQuotes) {
            throw new IOException("CSV row contains an unclosed quoted field.");
        }

        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }


    /**
     * Adds a new pet to the CSV file while preserving existing records.
     *
     * @param pet the pet to save
     * @throws IOException if the file is invalid or the pet cannot be saved
     */
    public void addPet(Pet pet) throws IOException {
        Path petsFile = Path.of("data", "pets.csv");

        String contents = Files.readString(
                petsFile, StandardCharsets.UTF_8);

        String expectedHeader =
                "PetId,UserId,Animal Type,Name,Breed,Age (yrs),"
                        + "Weight (kg),Daily Calorie Goal,Meals Per Day,"
                        + "Medication,Notes";

        List<String> lines = Files.readAllLines(
                petsFile, StandardCharsets.UTF_8);

        // Check the file structure before adding another record.
        if (lines.isEmpty()) {
            throw new IOException("The pet CSV is empty.");
        }

        String header = lines.get(0);

        // Some spreadsheet programs place a byte order mark before the header.
        if (header.startsWith("\uFEFF")) {
            header = header.substring(1);
        }

        if (!header.equals(expectedHeader)) {
            throw new IOException("The pet CSV has an unexpected header.");
        }

        // Validate existing rows and prevent duplicate pet IDs.
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }

            String[] fields = parseCsvLine(lines.get(i));

            if (fields.length != 11) {
                throw new IOException(
                        "Invalid pet record at row " + (i + 1));
            }

            if (fields[0].trim().equals(pet.getPetId())) {
                throw new IOException("That pet ID already exists.");
            }
        }

        // Store values in the same order as the CSV header.
        String row = String.join(",",
                csvField(pet.getPetId()),
                csvField(pet.getUserId()),
                csvField(pet.getAnimalType()),
                csvField(pet.getName()),
                csvField(pet.getBreed()),
                Integer.toString(pet.getAgeYears()),
                Float.toString(pet.getWeightKg()),
                Integer.toString(pet.getDailyCalorieGoal()),
                Integer.toString(pet.getMealsPerDay()),
                csvField(pet.getMedication()),
                csvField(pet.getNotes()));

        // Separate the new record if the existing file has no final newline.
        String separator =
                contents.endsWith("\n") || contents.endsWith("\r")
                        ? "" : System.lineSeparator();

        // Append the new pet without overwriting existing records.
        Files.writeString(
                petsFile,
                separator + row + System.lineSeparator(),
                StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.APPEND);
    }

    /**
     * Formats a text value for storage in a single CSV field.
     *
     * @param value the text to format
     * @return the value with CSV quotation marks escaped when necessary
     * @throws IOException if the value contains a line break
     */
    private String csvField(String value) throws IOException {
        if (value == null) {
            return "";
        }

        // The current CSV reader expects each record on one physical line.
        if (value.contains("\n") || value.contains("\r")) {
            throw new IOException(
                    "Please enter each field on a single line.");
        }

        // Quote fields containing commas and double any embedded quotes.
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }


    /**
     * Updates an existing pet's CSV record.
     * Matches both pet ID and owner ID to preserve ownership.
     *
     * @param pet the pet containing the updated details
     * @throws IOException if the file is invalid or cannot be updated
     */
    public void updatePet(Pet pet) throws IOException {
        Path petsFile = Path.of("data", "pets.csv");

        List<String> lines = Files.readAllLines(
                petsFile, StandardCharsets.UTF_8);

        String expectedHeader =
                "PetId,UserId,Animal Type,Name,Breed,Age (yrs),"
                        + "Weight (kg),Daily Calorie Goal,Meals Per Day,"
                        + "Medication,Notes";

        if (lines.isEmpty()) {
            throw new IOException("The pet CSV is empty.");
        }

        // Remove an optional byte order mark when checking the header.
        String header = lines.get(0);
        if (header.startsWith("\uFEFF")) {
            header = header.substring(1);
        }

        if (!header.equals(expectedHeader)) {
            throw new IOException("The pet CSV has an unexpected header.");
        }

        int matchingRow = -1;

        // Validate existing records and locate the pet being edited.
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }

            String[] fields = parseCsvLine(lines.get(i));

            if (fields.length != 11) {
                throw new IOException(
                        "Invalid pet record at row " + (i + 1));
            }

            if (fields[0].trim().equals(pet.getPetId())) {
                if (!fields[1].trim().equals(pet.getUserId())) {
                    throw new IOException("The pet's owner does not match.");
                }

                if (matchingRow != -1) {
                    throw new IOException("Duplicate pet ID in the CSV.");
                }

                matchingRow = i;
            }
        }

        if (matchingRow == -1) {
            throw new IOException("The selected pet could not be found.");
        }

        // Replace only the selected pet's record, keeping its IDs unchanged.
        String updatedRow = String.join(",",
                csvField(pet.getPetId()),
                csvField(pet.getUserId()),
                csvField(pet.getAnimalType()),
                csvField(pet.getName()),
                csvField(pet.getBreed()),
                Integer.toString(pet.getAgeYears()),
                Float.toString(pet.getWeightKg()),
                Integer.toString(pet.getDailyCalorieGoal()),
                Integer.toString(pet.getMealsPerDay()),
                csvField(pet.getMedication()),
                csvField(pet.getNotes()));

        lines.set(matchingRow, updatedRow);

        // Write a temporary file before replacing the original CSV.
        Path temporaryFile = Files.createTempFile(
                petsFile.toAbsolutePath().getParent(), "pets-", ".tmp");

        try {
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);

            try {
                // Replace the original in one operation when supported.
                Files.move(
                        temporaryFile,
                        petsFile.toAbsolutePath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                // Use a regular replacement if atomic moves are unavailable.
                Files.move(
                        temporaryFile,
                        petsFile.toAbsolutePath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            // Remove the temporary file if it remains after a failed update.
            Files.deleteIfExists(temporaryFile);
        }
    }


    /**
     * Removes a pet from the CSV file.
     * Checks both the pet ID and owner ID before deleting the record.
     *
     * @param petId the ID of the pet to remove
     * @param userId the ID of the pet's owner
     * @throws IOException if the file is invalid or deletion fails
     */
    public void removePet(String petId, String userId) throws IOException {
        Path petsFile = Path.of("data", "pets.csv");

        List<String> lines = Files.readAllLines(
                petsFile, StandardCharsets.UTF_8);

        String expectedHeader =
                "PetId,UserId,Animal Type,Name,Breed,Age (yrs),"
                        + "Weight (kg),Daily Calorie Goal,Meals Per Day,"
                        + "Medication,Notes";

        if (lines.isEmpty()) {
            throw new IOException("The pet CSV is empty.");
        }

        // Remove an optional byte order mark when checking the header.
        String header = lines.get(0);
        if (header.startsWith("\uFEFF")) {
            header = header.substring(1);
        }

        if (!header.equals(expectedHeader)) {
            throw new IOException("The pet CSV has an unexpected header.");
        }

        int matchingRow = -1;

        // Validate all records before changing the file.
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }

            String[] fields = parseCsvLine(lines.get(i));

            if (fields.length != 11) {
                throw new IOException(
                        "Invalid pet record at row " + (i + 1));
            }

            if (fields[0].trim().equals(petId)) {
                // Prevent removing a pet belonging to a different owner.
                if (!fields[1].trim().equals(userId)) {
                    throw new IOException("The pet's owner does not match.");
                }

                if (matchingRow != -1) {
                    throw new IOException("Duplicate pet ID in the CSV.");
                }

                matchingRow = i;
            }
        }

        if (matchingRow == -1) {
            throw new IOException("The selected pet could not be found.");
        }

        // Remove only the matching record, preserving all other pets.
        lines.remove(matchingRow);

        // Prepare the updated CSV before replacing the original file.
        Path temporaryFile = Files.createTempFile(
                petsFile.toAbsolutePath().getParent(), "pets-", ".tmp");

        try {
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);

            try {
                // Replace the original file atomically when supported.
                Files.move(
                        temporaryFile,
                        petsFile.toAbsolutePath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                // Fall back to a regular replacement when necessary.
                Files.move(
                        temporaryFile,
                        petsFile.toAbsolutePath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            // Clean up a temporary file left behind by a failed operation.
            Files.deleteIfExists(temporaryFile);
        }
    }

    /**
     * Registers an account and saves all accounts using password hashes.
     * Existing plaintext sample passwords are converted by loadUsers().
     *
     * @param firstName the user's first name
     * @param lastName the user's last name
     * @param email the user's email address
     * @param username the requested username
     * @param password the password to hash before storage
     * @return the newly registered user
     * @throws IOException if an account conflicts or saving fails
     */
    public User registerUser(
            String firstName,
            String lastName,
            String email,
            String username,
            String password) throws IOException {

        // Reload accounts so duplicate checks use the latest saved data.
        // The loader converts plaintext sample passwords into hashes in memory.
        loadUsers();

        firstName = firstName.trim();
        lastName = lastName.trim();
        email = email.trim();
        username = username.trim();

        // Reject incomplete account information before changing the file.
        if (firstName.isEmpty() || lastName.isEmpty()
                || email.isEmpty() || username.isEmpty()) {
            throw new IOException("Please complete every field.");
        }

        if (password == null || password.length() < 8) {
            throw new IOException(
                    "Your password must contain at least 8 characters.");
        }

        // Compare without case so capitalization cannot bypass duplicate checks.
        for (User existingUser : users) {
            if (existingUser.getUsername().equalsIgnoreCase(username)) {
                throw new IOException("That username is already taken.");
            }

            if (existingUser.getEmail().equalsIgnoreCase(email)) {
                throw new IOException("That email is already registered.");
            }
        }

        // Generate an ID and hash the password before creating the account.
        User newUser = new User(
                "U-" + java.util.UUID.randomUUID(),
                firstName,
                lastName,
                email,
                username,
                PasswordUtil.hashPassword(password));

        List<User> updatedUsers = new ArrayList<>(users);
        updatedUsers.add(newUser);

        List<String> outputLines = new ArrayList<>();

        // Every record now contains a hash, including existing sample accounts.
        outputLines.add(
                "UserId,First Name,Last Name,Email,Username,PasswordHash");

        for (User account : updatedUsers) {
            outputLines.add(String.join(",",
                    csvField(account.getUserId()),
                    csvField(account.getFirstName()),
                    csvField(account.getLastName()),
                    csvField(account.getEmail()),
                    csvField(account.getUsername()),
                    csvField(account.getPasswordHash())));
        }

        // Write the complete result before replacing the existing accounts file.
        Path temporaryFile = Files.createTempFile(
                usersFile.toAbsolutePath().getParent(), "users-", ".tmp");

        try {
            Files.write(
                    temporaryFile, outputLines, StandardCharsets.UTF_8);

            try {
                Files.move(
                        temporaryFile,
                        usersFile.toAbsolutePath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                // Use a regular replacement if atomic moves are unavailable.
                Files.move(
                        temporaryFile,
                        usersFile.toAbsolutePath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            // Clean up a temporary file left behind if saving fails.
            Files.deleteIfExists(temporaryFile);
        }

        // Update memory only after the accounts file has been saved.
        users.clear();
        users.addAll(updatedUsers);

        return newUser;
    }

    /**
     * Updates the name and email for an existing account.
     *
     * @param userId the ID of the account to update
     * @param firstName the updated first name
     * @param lastName the updated last name
     * @param email the updated email address
     * @return the saved account details
     * @throws IOException if the account cannot be updated or saved
     */
    public User updateUserProfile(
            String userId, String firstName, String lastName, String email)
            throws IOException {

        loadUsers();
        List<User> updatedUsers = new ArrayList<>(users);
        int accountIndex = -1;

        // Find the account by ID and check whether another account uses the email.
        for (int i = 0; i < updatedUsers.size(); i++) {
            User account = updatedUsers.get(i);

            if (account.getUserId().equals(userId)) {
                if (accountIndex != -1) {
                    throw new IOException("Duplicate user ID in the CSV.");
                }
                accountIndex = i;
            } else if (account.getEmail().equalsIgnoreCase(email)) {
                throw new IOException("That email is already registered.");
            }
        }

        if (accountIndex == -1) {
            throw new IOException("Your account could not be found.");
        }

        // Use the latest saved username and password hash.
        User original = updatedUsers.get(accountIndex);
        User updated = new User(
                original.getUserId(), firstName, lastName, email,
                original.getUsername(), original.getPasswordHash());
        updatedUsers.set(accountIndex, updated);

        List<String> outputLines = new ArrayList<>();
        outputLines.add(
                "UserId,First Name,Last Name,Email,Username,PasswordHash");

        for (User account : updatedUsers) {
            outputLines.add(String.join(",",
                    csvField(account.getUserId()),
                    csvField(account.getFirstName()),
                    csvField(account.getLastName()),
                    csvField(account.getEmail()),
                    csvField(account.getUsername()),
                    csvField(account.getPasswordHash())));
        }

        // Write the complete CSV before replacing the original.
        Path target = usersFile.toAbsolutePath();
        Path temporaryFile = Files.createTempFile(
                target.getParent(), "users-", ".tmp");

        try {
            Files.write(temporaryFile, outputLines, StandardCharsets.UTF_8);

            try {
                Files.move(temporaryFile, target,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, target,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }

        // Refresh the in-memory list only after saving succeeds.
        users.clear();
        users.addAll(updatedUsers);
        return updated;
    }

    /**
     * Loads recipes from data/recipes.csv.
     *
     * @return the recipes in the order they appear in the file
     * @throws IOException if the file cannot be read or contains an invalid record
     */
    public List<Recipe> loadRecipes() throws IOException {
        Path recipesFile = Path.of("data", "recipes.csv");
        List<String> lines = Files.readAllLines(
                recipesFile, StandardCharsets.UTF_8);

        String expectedHeader =
                "Recipe ID,Recipe Name,Brand,Calories per 100g,Tag,Favorite,Instructions";

        if (lines.isEmpty()
                || !lines.get(0).replaceFirst("^\uFEFF", "").equals(expectedHeader)) {
            throw new IOException("The recipe CSV has an unexpected header.");
        }

        List<Recipe> recipes = new ArrayList<>();

        // Skip the header and reuse the parser for quoted instructions.
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }

            String[] fields = parseCsvLine(line);
            if (fields.length != 7) {
                throw new IOException(
                        "Expected seven columns on recipe row " + (i + 1) + ".");
            }

            // The sample file stores favorites as Yes or No.
            String favorite = fields[5].trim();
            if (!favorite.equalsIgnoreCase("Yes")
                    && !favorite.equalsIgnoreCase("No")) {
                throw new IOException(
                        "Expected Yes or No on recipe row " + (i + 1) + ".");
            }

            int calories;
            try {
                calories = Integer.parseInt(fields[3].trim());
            } catch (NumberFormatException exception) {
                throw new IOException(
                        "Invalid calories on recipe row " + (i + 1) + ".", exception);
            }

            if (calories < 0) {
                throw new IOException(
                        "Calories cannot be negative on recipe row " + (i + 1) + ".");
            }

            recipes.add(new Recipe(
                    fields[0].trim(),
                    fields[1].trim(),
                    fields[2].trim(),
                    calories,
                    fields[4].trim(),
                    favorite.equalsIgnoreCase("Yes"),
                    fields[6].trim()));
        }

        return recipes;
    }

    /**
     * Creates the next sequential recipe ID, such as R012.
     *
     * @return an ID one higher than the largest existing R-number
     * @throws IOException if the recipe file is invalid
     */
    public String nextRecipeId() throws IOException {
        List<String> lines = readRecipeLines();
        int highestNumber = 0;

        // Look only at IDs that follow the R### pattern used by the sample data.
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }

            String recipeId = parseCsvLine(lines.get(i))[0].trim();

            if (recipeId.matches("R\\d+")) {
                highestNumber = Math.max(
                        highestNumber, Integer.parseInt(recipeId.substring(1)));
            }
        }

        return String.format("R%03d", highestNumber + 1);
    }

    /**
     * Adds a new recipe to the end of the recipe CSV.
     *
     * @param recipe the recipe to save
     * @throws IOException if the file is invalid or the recipe cannot be saved
     */
    public void addRecipe(Recipe recipe) throws IOException {
        List<String> lines = readRecipeLines();

        // Prevent two recipes from sharing the same ID.
        if (findRecipeRow(lines, recipe.getRecipeId()) != -1) {
            throw new IOException("That recipe ID already exists.");
        }

        lines.add(recipeRow(recipe));
        writeRecipeLines(lines);
    }

    /**
     * Replaces an existing recipe's CSV record, keeping its ID unchanged.
     *
     * @param recipe the recipe containing the updated details
     * @throws IOException if the file is invalid or cannot be updated
     */
    public void updateRecipe(Recipe recipe) throws IOException {
        List<String> lines = readRecipeLines();
        int matchingRow = findRecipeRow(lines, recipe.getRecipeId());

        if (matchingRow == -1) {
            throw new IOException("The selected recipe could not be found.");
        }

        lines.set(matchingRow, recipeRow(recipe));
        writeRecipeLines(lines);
    }

    /**
     * Removes a recipe from the recipe CSV.
     *
     * @param recipeId the ID of the recipe to remove
     * @throws IOException if the file is invalid or deletion fails
     */
    public void removeRecipe(String recipeId) throws IOException {
        List<String> lines = readRecipeLines();
        int matchingRow = findRecipeRow(lines, recipeId);

        if (matchingRow == -1) {
            throw new IOException("The selected recipe could not be found.");
        }

        // Remove only the matching record, preserving all other recipes.
        lines.remove(matchingRow);
        writeRecipeLines(lines);
    }

    /**
     * Reads the recipe CSV and checks its header and column counts
     * before any record is changed.
     */
    private List<String> readRecipeLines() throws IOException {
        Path recipesFile = Path.of("data", "recipes.csv");
        List<String> lines = new ArrayList<>(Files.readAllLines(
                recipesFile, StandardCharsets.UTF_8));

        String expectedHeader =
                "Recipe ID,Recipe Name,Brand,Calories per 100g,Tag,Favorite,Instructions";

        if (lines.isEmpty()
                || !lines.get(0).replaceFirst("^﻿", "").equals(expectedHeader)) {
            throw new IOException("The recipe CSV has an unexpected header.");
        }

        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }

            if (parseCsvLine(lines.get(i)).length != 7) {
                throw new IOException(
                        "Invalid recipe record at row " + (i + 1));
            }
        }

        return lines;
    }

    /**
     * Finds the line index of a recipe ID, or -1 when it is not present.
     */
    private int findRecipeRow(List<String> lines, String recipeId)
            throws IOException {
        int matchingRow = -1;

        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }

            if (parseCsvLine(lines.get(i))[0].trim().equals(recipeId)) {
                if (matchingRow != -1) {
                    throw new IOException("Duplicate recipe ID in the CSV.");
                }

                matchingRow = i;
            }
        }

        return matchingRow;
    }

    /**
     * Formats a recipe in the same column order as the CSV header.
     */
    private String recipeRow(Recipe recipe) throws IOException {
        return String.join(",",
                csvField(recipe.getRecipeId()),
                csvField(recipe.getName()),
                csvField(recipe.getBrand()),
                Integer.toString(recipe.getCaloriesPer100g()),
                csvField(recipe.getTag()),
                recipe.isFavorite() ? "Yes" : "No",
                csvField(recipe.getInstructions()));
    }

    /**
     * Writes the recipe CSV to a temporary file before replacing the original.
     */
    private void writeRecipeLines(List<String> lines) throws IOException {
        writeCsvLines(Path.of("data", "recipes.csv"), lines);
    }

    /**
     * Loads the meals logged for one pet on one date.
     *
     * @param petId the ID of the pet whose meals to load
     * @param date the day to load meals for
     * @return the matching meals in the order they were logged
     * @throws IOException if the file cannot be read or contains an invalid record
     */
    public List<Meal> loadMealsForPet(String petId, LocalDate date)
            throws IOException {
        List<String> lines = readMealLines();
        List<Meal> meals = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }

            String[] fields = parseCsvLine(lines.get(i));

            // Only include this pet's meals for the requested day.
            if (!fields[1].trim().equals(petId)
                    || !fields[2].trim().equals(date.toString())) {
                continue;
            }

            try {
                meals.add(new Meal(
                        fields[0].trim(),
                        fields[1].trim(),
                        LocalDate.parse(fields[2].trim()),
                        fields[3].trim(),
                        fields[4].trim(),
                        Integer.parseInt(fields[5].trim()),
                        Integer.parseInt(fields[6].trim())));
            } catch (NumberFormatException | java.time.format.DateTimeParseException exception) {
                throw new IOException(
                        "Invalid value in meal CSV at row " + (i + 1), exception);
            }
        }

        return meals;
    }

    /**
     * Adds a meal to the meal CSV, creating the file if it does not exist yet.
     *
     * @param meal the meal to save
     * @throws IOException if the file is invalid or the meal cannot be saved
     */
    public void addMeal(Meal meal) throws IOException {
        List<String> lines = readMealLines();

        // Prevent two meals from sharing the same ID.
        if (findMealRow(lines, meal.getMealId()) != -1) {
            throw new IOException("That meal ID already exists.");
        }

        // Store values in the same order as the CSV header.
        lines.add(String.join(",",
                csvField(meal.getMealId()),
                csvField(meal.getPetId()),
                meal.getDate().toString(),
                csvField(meal.getRecipeId()),
                csvField(meal.getRecipeName()),
                Integer.toString(meal.getGrams()),
                Integer.toString(meal.getCalories())));

        writeCsvLines(Path.of("data", "meals.csv"), lines);
    }

    /**
     * Removes a logged meal.
     * Checks both the meal ID and pet ID before deleting the record.
     *
     * @param mealId the ID of the meal to remove
     * @param petId the ID of the pet the meal belongs to
     * @throws IOException if the file is invalid or deletion fails
     */
    public void removeMeal(String mealId, String petId) throws IOException {
        List<String> lines = readMealLines();
        int matchingRow = findMealRow(lines, mealId);

        if (matchingRow == -1) {
            throw new IOException("The selected meal could not be found.");
        }

        // Prevent removing a meal that belongs to a different pet.
        if (!parseCsvLine(lines.get(matchingRow))[1].trim().equals(petId)) {
            throw new IOException("The meal's pet does not match.");
        }

        lines.remove(matchingRow);
        writeCsvLines(Path.of("data", "meals.csv"), lines);
    }

    /**
     * Reads the meal CSV and checks its header and column counts.
     * Returns only the header when no meals have been logged yet.
     */
    private List<String> readMealLines() throws IOException {
        Path mealsFile = Path.of("data", "meals.csv");
        String expectedHeader =
                "Meal ID,Pet ID,Date,Recipe ID,Recipe Name,Grams,Calories";

        // A fresh clone may not have a meal file until the first meal is saved.
        if (!Files.exists(mealsFile)) {
            List<String> lines = new ArrayList<>();
            lines.add(expectedHeader);
            return lines;
        }

        List<String> lines = new ArrayList<>(Files.readAllLines(
                mealsFile, StandardCharsets.UTF_8));

        if (lines.isEmpty()
                || !lines.get(0).replaceFirst("^﻿", "").equals(expectedHeader)) {
            throw new IOException("The meal CSV has an unexpected header.");
        }

        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }

            if (parseCsvLine(lines.get(i)).length != 7) {
                throw new IOException(
                        "Invalid meal record at row " + (i + 1));
            }
        }

        return lines;
    }

    /**
     * Finds the line index of a meal ID, or -1 when it is not present.
     */
    private int findMealRow(List<String> lines, String mealId)
            throws IOException {
        int matchingRow = -1;

        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }

            if (parseCsvLine(lines.get(i))[0].trim().equals(mealId)) {
                if (matchingRow != -1) {
                    throw new IOException("Duplicate meal ID in the CSV.");
                }

                matchingRow = i;
            }
        }

        return matchingRow;
    }

    /**
     * Writes a CSV to a temporary file before replacing the original,
     * so a failed save never leaves a half-written file behind.
     */
    private void writeCsvLines(Path file, List<String> lines) throws IOException {
        Path target = file.toAbsolutePath();
        Path temporaryFile = Files.createTempFile(
                target.getParent(), "data-", ".tmp");

        try {
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);

            try {
                Files.move(temporaryFile, target,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                // Use a regular replacement if atomic moves are unavailable.
                Files.move(temporaryFile, target,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            // Clean up a temporary file left behind if saving fails.
            Files.deleteIfExists(temporaryFile);
        }
    }

}