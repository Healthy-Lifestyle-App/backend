package healthy.lifestyle.backend.testutil;

import healthy.lifestyle.backend.activity.mental.model.MentalActivity;
import healthy.lifestyle.backend.activity.mental.model.MentalType;
import healthy.lifestyle.backend.activity.mental.model.MentalWorkout;
import healthy.lifestyle.backend.activity.nutrition.model.Nutrition;
import healthy.lifestyle.backend.activity.nutrition.model.NutritionType;
import healthy.lifestyle.backend.activity.workout.model.BodyPart;
import healthy.lifestyle.backend.activity.workout.model.Exercise;
import healthy.lifestyle.backend.activity.workout.model.HttpRef;
import healthy.lifestyle.backend.activity.workout.model.Workout;
import healthy.lifestyle.backend.user.model.Country;
import healthy.lifestyle.backend.user.model.Role;
import healthy.lifestyle.backend.user.model.User;
import java.util.List;

public interface Util {
    BodyPart createBodyPart(int seed);

    HttpRef createDefaultHttpRef(int seed);

    HttpRef createCustomHttpRef(int seed, User user);

    Exercise createDefaultExercise(int seed, boolean needsEquipment, List<BodyPart> bodyParts, List<HttpRef> httpRefs);

    Exercise createCustomExercise(
            int seed, boolean needsEquipment, List<BodyPart> bodyParts, List<HttpRef> httpRefs, User user);

    Workout createDefaultWorkout(int seed, List<Exercise> exercises);

    Workout createCustomWorkout(int seed, List<Exercise> exercises, User user);

    User createUser(int seed);

    User createUser(int seed, int age);

    User createAdminUser(int seed);

    User createUser(int seed, Role role, Country country);

    User createUser(int seed, Role role, Country country, int age);

    Role createUserRole();

    Role createAdminRole();

    Country createCountry(int seed);

    MentalActivity createDefaultMentalActivity(int seed, List<HttpRef> httpRefs, MentalType mentalType);

    MentalActivity createCustomMentalActivity(int seed, List<HttpRef> httpRefs, MentalType mentalType, User user);

    MentalType createMeditationType();

    MentalType createAffirmationType();

    MentalWorkout createCustomMentalWorkout(int seed, List<MentalActivity> mentalActivities, User user);

    Nutrition createDefaultNutrition(int seed, List<HttpRef> httpRefs, NutritionType nutritionType);

    Nutrition createCustomNutrition(int seed, List<HttpRef> httpRefs, NutritionType nutritionType, User user);

    NutritionType createSupplementType();

    NutritionType createRecipeType();
}
