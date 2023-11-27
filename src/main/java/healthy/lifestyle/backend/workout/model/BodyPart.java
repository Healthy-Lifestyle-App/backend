package healthy.lifestyle.backend.workout.model;

import healthy.lifestyle.backend.exception.ExceptionGeneric;
import jakarta.persistence.*;
import java.util.Set;
import lombok.*;

/**
 * body_parts table contains default values only.
 * Users cannot change values in this table.
 */
@Entity
@Table(name = "body_parts")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class BodyPart implements ExceptionGeneric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @ManyToMany(mappedBy = "bodyParts")
    @OrderBy("id")
    private Set<Exercise> exercises;
}
