package healthy.lifestyle.backend.workout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseResponseDto {
    private Long id;

    private String title;

    private String description;

    @JsonProperty(value = "isCustom")
    private boolean isCustom;

    private boolean needsEquipment;

    private List<BodyPartResponseDto> bodyParts;

    private List<HttpRefResponseDto> httpRefs;
}
