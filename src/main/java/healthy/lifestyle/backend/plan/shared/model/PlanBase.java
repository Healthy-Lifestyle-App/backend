package healthy.lifestyle.backend.plan.shared.model;

import healthy.lifestyle.backend.user.model.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public class PlanBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_date", nullable = false, unique = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false, unique = false)
    private LocalDate endDate;

    @Column(name = "is_active", nullable = false, unique = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, unique = false)
    private LocalDateTime createdAt;

    @Column(name = "deactivated_at", nullable = false, unique = false)
    private LocalDateTime deactivatedAt;
}
