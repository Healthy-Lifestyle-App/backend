package healthy.lifestyle.backend.workout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import healthy.lifestyle.backend.exception.ApiException;
import healthy.lifestyle.backend.exception.ErrorMessage;
import healthy.lifestyle.backend.users.model.User;
import healthy.lifestyle.backend.users.service.UserService;
import healthy.lifestyle.backend.util.DtoUtil;
import healthy.lifestyle.backend.util.TestUtil;
import healthy.lifestyle.backend.workout.dto.HttpRefCreateRequestDto;
import healthy.lifestyle.backend.workout.dto.HttpRefResponseDto;
import healthy.lifestyle.backend.workout.dto.HttpRefUpdateRequestDto;
import healthy.lifestyle.backend.workout.model.HttpRef;
import healthy.lifestyle.backend.workout.repository.HttpRefRepository;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
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

    TestUtil testUtil = new TestUtil();

    DtoUtil dtoUtil = new DtoUtil();

    @Test
    void createCustomHttpRefTest_shouldReturnHttpRefResponseDto() {
        // Given
        User user = testUtil.createUser(1);
        HttpRefCreateRequestDto createHttpRequestDto = dtoUtil.httpRefCreateRequestDto(1);

        when(userService.getUserById(user.getId())).thenReturn(user);
        when(httpRefRepository.findCustomByNameAndUserId(createHttpRequestDto.getName(), user.getId()))
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
        verify(userService, times(1)).getUserById(user.getId());
        verify(httpRefRepository, times(1)).findCustomByNameAndUserId(createHttpRequestDto.getName(), user.getId());
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
        HttpRefCreateRequestDto createHttpRequestDto = dtoUtil.httpRefCreateRequestDto(1);
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
        HttpRefCreateRequestDto createHttpRequestDto = dtoUtil.httpRefCreateRequestDto(1);
        User user = testUtil.createUser(1);
        when(userService.getUserById(user.getId())).thenReturn(user);

        HttpRef httpRef = testUtil.createCustomHttpRef(1, user);
        when(httpRefRepository.findCustomByNameAndUserId(createHttpRequestDto.getName(), user.getId()))
                .thenReturn(Optional.of(httpRef));

        // When
        ApiException exception = assertThrows(
                ApiException.class, () -> httpRefService.createCustomHttpRef(user.getId(), createHttpRequestDto));

        // Then
        verify(userService, times(1)).getUserById(user.getId());
        verify(httpRefRepository, times(1)).findCustomByNameAndUserId(createHttpRequestDto.getName(), user.getId());
        verify(httpRefRepository, times(0)).save(org.mockito.ArgumentMatchers.any(HttpRef.class));

        assertEquals(ErrorMessage.ALREADY_EXISTS.getName(), exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void getDefaultHttpRefsTest_shouldReturnDefaultHttpRefs() {
        // Given
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        HttpRef httpRef1 = testUtil.createDefaultHttpRef(1);
        HttpRef httpRef2 = testUtil.createDefaultHttpRef(2);
        List<HttpRef> httpRefsDefault = List.of(httpRef1, httpRef2);
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
    void getCustomHttpRefsTest_shouldReturnCustomHttpRefs_whenValidUserIdAndSortByProvided() {
        // Given
        HttpRef httpRef1 = testUtil.createDefaultHttpRef(1);
        HttpRef httpRef2 = testUtil.createDefaultHttpRef(2);
        HttpRef httpRef3 = testUtil.createDefaultHttpRef(3);
        HttpRef httpRef4 = testUtil.createDefaultHttpRef(4);
        List<HttpRef> httpRefs = List.of(httpRef1, httpRef2, httpRef3, httpRef4);
        long userId = 1L;
        String sortBy = "id";
        Sort sort = Sort.by(Sort.Direction.ASC, sortBy);
        when(httpRefRepository.findCustomByUserId(userId, sort)).thenReturn(httpRefs);

        // When
        List<HttpRefResponseDto> httpRefsActual = httpRefService.getCustomHttpRefs(userId, "id");

        // Then
        verify(httpRefRepository, times(1)).findCustomByUserId(userId, sort);

        org.hamcrest.MatcherAssert.assertThat(httpRefsActual, hasSize(httpRefs.size()));

        assertThat(httpRefs)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("exercises", "user")
                .isEqualTo(httpRefsActual);
    }

    @Test
    void updateCustomHttpRefTest_shouldReturnHttpRefResponseDto_whenValidUpdateDtoProvided() {
        // Given
        User user = testUtil.createUser(1);
        HttpRef httpRef = testUtil.createCustomHttpRef(1, user);
        HttpRefUpdateRequestDto requestDto = dtoUtil.httpRefUpdateRequestDto(1);

        when(httpRefRepository.findById(httpRef.getId())).thenReturn(Optional.of(httpRef));
        when(httpRefRepository.save(httpRef)).thenReturn(httpRef);

        // When
        HttpRefResponseDto responseDto = httpRefService.updateCustomHttpRef(user.getId(), httpRef.getId(), requestDto);

        // Then
        verify(httpRefRepository, times(1)).findById(httpRef.getId());
        verify(httpRefRepository, times(1)).save(httpRef);

        assertEquals(requestDto.getName(), responseDto.getName());
        assertEquals(requestDto.getDescription(), responseDto.getDescription());
        assertEquals(requestDto.getRef(), responseDto.getRef());
        assertEquals(httpRef.getId(), responseDto.getId());
    }

    @Test
    void updateCustomHttpRefTest_shouldReturnNotFoundAnd400_whenHttpRefNotFound() {
        // Given
        HttpRefUpdateRequestDto requestDto = dtoUtil.httpRefUpdateRequestDto(1);
        when(httpRefRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When
        ApiException exception =
                assertThrows(ApiException.class, () -> httpRefService.updateCustomHttpRef(1L, 2L, requestDto));

        // Then
        verify(httpRefRepository, times(1)).findById(anyLong());
        verify(httpRefRepository, times(0)).save(ArgumentMatchers.any(HttpRef.class));

        assertEquals(ErrorMessage.NOT_FOUND.getName(), exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void updateCustomHttpRefTest_shouldReturnErrorMessageAnd400_whenDefaultMediaIsRequestedToUpdate() {
        // Given
        User user = testUtil.createUser(1);
        HttpRef httpRef = testUtil.createDefaultHttpRef(1);
        HttpRefUpdateRequestDto requestDto = dtoUtil.httpRefUpdateRequestDto(1);
        when(httpRefRepository.findById(httpRef.getId())).thenReturn(Optional.of(httpRef));

        // When
        ApiException exception = assertThrows(
                ApiException.class,
                () -> httpRefService.updateCustomHttpRef(user.getId(), httpRef.getId(), requestDto));

        // Then
        verify(httpRefRepository, times(1)).findById(httpRef.getId());
        verify(httpRefRepository, times(0)).save(ArgumentMatchers.any(HttpRef.class));

        assertEquals(ErrorMessage.DEFAULT_MEDIA_IS_NOT_ALLOWED_TO_MODIFY.getName(), exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void updateCustomHttpRefTest_shouldReturnErrorMessageAnd400_whenUserResourceMismatch() {
        // Given
        User user = testUtil.createUser(1);
        HttpRef httpRef = testUtil.createCustomHttpRef(1, user);
        HttpRefUpdateRequestDto requestDto = dtoUtil.httpRefUpdateRequestDto(1);
        when(httpRefRepository.findById(httpRef.getId())).thenReturn(Optional.of(httpRef));
        long wrongUserId = user.getId() + 1;

        // When
        ApiException exception = assertThrows(
                ApiException.class, () -> httpRefService.updateCustomHttpRef(wrongUserId, httpRef.getId(), requestDto));

        // Then
        verify(httpRefRepository, times(1)).findById(httpRef.getId());
        verify(httpRefRepository, times(0)).save(ArgumentMatchers.any(HttpRef.class));

        assertEquals(ErrorMessage.USER_RESOURCE_MISMATCH.getName(), exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void updateCustomHttpRefTest_shouldReturnErrorMessageAnd400_whenEmptyDtoProvided() {
        // Given
        HttpRefUpdateRequestDto requestDto = dtoUtil.httpRefUpdateRequestDto(1);
        requestDto.setName(null);
        requestDto.setDescription(null);
        requestDto.setRef(null);

        // When
        ApiException exception =
                assertThrows(ApiException.class, () -> httpRefService.updateCustomHttpRef(1L, 2L, requestDto));

        // Then
        verify(httpRefRepository, times(0)).findById(anyLong());
        verify(httpRefRepository, times(0)).save(ArgumentMatchers.any(HttpRef.class));

        assertEquals(ErrorMessage.EMPTY_REQUEST.getName(), exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void deleteCustomHttpRefTest_shouldReturnDeletedHttpRefId() {
        // Given
        User user = testUtil.createUser(1);
        HttpRef httpRef = testUtil.createCustomHttpRef(1, user);
        when(httpRefRepository.findById(httpRef.getId())).thenReturn(Optional.of(httpRef));

        // When
        long deletedId = httpRefService.deleteCustomHttpRef(user.getId(), httpRef.getId());

        // Then
        verify(httpRefRepository, times(1)).findById(httpRef.getId());

        assertEquals(httpRef.getId(), deletedId);
    }

    @Test
    void deleteCustomHttpRefTest_shouldReturnNotFoundAnd400_whenHttpRefNotFound() {
        // Given
        when(httpRefRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When
        ApiException exception = assertThrows(ApiException.class, () -> httpRefService.deleteCustomHttpRef(1L, 2L));

        // Then
        verify(httpRefRepository, times(1)).findById(anyLong());

        assertEquals(ErrorMessage.NOT_FOUND.getName(), exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void deleteCustomHttpRefTest_shouldReturnErrorMessageAnd400_whenDefaultHttpRefIsRequestedToDelete() {
        // Given
        User user = testUtil.createUser(1);
        HttpRef httpRef = testUtil.createDefaultHttpRef(1);
        when(httpRefRepository.findById(httpRef.getId())).thenReturn(Optional.of(httpRef));

        // When
        ApiException exception = assertThrows(
                ApiException.class, () -> httpRefService.deleteCustomHttpRef(user.getId(), httpRef.getId()));

        // Then
        verify(httpRefRepository, times(1)).findById(httpRef.getId());

        assertEquals(ErrorMessage.DEFAULT_MEDIA_IS_NOT_ALLOWED_TO_MODIFY.getName(), exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void deleteCustomHttpRefTest_shouldReturnErrorMessageAnd400_whenUserResourceMismatch() {
        // Given
        User user = testUtil.createUser(1);
        HttpRef httpRef = testUtil.createCustomHttpRef(1, user);
        when(httpRefRepository.findById(httpRef.getId())).thenReturn(Optional.of(httpRef));
        long wrongUserId = user.getId() + 1;

        // When
        ApiException exception = assertThrows(
                ApiException.class, () -> httpRefService.deleteCustomHttpRef(wrongUserId, httpRef.getId()));

        // Then
        verify(httpRefRepository, times(1)).findById(httpRef.getId());

        assertEquals(ErrorMessage.USER_RESOURCE_MISMATCH.getName(), exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void getCustomHttpRefByIdTest_shouldReturnHttpRefResponseDto() {
        // Given
        User user = testUtil.createUser(1);
        HttpRef httpRef = testUtil.createCustomHttpRef(1, user);
        when(httpRefRepository.findById(httpRef.getId())).thenReturn(Optional.of(httpRef));

        // When
        HttpRefResponseDto responseDto = httpRefService.getCustomHttpRefById(user.getId(), httpRef.getId());

        // Then
        verify(httpRefRepository, times(1)).findById(httpRef.getId());

        assertThat(responseDto)
                .usingRecursiveComparison()
                .ignoringFields("exercises", "user")
                .isEqualTo(httpRef);
    }

    @Test
    void getCustomHttpRefByIdTest_shouldReturnNotFoundAnd400_whenHttpRefNotFound() {
        // Given
        when(httpRefRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When
        ApiException exception = assertThrows(ApiException.class, () -> httpRefService.getCustomHttpRefById(1L, 2L));

        // Then
        verify(httpRefRepository, times(1)).findById(anyLong());

        assertEquals(ErrorMessage.NOT_FOUND.getName(), exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void getCustomHttpRefByIdTest_shouldReturnErrorMessageAnd400_whenDefaultMediaIsRequestedToDelete() {
        // Given
        User user = testUtil.createUser(1);
        HttpRef httpRef = testUtil.createDefaultHttpRef(1);
        when(httpRefRepository.findById(httpRef.getId())).thenReturn(Optional.of(httpRef));

        // When
        ApiException exception = assertThrows(
                ApiException.class, () -> httpRefService.getCustomHttpRefById(user.getId(), httpRef.getId()));

        // Then
        verify(httpRefRepository, times(1)).findById(httpRef.getId());

        assertEquals(ErrorMessage.DEFAULT_MEDIA_REQUESTED.getName(), exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void getCustomHttpRefByIdTest_shouldReturnErrorMessageAnd400_whenUserResourceMismatch() {
        // Given
        User user = testUtil.createUser(1);
        HttpRef httpRef = testUtil.createCustomHttpRef(1, user);
        when(httpRefRepository.findById(httpRef.getId())).thenReturn(Optional.of(httpRef));
        long wrongUserId = user.getId() + 1;

        // When
        ApiException exception = assertThrows(
                ApiException.class, () -> httpRefService.getCustomHttpRefById(wrongUserId, httpRef.getId()));

        // Then
        verify(httpRefRepository, times(1)).findById(httpRef.getId());

        assertEquals(ErrorMessage.USER_RESOURCE_MISMATCH.getName(), exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }
}
