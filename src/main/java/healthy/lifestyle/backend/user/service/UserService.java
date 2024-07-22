package healthy.lifestyle.backend.user.service;

import healthy.lifestyle.backend.activity.mental.model.MentalActivity;
import healthy.lifestyle.backend.activity.mental.model.MentalWorkout;
import healthy.lifestyle.backend.activity.workout.model.Exercise;
import healthy.lifestyle.backend.activity.workout.model.Workout;
import healthy.lifestyle.backend.user.dto.*;
import healthy.lifestyle.backend.user.model.User;

public interface UserService {
    void createUser(SignupRequestDto requestDto);

    User getUserById(long userId);

    UserResponseDto getUserDetailsById(long userId);

    UserResponseDto updateUser(Long userId, UserUpdateRequestDto requestDto)
            throws NoSuchFieldException, IllegalAccessException;

    void deleteUser(long userId);

    void addExerciseToUser(long userId, Exercise exercise);

    void deleteExerciseFromUser(long userId, Exercise exercise);

    void addWorkoutToUser(User user, Workout workout);

    void deleteWorkoutFromUser(User user, Workout workout);

    void deleteMentalActivitiesFromUser(long userId, MentalActivity mental);

    void addMentalActivitiesToUser(long userId, MentalActivity mental);

    void addMentalWorkoutToUser(User user, MentalWorkout mentalWorkout);

    void deleteMentalWorkoutFromUser(User user, MentalWorkout mentalWorkout);
}
