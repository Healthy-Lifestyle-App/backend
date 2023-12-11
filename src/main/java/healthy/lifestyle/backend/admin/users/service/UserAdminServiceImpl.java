package healthy.lifestyle.backend.admin.users.service;

import healthy.lifestyle.backend.admin.users.repository.UserAdminRepository;
import healthy.lifestyle.backend.exception.ApiException;
import healthy.lifestyle.backend.exception.ErrorMessage;
import healthy.lifestyle.backend.users.dto.UserResponseDto;
import healthy.lifestyle.backend.users.model.Country;
import healthy.lifestyle.backend.users.model.Role;
import healthy.lifestyle.backend.users.model.User;
import java.util.Comparator;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private final UserAdminRepository userAdminRepository;

    private final ModelMapper modelMapper;

    public UserAdminServiceImpl(UserAdminRepository userAdminRepository, ModelMapper modelMapper) {
        this.userAdminRepository = userAdminRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public List<UserResponseDto> getUsersByFilters(
            Role role, String username, String email, String fullName, Country country, Integer age) {
        List<User> users = userAdminRepository
                .findByFilters(role, username, email, fullName, country, age)
                .orElseThrow(() -> new ApiException(ErrorMessage.NOT_FOUND, HttpStatus.NOT_FOUND));

        return users.stream()
                .map(user -> modelMapper.map(user, UserResponseDto.class))
                .sorted(Comparator.comparing(UserResponseDto::getId))
                .toList();
    }
}
