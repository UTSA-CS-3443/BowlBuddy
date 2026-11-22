# BowlBuddy

BowlBuddy is a JavaFX desktop app for managing pet profiles, browsing feeding recipes, logging meals, and tracking each pet's calories on a feeding calendar. Team ByteMe built it for CS 3443 – Application Programming at UTSA.

## Current Features

- Create an account, sign in, and log out. New passwords are stored as hashes.
- View and edit your account name and email.
- View, add, edit, and remove pets linked to your account. Removing a pet requires confirmation.
- View each pet’s breed, age, weight, daily calorie goal, feeding frequency, medication, and notes.
- Browse the Recipe Library to see brands, calories per 100 g, tags, and instructions. Add, edit, and delete recipes.
- Log meals for a pet and see today’s meals and calorie total on the pet profile.
- Track calories against each pet’s daily goal with a progress bar that turns red when the goal is exceeded.
- Open a monthly feeding calendar from a pet’s profile. Each day shows its calorie total; click a day to see that day’s meals, and use Previous and Next to change months.
- Save account, pet, recipe, and meal changes locally so they’re available after restarting the app.

## Run the App

**Requirements:** JDK 21. The project uses Maven and JavaFX 21.0.6. The Maven wrapper is included.

1. Clone the repository and open the `BowlBuddyProto` folder—the one containing `pom.xml`.
2. In IntelliJ IDEA, import it as a Maven project and set the project SDK to JDK 21.
3. Run `Launcher.java`. Set the run configuration’s working directory to `BowlBuddyProto` so the app can find the `data` folder.

On Windows, you can also launch from PowerShell:

```powershell
cd BowlBuddyProto
.\mvnw.cmd javafx:run
```

Run those commands from the repository root. From the sign-in screen, use a demo account or click **Create Account** to make a local test account.

## Project Layout

| Path | What it contains |
|---|---|
| `BowlBuddyProto/src/main/java` | Java models, controllers, `DataManager`, and the launcher |
| `BowlBuddyProto/src/main/resources` | FXML views |
| `BowlBuddyProto/data/users.csv` | Sample accounts and local account changes |
| `BowlBuddyProto/data/pets.csv` | Pet profiles linked to their owners by user ID |
| `BowlBuddyProto/data/recipes.csv` | Sample recipes used by the Recipe Library |
| `BowlBuddyProto/data/meals.csv` | Logged meals linked to pets by pet ID, used by the pet profile and feeding calendar |

BowlBuddy follows the Model–View–Controller pattern. `DataManager` handles CSV reads and writes. `FeedingCalendar` groups a pet’s meals by date for the calendar view. Each clone has its own local data; keep personal account information and local test registrations out of the public repository.

## Build Check

The prototype update built successfully with `mvnw.cmd clean test` on Windows. Maven reported that there were no automated tests to run.