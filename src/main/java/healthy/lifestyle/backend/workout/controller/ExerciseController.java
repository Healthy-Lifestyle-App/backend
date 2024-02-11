package healthy.lifestyle.backend.workout.controller;

import healthy.lifestyle.backend.shared.validation.annotation.*;
import healthy.lifestyle.backend.user.service.AuthUtil;
import healthy.lifestyle.backend.workout.dto.ExerciseCreateRequestDto;
import healthy.lifestyle.backend.workout.dto.ExerciseResponseDto;
import healthy.lifestyle.backend.workout.dto.ExerciseUpdateRequestDto;
import healthy.lifestyle.backend.workout.service.ExerciseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@Controller
@RequestMapping("${api.basePath}/${api.version}/workouts/exercises")
public class ExerciseController {
    @Autowired
    ExerciseService exerciseService;

    @Autowired
    AuthUtil authUtil;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<ExerciseResponseDto> createCustomExercise(
            @Valid @RequestBody ExerciseCreateRequestDto requestDto) {
        Long userId = authUtil.getUserIdFromAuthentication(
                SecurityContextHolder.getContext().getAuthentication());
        ExerciseResponseDto responseDto = exerciseService.createCustomExercise(requestDto, userId);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @GetMapping("/default/{exercise_id}")
    public ResponseEntity<ExerciseResponseDto> getDefaultExerciseById(
            @PathVariable("exercise_id") @IdValidation long exercise_id) {
        ExerciseResponseDto responseDto = exerciseService.getExerciseById(exercise_id, true, null);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/{exercise_id}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<ExerciseResponseDto> getCustomExerciseById(
            @PathVariable("exercise_id") @IdValidation long exercise_id) {
        Long userId = authUtil.getUserIdFromAuthentication(
                SecurityContextHolder.getContext().getAuthentication());
        ExerciseResponseDto responseDto = exerciseService.getExerciseById(exercise_id, false, userId);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping()
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Page<ExerciseResponseDto>> getExercisesWithFilter(
            @RequestParam(required = false) Boolean isCustom,
            @RequestParam(required = false) @TitleOptionalValidation(min = 2) String title,
            @RequestParam(required = false) @DescriptionOptionalValidation(min = 2) String description,
            @RequestParam(required = false) Boolean needsEquipment,
            @RequestParam(required = false) List<Long> bodyPartsIds,
            @RequestParam(required = false, defaultValue = "title") String sortField,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam(required = false, defaultValue = "0") int pageNumber) {
        Long userId = null;
        if (isCustom == null || isCustom)
            userId = authUtil.getUserIdFromAuthentication(
                    SecurityContextHolder.getContext().getAuthentication());
        Page<ExerciseResponseDto> dtoPage = exerciseService.getExercisesWithFilter(
                isCustom,
                userId,
                title,
                description,
                needsEquipment,
                bodyPartsIds,
                sortField,
                sortDirection,
                pageNumber,
                pageSize);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/default")
    public ResponseEntity<Page<ExerciseResponseDto>> getDefaultExercises(
            @RequestParam(required = false) @TitleOptionalValidation(min = 2) String title,
            @RequestParam(required = false) @DescriptionOptionalValidation(min = 2) String description,
            @RequestParam(required = false) Boolean needsEquipment,
            @RequestParam(required = false) List<Long> bodyPartsIds,
            @RequestParam(required = false, defaultValue = "title") String sortField,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam(required = false, defaultValue = "0") int pageNumber) {
        Page<ExerciseResponseDto> dtoPage = exerciseService.getExercisesWithFilter(
                false,
                null,
                title,
                description,
                needsEquipment,
                bodyPartsIds,
                sortField,
                sortDirection,
                pageNumber,
                pageSize);
        return ResponseEntity.ok(dtoPage);
    }

    @PatchMapping("/{exerciseId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<ExerciseResponseDto> updateCustomExercise(
            @PathVariable("exerciseId") long exerciseId, @Valid @RequestBody ExerciseUpdateRequestDto requestDto)
            throws NoSuchFieldException, IllegalAccessException {

        Long userId = authUtil.getUserIdFromAuthentication(
                SecurityContextHolder.getContext().getAuthentication());
        ExerciseResponseDto responseDto = exerciseService.updateCustomExercise(exerciseId, userId, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{exerciseId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> deleteCustomExercise(@PathVariable("exerciseId") @IdValidation long exerciseId) {
        Long userId = authUtil.getUserIdFromAuthentication(
                SecurityContextHolder.getContext().getAuthentication());
        exerciseService.deleteCustomExercise(exerciseId, userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
