package healthy.lifestyle.backend.util;

public class URL {
    public static final String SIGNUP = "/api/v1/users/auth/signup";

    public static final String LOGIN = "/api/v1/users/auth/login";

    public static final String VALIDATE = "/api/v1/users/auth/validate";

    public static final String USERS = "/api/v1/users";

    public static final String USER_ID = "/api/v1/users/{userId}";

    public static final String COUNTRIES = "/api/v1/users/countries";

    public static final String BODY_PARTS = "/api/v1/workouts/bodyParts";

    public static final String CUSTOM_HTTP_REFS = "/api/v1/workouts/httpRefs";

    public static final String DEFAULT_HTTP_REFS = "/api/v1/workouts/httpRefs/default";

    public static final String CUSTOM_HTTP_REF_ID = "/api/v1/workouts/httpRefs/{httpRefId}";

    public static final String DEFAULT_HTTP_REF_ID = "/api/v1/workouts/httpRefs/default/{httpRefId}";

    public static final String CUSTOM_EXERCISES = "/api/v1/workouts/exercises";

    public static final String DEFAULT_EXERCISES = "/api/v1/workouts/exercises/default";

    public static final String CUSTOM_EXERCISE_ID = "/api/v1/workouts/exercises/{exerciseId}";

    public static final String DEFAULT_EXERCISE_ID = "/api/v1/workouts/exercises/default/{exerciseId}";

    public static final String ADMIN_EXERCISES = "/api/v1/admin/exercises";

    public static final String CUSTOM_WORKOUTS = "/api/v1/workouts";

    public static final String DEFAULT_WORKOUTS = "/api/v1/workouts/default";

    public static final String CUSTOM_WORKOUT_ID = "/api/v1/workouts/{workoutId}";

    public static final String DEFAULT_WORKOUT_ID = "/api/v1/workouts/default/{workoutId}";
}
