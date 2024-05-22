package healthy.lifestyle.backend.plan.workout.model;

import com.fasterxml.jackson.databind.JsonNode;
import healthy.lifestyle.backend.activity.workout.model.Workout;
import healthy.lifestyle.backend.calendar.model.PlanBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "workout_plans")
public class WorkoutPlan extends PlanBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "json_description", columnDefinition = "jsonb")
    private JsonNode jsonDescription;
}
