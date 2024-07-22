package healthy.lifestyle.backend.user.service;

import healthy.lifestyle.backend.activity.mental.model.MentalActivity;
import healthy.lifestyle.backend.activity.mental.model.MentalWorkout;
import healthy.lifestyle.backend.activity.workout.model.Exercise;
import healthy.lifestyle.backend.activity.workout.model.Workout;
import healthy.lifestyle.backend.activity.workout.service.RemovalService;
import healthy.lifestyle.backend.exception.ApiException;
import healthy.lifestyle.backend.exception.ApiExceptionCustomMessage;
import healthy.lifestyle.backend.exception.ErrorMessage;
import healthy.lifestyle.backend.shared.util.VerificationUtil;
import healthy.lifestyle.backend.user.dto.*;
import healthy.lifestyle.backend.user.model.Country;
import healthy.lifestyle.backend.user.model.Role;
import healthy.lifestyle.backend.user.model.Timezone;
import healthy.lifestyle.backend.user.model.User;
import healthy.lifestyle.backend.user.repository.CountryRepository;
import healthy.lifestyle.backend.user.repository.RoleRepository;
import healthy.lifestyle.backend.user.repository.TimezoneRepository;
import healthy.lifestyle.backend.user.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    CountryRepository countryRepository;

    @Autowired
    TimezoneRepository timezoneRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    RemovalService removalService;

    @Autowired
    VerificationUtil verificationUtil;

    @Override
    public void createUser(SignupRequestDto requestDto) {
        if (userRepository.existsByEmail(requestDto.getEmail()))
            throw new ApiException(ErrorMessage.ALREADY_EXISTS, null, HttpStatus.BAD_REQUEST);

        if (userRepository.existsByUsername(requestDto.getUsername()))
            throw new ApiException(ErrorMessage.ALREADY_EXISTS, null, HttpStatus.BAD_REQUEST);

        Role role = roleRepository
                .findByName("ROLE_USER")
                .orElseThrow(
                        () -> new ApiException(ErrorMessage.ROLE_NOT_FOUND, null, HttpStatus.INTERNAL_SERVER_ERROR));

        Country country = countryRepository
                .findById(requestDto.getCountryId())
                .orElseThrow(() -> new ApiException(
                        ErrorMessage.COUNTRY_NOT_FOUND, requestDto.getCountryId(), HttpStatus.NOT_FOUND));

        Timezone timezone = timezoneRepository
                .findById(requestDto.getTimezoneId())
                .orElseThrow(() -> new ApiException(
                        ErrorMessage.TIMEZONE_NOT_FOUND, requestDto.getTimezoneId(), HttpStatus.NOT_FOUND));

        User user = User.builder()
                .username(requestDto.getUsername())
                .email(requestDto.getEmail())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .fullName(requestDto.getFullName())
                .role(role)
                .country(country)
                .timezone(timezone)
                .age(requestDto.getAge())
                .build();
        userRepository.save(user);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public User getUserById(long userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ApiException(ErrorMessage.USER_NOT_FOUND, userId, HttpStatus.BAD_REQUEST));
        return user;
    }

    @Override
    public UserResponseDto getUserDetailsById(long userId) {
        UserResponseDto responseDto = userRepository
                .findById(userId)
                .map(user -> modelMapper.map(user, UserResponseDto.class))
                .orElseThrow(() -> new ApiException(ErrorMessage.USER_NOT_FOUND, userId, HttpStatus.NOT_FOUND));
        return responseDto;
    }

    @Override
    public UserResponseDto updateUser(Long userId, UserUpdateRequestDto requestDto)
            throws NoSuchFieldException, IllegalAccessException {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ApiException(ErrorMessage.USER_NOT_FOUND, userId, HttpStatus.NOT_FOUND));

        boolean fieldsAreNull =
                verificationUtil.areFieldsNull(requestDto, "username", "email", "fullName", "age", "password");
        if (fieldsAreNull
                && user.getCountry().getId().equals(requestDto.getCountryId())
                && user.getTimezone().getId().equals(requestDto.getTimezoneId())) {
            throw new ApiExceptionCustomMessage(ErrorMessage.NO_UPDATES_REQUEST.getName(), HttpStatus.BAD_REQUEST);
        }

        List<String> fieldsWithSameValues =
                verificationUtil.getFieldsWithSameValues(user, requestDto, "username", "email", "fullName", "age");
        if (requestDto.getPassword() != null && passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            fieldsWithSameValues.add("password");
        }
        if (!fieldsWithSameValues.isEmpty()) {
            String errorMessage =
                    ErrorMessage.FIELDS_VALUES_ARE_NOT_DIFFERENT.getName() + String.join(", ", fieldsWithSameValues);
            throw new ApiExceptionCustomMessage(errorMessage, HttpStatus.BAD_REQUEST);
        }

        if (requestDto.getUsername() != null) {
            Optional<User> userWithSameUsernameOpt = userRepository.findByUsername(requestDto.getUsername());
            if (userWithSameUsernameOpt.isPresent()) {
                throw new ApiException(ErrorMessage.USERNAME_ALREADY_EXISTS, null, HttpStatus.BAD_REQUEST);
            }
            user.setUsername(requestDto.getUsername());
        }

        if (requestDto.getEmail() != null) {
            Optional<User> userWithSameEmailOpt = userRepository.findByEmail(requestDto.getEmail());
            if (userWithSameEmailOpt.isPresent()) {
                throw new ApiException(ErrorMessage.EMAIL_ALREADY_EXISTS, null, HttpStatus.BAD_REQUEST);
            }
            user.setEmail(requestDto.getEmail());
        }

        if (requestDto.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        }
        if (requestDto.getFullName() != null) {
            user.setFullName(requestDto.getFullName());
        }
        if (requestDto.getAge() != null) {
            user.setAge(requestDto.getAge());
        }
        if (requestDto.getCountryId() != null
                && !requestDto.getCountryId().equals(user.getCountry().getId())) {
            Country country = countryRepository
                    .findById(requestDto.getCountryId())
                    .orElseThrow(() -> new ApiException(
                            ErrorMessage.COUNTRY_NOT_FOUND, requestDto.getCountryId(), HttpStatus.NOT_FOUND));
            user.setCountry(country);
        }

        if (requestDto.getTimezoneId() != null
                && !requestDto.getTimezoneId().equals(user.getTimezone().getId())) {
            Timezone timezone = timezoneRepository
                    .findById(requestDto.getTimezoneId())
                    .orElseThrow(() -> new ApiException(
                            ErrorMessage.TIMEZONE_NOT_FOUND, requestDto.getTimezoneId(), HttpStatus.NOT_FOUND));
            user.setTimezone(timezone);
        }

        User savedUser = userRepository.save(user);
        UserResponseDto responseDto = modelMapper.map(savedUser, UserResponseDto.class);
        return responseDto;
    }

    @Override
    @Transactional
    public void deleteUser(long userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ApiException(ErrorMessage.USER_NOT_FOUND, userId, HttpStatus.NOT_FOUND));
        removalService.deleteCustomWorkouts(user.getWorkoutsIdsSorted());
        removalService.deleteCustomExercises(user.getExercisesIdsSorted());
        removalService.deleteCustomHttpRefs(user.getHttpRefsIdsSorted());
        userRepository.delete(user);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void addExerciseToUser(long userId, Exercise exercise) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ApiException(ErrorMessage.USER_NOT_FOUND, userId, HttpStatus.NOT_FOUND));
        if (user.getExercises() == null) user.setExercises(new HashSet<>());
        user.getExercises().add(exercise);
        userRepository.save(user);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteExerciseFromUser(long userId, Exercise exercise) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ApiException(ErrorMessage.USER_NOT_FOUND, userId, HttpStatus.NOT_FOUND));
        if (user.getExercises() != null) user.getExercises().remove(exercise);
        userRepository.save(user);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void addWorkoutToUser(User user, Workout workout) {
        if (user.getWorkouts() == null) user.setWorkouts(new HashSet<>());
        user.getWorkouts().add(workout);
        userRepository.save(user);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteWorkoutFromUser(User user, Workout workout) {
        if (user.getWorkouts() != null) user.getWorkouts().remove(workout);
        userRepository.save(user);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteMentalActivitiesFromUser(long userId, MentalActivity mental) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ApiException(ErrorMessage.USER_NOT_FOUND, userId, HttpStatus.NOT_FOUND));
        if (user.getMentalActivities() != null) user.getMentalActivities().remove(mental);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void addMentalActivitiesToUser(long userId, MentalActivity mental) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ApiException(ErrorMessage.USER_NOT_FOUND, userId, HttpStatus.NOT_FOUND));
        if (user.getMentalActivities() == null) user.setMentalActivities(new HashSet<>());
        user.getMentalActivities().add(mental);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void addMentalWorkoutToUser(User user, MentalWorkout mentalWorkout) {
        if (user.getMentalWorkouts() == null) user.setMentalWorkouts(new HashSet<>());
        user.getMentalWorkouts().add(mentalWorkout);
        userRepository.save(user);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteMentalWorkoutFromUser(User user, MentalWorkout mentalWorkout) {
        if (user.getMentalWorkouts() != null) user.getMentalWorkouts().remove(mentalWorkout);
        userRepository.save(user);
    }
}
