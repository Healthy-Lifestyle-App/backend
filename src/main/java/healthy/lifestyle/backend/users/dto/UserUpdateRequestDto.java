package healthy.lifestyle.backend.users.dto;

import healthy.lifestyle.backend.validation.*;
import jakarta.validation.constraints.*;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@PasswordsMatchValidation.List({
    @PasswordsMatchValidation(
            password = "password",
            confirmPassword = "confirmPassword",
            message = "Passwords must match")
})
public class UserUpdateRequestDto {
    @Size(min = 6, max = 20)
    @UsernameValidation
    private String username;

    @Email
    @Size(min = 6, max = 64)
    @EmailValidation
    private String email;

    @Size(min = 10, max = 64)
    @PasswordValidation
    private String password;

    @Size(min = 10, max = 64)
    @PasswordValidation
    private String confirmPassword;

    @Size(min = 4, max = 64)
    @FullnameValidation
    private String fullName;

    @NotNull
    private Long countryId;

    @AgeValidation
    private Integer updatedAge;
}
