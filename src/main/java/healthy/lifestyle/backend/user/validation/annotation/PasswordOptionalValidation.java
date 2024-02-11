package healthy.lifestyle.backend.user.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordOptionalValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordOptionalValidation {
    String message() default "{validation.message.password.allowed-symbols}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
