package healthy.lifestyle.backend.workout.dto;

import healthy.lifestyle.backend.validation.DescriptionValidation;
import healthy.lifestyle.backend.validation.TitleValidation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutUpdateRequestDto {
    @TitleValidation
    @Size(min = 5, max = 255, message = "Size should be from 5 to 255 characters long")
    private String title;

    @DescriptionValidation
    private String description;

    @NotNull private List<Long> exerciseIds;
}
