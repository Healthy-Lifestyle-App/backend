package healthy.lifestyle.backend.admin.workout.service;

import healthy.lifestyle.backend.admin.workout.repository.ExerciseAdminRepository;
import healthy.lifestyle.backend.exception.ApiException;
import healthy.lifestyle.backend.exception.ErrorMessage;
import healthy.lifestyle.backend.workout.dto.*;
import healthy.lifestyle.backend.workout.model.Exercise;
import java.util.Comparator;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ExerciseAdminServiceImpl implements ExerciseAdminService {
    private final ExerciseAdminRepository exerciseAdminRepository;

    private final ModelMapper modelMapper;

    public ExerciseAdminServiceImpl(ExerciseAdminRepository exerciseAdminRepository, ModelMapper modelMapper) {
        this.exerciseAdminRepository = exerciseAdminRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public List<ExerciseResponseDto> getExercisesByFilters(
            String title, String description, Boolean isCustom, Boolean needsEquipment) {

        List<Exercise> exercises = exerciseAdminRepository
                .findByFilters(title, description, isCustom, needsEquipment)
                .orElseThrow(() -> new ApiException(ErrorMessage.NOT_FOUND, null, HttpStatus.NOT_FOUND));

        return exercises.stream()
                .map(exercise -> modelMapper.map(exercise, ExerciseResponseDto.class))
                .peek(elt -> {
                    List<BodyPartResponseDto> bodyPartsSorted = elt.getBodyParts().stream()
                            .sorted(Comparator.comparingLong(BodyPartResponseDto::getId))
                            .toList();
                    List<HttpRefResponseDto> httpRefsSorted = elt.getHttpRefs().stream()
                            .sorted(Comparator.comparingLong(HttpRefResponseDto::getId))
                            .toList();

                    elt.setBodyParts(bodyPartsSorted);
                    elt.setHttpRefs(httpRefsSorted);
                })
                .sorted(Comparator.comparing(ExerciseResponseDto::getId))
                .toList();
    }
}
