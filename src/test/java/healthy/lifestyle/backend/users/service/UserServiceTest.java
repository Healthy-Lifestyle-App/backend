package healthy.lifestyle.backend.users.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import healthy.lifestyle.backend.data.DataUtil;
import healthy.lifestyle.backend.users.dto.SignupRequestDto;
import healthy.lifestyle.backend.users.dto.SignupResponseDto;
import healthy.lifestyle.backend.users.dto.UpdateUserRequestDto;
import healthy.lifestyle.backend.users.dto.UserResponseDto;
import healthy.lifestyle.backend.users.model.Country;
import healthy.lifestyle.backend.users.model.Role;
import healthy.lifestyle.backend.users.model.User;
import healthy.lifestyle.backend.users.repository.CountryRepository;
import healthy.lifestyle.backend.users.repository.RoleRepository;
import healthy.lifestyle.backend.users.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @InjectMocks
    UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CountryRepository countryRepository;

    @Spy
    private PasswordEncoder passwordEncoder;

    @Spy
    private DataUtil dataUtil;

    @Spy
    ModelMapper modelMapper;

    @Test
    void createUserTest_shouldReturnUserDto() {
        // Given
        Integer age = 20;
        SignupRequestDto signupRequestDto = dataUtil.createSignupRequestDto("one", 1L, age);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);

        Role role = new Role.Builder().id(1L).name("ROLE_USER").build();
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.ofNullable(role));
        Country country = Country.builder().id(1L).name("Country").build();
        when(countryRepository.getReferenceById(1L)).thenReturn(country);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            User saved = (User) args[0];
            saved.setId(1L);
            return saved;
        });

        // When
        SignupResponseDto responseDto = userService.createUser(signupRequestDto);

        // Then
        assertEquals(1L, responseDto.getId());
        assertEquals(age, signupRequestDto.getAge());
    }

    @Test
    void updateUserTest_shouldReturnUserDto() {
        // Given
        Country country = Country.builder().id(1L).name("Country").build();
        User user = dataUtil.createUserEntity(1);
        user.setCountry(country);

        UpdateUserRequestDto updateUserRequestDto = dataUtil.createUpdateUserRequestDto("one", 1L, 25);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(countryRepository.findById(updateUserRequestDto.getUpdatedCountryId()))
                .thenReturn(Optional.of(country));
        when(userRepository.save(user)).thenReturn(user);

        // When
        UserResponseDto updatedUserResponse = userService.updateUser(user.getId(), updateUserRequestDto);

        // Then
        assertEquals(updatedUserResponse.getUsername(), updateUserRequestDto.getUpdatedUsername());
        assertEquals(updatedUserResponse.getEmail(), updateUserRequestDto.getUpdatedEmail());
        assertEquals(updatedUserResponse.getFullName(), updateUserRequestDto.getUpdatedFullName());
        assertEquals(25, updatedUserResponse.getAge());
        assertEquals(1L, updatedUserResponse.getCountryId());
        assertEquals(user.getId(), updatedUserResponse.getId());
    }

    @Test
    void testDeleteUser() {
        // Given
        Long userId = 1L;

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository, times(1)).deleteById(userId);
    }
}
