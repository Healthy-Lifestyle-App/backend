package healthy.lifestyle.backend.user.model;

import healthy.lifestyle.backend.activity.mental.model.MentalActivity;
import healthy.lifestyle.backend.activity.mental.model.MentalWorkout;
import healthy.lifestyle.backend.activity.nutrition.model.Nutrition;
import healthy.lifestyle.backend.activity.workout.model.BodyPart;
import healthy.lifestyle.backend.activity.workout.model.Exercise;
import healthy.lifestyle.backend.activity.workout.model.HttpRef;
import healthy.lifestyle.backend.activity.workout.model.Workout;
import healthy.lifestyle.backend.notification.model.FirebaseUserToken;
import healthy.lifestyle.backend.reminder.workout.model.WorkoutReminder;
import jakarta.persistence.*;
import java.util.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = true, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", nullable = true, unique = false)
    private String fullName;

    @Column(name = "password", nullable = false, unique = false)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id") // FOREIGN KEY(role_id) REFERENCES roles(id)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id") // FOREIGN KEY(country_id) REFERENCES countries(id)
    private Country country;

    @Column(name = "age", nullable = true, unique = false)
    private Integer age;

    @OneToMany(mappedBy = "user")
    private Set<Exercise> exercises;

    @OneToMany(mappedBy = "user")
    private Set<Workout> workouts;

    @OneToMany(mappedBy = "user")
    private Set<HttpRef> httpRefs;

    @OneToMany(mappedBy = "user")
    private Set<MentalActivity> mentalActivities;

    @OneToMany(mappedBy = "user")
    private Set<MentalWorkout> mentalWorkouts;

    @OneToMany(mappedBy = "user")
    private Set<Nutrition> nutritions;

    @OneToMany(mappedBy = "user")
    private Set<WorkoutReminder> workoutReminders;

    // In fact, one-to-one
    @OneToMany(mappedBy = "user")
    private Set<Profile> profile;

    @OneToMany(mappedBy = "user")
    private Set<FirebaseUserToken> firebaseUserTokens;

    public List<Exercise> getExercisesSortedById() {
        return this.getExercises().stream()
                .sorted(Comparator.comparingLong(Exercise::getId))
                .toList();
    }

    public List<Long> getExercisesIdsSorted() {
        return this.getExercisesSortedById().stream().map(Exercise::getId).toList();
    }

    public List<Workout> getWorkoutsSortedById() {
        return this.getWorkouts().stream()
                .sorted(Comparator.comparingLong(Workout::getId))
                .toList();
    }

    public List<Long> getWorkoutsIdsSorted() {
        return this.getWorkoutsSortedById().stream().map(Workout::getId).toList();
    }

    public List<HttpRef> getHttpRefsSortedById() {
        return this.getHttpRefs().stream()
                .sorted(Comparator.comparingLong(HttpRef::getId))
                .toList();
    }

    public List<Long> getHttpRefsIdsSorted() {
        return this.getHttpRefsSortedById().stream().map(HttpRef::getId).toList();
    }

    public List<BodyPart> getDistinctBodyPartsSortedById() {
        Set<BodyPart> bodyParts = new HashSet<>();
        this.getExercises().forEach(elt -> bodyParts.addAll(elt.getBodyParts()));
        return bodyParts.stream().toList();
    }

    public List<MentalActivity> getMentalsSortedById() {
        return this.getMentalActivities().stream()
                .sorted(Comparator.comparingLong(MentalActivity::getId))
                .toList();
    }

    public List<Nutrition> getNutritionsSortedById() {
        return this.getNutritions().stream()
                .sorted(Comparator.comparingLong(Nutrition::getId))
                .toList();
    }
}
