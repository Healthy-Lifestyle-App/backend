package healthy.lifestyle.backend.validation;

import static java.util.Objects.isNull;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class HttpValidator implements ConstraintValidator<HttpValidation, String> {
    @Override
    public void initialize(HttpValidation constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return validation(value);
    }

    public boolean validation(String input) {
        if (isNull(input)) return true;
        return input.startsWith("http");
    }
}
