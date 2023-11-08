package healthy.lifestyle.backend.workout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import healthy.lifestyle.backend.data.DataUtil;
import healthy.lifestyle.backend.exception.ApiException;
import healthy.lifestyle.backend.exception.ErrorMessage;
import healthy.lifestyle.backend.users.model.User;
import healthy.lifestyle.backend.users.service.UserService;
import healthy.lifestyle.backend.workout.dto.CreateHttpRequestDto;
import healthy.lifestyle.backend.workout.dto.HttpRefResponseDto;
import healthy.lifestyle.backend.workout.model.HttpRef;
import healthy.lifestyle.backend.workout.repository.HttpRefRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class HttpRefServiceTest {
    @InjectMocks
    HttpRefServiceImpl httpRefService;

    @Mock
    HttpRefRepository httpRefRepository;

    @Mock
    UserService userService;

    @Spy
    ModelMapper modelMapper;

    DataUtil dataUtil = new DataUtil();

    @Test
    void getHttpRefsTest_shouldReturnDefaultAndCustomRefs() {
        // Given
        List<HttpRef> httpRefsDefault = dataUtil.createHttpRefs(1, 2, false);
        List<HttpRef> httpRefsCustom = dataUtil.createHttpRefs(3, 4, true);
        List<HttpRef> httpRefs = new ArrayList<>(httpRefsDefault);
        httpRefs.addAll(httpRefsCustom);
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        long userId = 1L;
        when(httpRefRepository.findAllDefault(sort)).thenReturn(httpRefsDefault);
        when(httpRefRepository.findCustomByUserId(userId, sort)).thenReturn(httpRefsCustom);

        // When
        List<HttpRefResponseDto> httpRefsActual = httpRefService.getHttpRefs(userId, sort, false);

        // Then
        verify(httpRefRepository, times(1)).findAllDefault(sort);
        verify(httpRefRepository, times(1)).findCustomByUserId(userId, sort);

        org.hamcrest.MatcherAssert.assertThat(httpRefsActual, hasSize(httpRefs.size()));

        assertThat(httpRefs)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("exercises", "user")
                .isEqualTo(httpRefsActual);
    }

    @Test
    void getHttpRefsTest_shouldReturnDefaultOnlyRefs() {
        // When
        List<HttpRef> httpRefs = dataUtil.createHttpRefs(1, 2, false);
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        long userId = 1L;
        when(httpRefRepository.findAllDefault(sort)).thenReturn(httpRefs);

        // When
        List<HttpRefResponseDto> httpRefsActual = httpRefService.getHttpRefs(userId, sort, true);

        // Then
        verify(httpRefRepository, times(1)).findAllDefault(sort);
        verify(httpRefRepository, times(0)).findCustomByUserId(userId, sort);

        org.hamcrest.MatcherAssert.assertThat(httpRefsActual, hasSize(httpRefs.size()));

        Assertions.assertThat(httpRefs)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("exercises", "user")
                .isEqualTo(httpRefsActual);
    }

    @Test
    void getHttpRefsTest_shouldThrowException_whenNoRefs() {
        // Given
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        long userId = 1L;
        when(httpRefRepository.findAllDefault(sort)).thenReturn(new ArrayList<>());
        when(httpRefRepository.findCustomByUserId(userId, sort)).thenReturn(new ArrayList<>());

        // When
        ApiException exception =
                assertThrows(ApiException.class, () -> httpRefService.getHttpRefs(userId, sort, false));

        // Then
        verify(httpRefRepository, times(1)).findAllDefault(sort);
        verify(httpRefRepository, times(1)).findCustomByUserId(userId, sort);

        assertEquals(ErrorMessage.SERVER_ERROR.getName(), exception.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
    }

    @Test
    void getDefaultHttpRefsTest_shouldReturnDefaultHttpRefs() {
        // Given
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        List<HttpRef> httpRefsDefault = dataUtil.createHttpRefs(1, 2, false);
        when(httpRefRepository.findAllDefault(sort)).thenReturn(httpRefsDefault);

        // When
        List<HttpRefResponseDto> httpRefsActual = httpRefService.getDefaultHttpRefs(sort);

        // Then
        verify(httpRefRepository, times(1)).findAllDefault(sort);

        org.hamcrest.MatcherAssert.assertThat(httpRefsActual, hasSize(httpRefsDefault.size()));

        Assertions.assertThat(httpRefsDefault)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("exercises", "user")
                .isEqualTo(httpRefsActual);
    }

    @Test
    void createCustomHttpRefTest_shouldReturnHttpRefResponseDto() {
        // Given
        CreateHttpRequestDto createHttpRequestDto = dataUtil.createHttpRequestDto(1);
        long userId = 1L;
        User user = dataUtil.createUserEntity(userId);
        when(userService.getUserById(user.getId())).thenReturn(user);
        when(httpRefRepository.findCustomByNameAndUserId(createHttpRequestDto.getName(), userId))
                .thenReturn(Optional.empty());
        when(httpRefRepository.save(org.mockito.ArgumentMatchers.any(HttpRef.class)))
                .thenAnswer(invocation -> {
                    Object[] args = invocation.getArguments();
                    HttpRef saved = (HttpRef) args[0];
                    saved.setId(1L);
                    return saved;
                });

        // When
        HttpRefResponseDto httpRefResponseDto = httpRefService.createCustomHttpRef(user.getId(), createHttpRequestDto);

        // Then
        verify(userService, times(1)).getUserById(userId);
        verify(httpRefRepository, times(1)).findCustomByNameAndUserId(createHttpRequestDto.getName(), userId);
        verify(httpRefRepository, times(1)).save(org.mockito.ArgumentMatchers.any(HttpRef.class));

        assertThat(httpRefResponseDto)
                .usingRecursiveComparison()
                .ignoringFields("id", "isCustom")
                .isEqualTo(createHttpRequestDto);

        assertTrue(httpRefResponseDto.isCustom());
        assertEquals(1L, httpRefResponseDto.getId());
    }

    @Test
    void createCustomHttpRefTest_shouldReturnUserNotFoundAndBadRequest_whenInvalidUserIdProvided() {
        // Given
        CreateHttpRequestDto createHttpRequestDto = dataUtil.createHttpRequestDto(1);
        long userId = 1L;
        when(userService.getUserById(userId)).thenReturn(null);

        // When
        ApiException exception = assertThrows(
                ApiException.class, () -> httpRefService.createCustomHttpRef(userId, createHttpRequestDto));

        // Then
        verify(userService, times(1)).getUserById(userId);
        verify(httpRefRepository, times(0)).findCustomByNameAndUserId(createHttpRequestDto.getName(), userId);
        verify(httpRefRepository, times(0)).save(org.mockito.ArgumentMatchers.any(HttpRef.class));

        assertEquals(ErrorMessage.USER_NOT_FOUND.getName(), exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void createCustomHttpRefTest_shouldReturnAlreadyExistsAndBadRequest_whenNameDuplicated() {
        // Given
        CreateHttpRequestDto createHttpRequestDto = dataUtil.createHttpRequestDto(1);
        long userId = 1L;
        User user = dataUtil.createUserEntity(userId);
        when(userService.getUserById(user.getId())).thenReturn(user);

        HttpRef httpRef = dataUtil.createHttpRef(1, true);
        when(httpRefRepository.findCustomByNameAndUserId(createHttpRequestDto.getName(), userId))
                .thenReturn(Optional.of(httpRef));

        // When
        ApiException exception = assertThrows(
                ApiException.class, () -> httpRefService.createCustomHttpRef(user.getId(), createHttpRequestDto));

        // Then
        verify(userService, times(1)).getUserById(userId);
        verify(httpRefRepository, times(1)).findCustomByNameAndUserId(createHttpRequestDto.getName(), userId);
        verify(httpRefRepository, times(0)).save(org.mockito.ArgumentMatchers.any(HttpRef.class));

        assertEquals(ErrorMessage.ALREADY_EXISTS.getName(), exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }
}
