package healthy.lifestyle.backend.plan.workout.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "workout_json_ids")
public class WorkoutPlanDayId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "json_id", nullable = false, unique = false)
    private Long json_id;
}
