package healthy.lifestyle.backend.workout.dto;

import healthy.lifestyle.backend.validation.DescriptionValidation;
import healthy.lifestyle.backend.validation.HttpValidation;
import healthy.lifestyle.backend.validation.TitleValidation;
import jakarta.validation.constraints.Size;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class HttpRefUpdateRequestDto {
    @Size(min = 5, max = 255, message = "Size should be from 5 to 255 characters long")
    @TitleValidation
    private String name;

    @DescriptionValidation
    private String description;

    @HttpValidation
    private String ref;
}
