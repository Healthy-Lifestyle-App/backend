package healthy.lifestyle.backend.workout.controller;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import healthy.lifestyle.backend.config.BeanConfig;
import healthy.lifestyle.backend.config.ContainerConfig;
import healthy.lifestyle.backend.exception.ErrorMessage;
import healthy.lifestyle.backend.users.model.Country;
import healthy.lifestyle.backend.users.model.Role;
import healthy.lifestyle.backend.users.model.User;
import healthy.lifestyle.backend.util.DbUtil;
import healthy.lifestyle.backend.util.DtoUtil;
import healthy.lifestyle.backend.util.TestUtil;
import healthy.lifestyle.backend.util.URL;
import healthy.lifestyle.backend.workout.dto.*;
import healthy.lifestyle.backend.workout.model.BodyPart;
import healthy.lifestyle.backend.workout.model.Exercise;
import healthy.lifestyle.backend.workout.model.HttpRef;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(BeanConfig.class)
class ExerciseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    DbUtil dbUtil;

    @Autowired
    TestUtil testUtil;

    @Autowired
    DtoUtil dtoUtil;

    @Autowired
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Container
    static PostgreSQLContainer<?> postgresqlContainer =
            new PostgreSQLContainer<>(DockerImageName.parse(ContainerConfig.POSTGRES));

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresqlContainer::getUsername);
        registry.add("spring.datasource.password", postgresqlContainer::getPassword);
    }

    @BeforeEach
    void beforeEach() {
        dbUtil.deleteAll();
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void createCustomExerciseTest_shouldReturnExerciseDtoWith201_whenValidRequest() throws Exception {
        // Given
        User user = dbUtil.createUser(1);
        BodyPart bodyPart1 = dbUtil.createBodyPart(1);
        BodyPart bodyPart2 = dbUtil.createBodyPart(2);
        HttpRef defaultHttpRef = dbUtil.createDefaultHttpRef(1);
        HttpRef customHttpRef = dbUtil.createCustomHttpRef(2, user);
        boolean needsEquipment = false;
        ExerciseCreateRequestDto requestDto = dtoUtil.exerciseCreateRequestDto(
                1,
                needsEquipment,
                List.of(bodyPart1.getId(), bodyPart2.getId()),
                List.of(defaultHttpRef.getId(), customHttpRef.getId()));

        // When
        MvcResult mvcResult = mockMvc.perform(post(URL.CUSTOM_EXERCISES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                // Then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(notNullValue())))
                .andExpect(jsonPath("$.title", is(requestDto.getTitle())))
                .andExpect(jsonPath("$.description", is(requestDto.getDescription())))
                .andExpect(jsonPath("$.isCustom", is(true)))
                .andExpect(jsonPath("$.needsEquipment", is(needsEquipment)))
                .andDo(print())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        ExerciseResponseDto responseDto = objectMapper.readValue(responseContent, ExerciseResponseDto.class);

        assertEquals(
                requestDto.getBodyParts().size(), responseDto.getBodyParts().size());
        assertEquals(requestDto.getHttpRefs().size(), responseDto.getHttpRefs().size());

        assertThat(responseDto.getBodyParts())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("exercises")
                .isEqualTo(List.of(bodyPart1, bodyPart2));

        assertThat(responseDto.getHttpRefs())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("exercises", "user")
                .isEqualTo(List.of(defaultHttpRef, customHttpRef));
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void createCustomExerciseTest_shouldReturnExerciseDtoWith201_whenEmptyHttpRefsListGiven() throws Exception {
        // Given
        User user = dbUtil.createUser(1);
        BodyPart bodyPart1 = dbUtil.createBodyPart(1);
        BodyPart bodyPart2 = dbUtil.createBodyPart(2);
        boolean needsEquipment = false;
        ExerciseCreateRequestDto requestDto = dtoUtil.exerciseCreateRequestDto(
                1, needsEquipment, List.of(bodyPart1.getId(), bodyPart2.getId()), Collections.emptyList());

        // When
        MvcResult mvcResult = mockMvc.perform(post(URL.CUSTOM_EXERCISES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                // Then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(notNullValue())))
                .andExpect(jsonPath("$.title", is(requestDto.getTitle())))
                .andExpect(jsonPath("$.description", is(requestDto.getDescription())))
                .andExpect(jsonPath("$.isCustom", is(true)))
                .andExpect(jsonPath("$.needsEquipment", is(needsEquipment)))
                .andDo(print())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        ExerciseResponseDto responseDto = objectMapper.readValue(responseContent, ExerciseResponseDto.class);

        assertEquals(
                requestDto.getBodyParts().size(), responseDto.getBodyParts().size());
        assertTrue(requestDto.getHttpRefs().isEmpty());

        assertThat(responseDto.getBodyParts())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("exercises")
                .isEqualTo(List.of(bodyPart1, bodyPart2));
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void createCustomExerciseTest_shouldReturnErrorMessageWith400_whenNoBodyPartsGiven() throws Exception {
        // Given
        User user = dbUtil.createUser(1);
        HttpRef defaultHttpRef = dbUtil.createDefaultHttpRef(1);
        HttpRef customHttpRef = dbUtil.createCustomHttpRef(2, user);
        boolean needsEquipment = false;
        ExerciseCreateRequestDto requestDto = dtoUtil.exerciseCreateRequestDto(
                1, needsEquipment, Collections.emptyList(), List.of(customHttpRef.getId(), defaultHttpRef.getId()));

        // When
        mockMvc.perform(post(URL.CUSTOM_EXERCISES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid nested object")))
                .andDo(print());
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void createCustomExerciseTest_shouldReturnErrorMessageWith400_whenWrongBodyPartIdsGiven() throws Exception {
        // Given
        User user = dbUtil.createUser(1);
        boolean needsEquipment = false;
        long nonExistentBodyPartId = 1000L;
        ExerciseCreateRequestDto requestDto = dtoUtil.exerciseCreateRequestDto(
                1, needsEquipment, List.of(nonExistentBodyPartId), Collections.emptyList());

        // When
        mockMvc.perform(post(URL.CUSTOM_EXERCISES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid nested object")))
                .andDo(print());
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void createCustomExerciseTest_shouldReturnErrorMessageWith400_whenWrongHttpRefIdsGiven() throws Exception {
        // Given
        User user = dbUtil.createUser(1);
        BodyPart bodyPart1 = dbUtil.createBodyPart(1);
        BodyPart bodyPart2 = dbUtil.createBodyPart(2);
        boolean needsEquipment = false;
        long nonExistentHttpRefId = 1000L;
        ExerciseCreateRequestDto requestDto = dtoUtil.exerciseCreateRequestDto(
                1, needsEquipment, List.of(bodyPart1.getId(), bodyPart2.getId()), List.of(nonExistentHttpRefId));

        // When
        mockMvc.perform(post(URL.CUSTOM_EXERCISES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid nested object")))
                .andDo(print());
    }

    @Test
    void getDefaultExerciseByIdTest_shouldReturnDefaultExerciseDtoWith200_whenValidRequest() throws Exception {
        // Given
        BodyPart bodyPart1 = dbUtil.createBodyPart(1);
        BodyPart bodyPart2 = dbUtil.createBodyPart(2);
        HttpRef defaultHttpRef1 = dbUtil.createDefaultHttpRef(1);
        HttpRef defaultHttpRef2 = dbUtil.createDefaultHttpRef(2);
        boolean defaultExerciseNeedsEquipment = true;
        Exercise defaultExercise1 = dbUtil.createDefaultExercise(
                1, defaultExerciseNeedsEquipment, List.of(bodyPart1), List.of(defaultHttpRef1));
        Exercise defaultExercise2 = dbUtil.createDefaultExercise(
                2, defaultExerciseNeedsEquipment, List.of(bodyPart2), List.of(defaultHttpRef2));

        User user = dbUtil.createUser(1);
        HttpRef customHttpRef1 = dbUtil.createCustomHttpRef(3, user);
        HttpRef customHttpRef2 = dbUtil.createCustomHttpRef(4, user);
        boolean customExerciseNeedsEquipment = true;
        Exercise customExercise1 = dbUtil.createCustomExercise(
                3,
                customExerciseNeedsEquipment,
                List.of(bodyPart1, bodyPart2),
                List.of(defaultHttpRef1, customHttpRef1),
                user);
        Exercise customExercise2 = dbUtil.createCustomExercise(
                4,
                customExerciseNeedsEquipment,
                List.of(bodyPart1, bodyPart2),
                List.of(defaultHttpRef2, customHttpRef2),
                user);

        // When
        MvcResult mvcResult = mockMvc.perform(
                        get(URL.DEFAULT_EXERCISE_ID, defaultExercise1.getId()).contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        ExerciseResponseDto responseDto =
                objectMapper.readValue(responseContent, new TypeReference<ExerciseResponseDto>() {});

        assertThat(responseDto)
                .usingRecursiveComparison()
                .ignoringFields("bodyParts", "httpRefs", "user")
                .isEqualTo(defaultExercise1);

        assertThat(responseDto.getBodyParts())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("exercises")
                .isEqualTo(defaultExercise1.getBodyPartsSortedById());

        assertThat(responseDto.getHttpRefs())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("exercises", "user")
                .isEqualTo(defaultExercise1.getHttpRefsSortedById());
    }

    @Test
    void getDefaultExerciseByIdTest_shouldReturnErrorMessageWith404_whenDefaultExerciseNotFound() throws Exception {
        // Given
        long nonExistentDefaultExerciseId = 1000L;

        // When
        mockMvc.perform(get(URL.DEFAULT_EXERCISE_ID, nonExistentDefaultExerciseId)
                        .contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(ErrorMessage.NOT_FOUND.getName())))
                .andExpect(jsonPath("$.code", is(HttpStatus.NOT_FOUND.value())))
                .andDo(print());
    }

    @Test
    void getDefaultExercisesTest_shouldReturnDefaultExercisesDtoListWith200_whenValidRequest() throws Exception {
        // Given
        BodyPart bodyPart1 = dbUtil.createBodyPart(1);
        BodyPart bodyPart2 = dbUtil.createBodyPart(2);
        HttpRef defaultHttpRef1 = dbUtil.createDefaultHttpRef(1);
        HttpRef defaultHttpRef2 = dbUtil.createDefaultHttpRef(2);
        boolean defaultExerciseNeedsEquipment = true;
        Exercise defaultExercise1 = dbUtil.createDefaultExercise(
                1, defaultExerciseNeedsEquipment, List.of(bodyPart1), List.of(defaultHttpRef1));
        Exercise defaultExercise2 = dbUtil.createDefaultExercise(
                2, defaultExerciseNeedsEquipment, List.of(bodyPart2), List.of(defaultHttpRef2));

        User user = dbUtil.createUser(1);
        HttpRef customHttpRef1 = dbUtil.createCustomHttpRef(3, user);
        HttpRef customHttpRef2 = dbUtil.createCustomHttpRef(4, user);
        boolean customExerciseNeedsEquipment = true;
        Exercise customExercise1 = dbUtil.createCustomExercise(
                3,
                customExerciseNeedsEquipment,
                List.of(bodyPart1, bodyPart2),
                List.of(defaultHttpRef1, customHttpRef1),
                user);
        Exercise customExercise2 = dbUtil.createCustomExercise(
                4,
                customExerciseNeedsEquipment,
                List.of(bodyPart1, bodyPart2),
                List.of(defaultHttpRef2, customHttpRef2),
                user);

        // When
        MvcResult mvcResult = mockMvc.perform(get(URL.DEFAULT_EXERCISES).contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        List<ExerciseResponseDto> responseDto =
                objectMapper.readValue(responseContent, new TypeReference<List<ExerciseResponseDto>>() {});

        assertEquals(2, responseDto.size());

        assertThat(responseDto)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("bodyParts", "httpRefs", "user")
                .isEqualTo(List.of(defaultExercise1, defaultExercise2));

        assertThat(responseDto.get(0).getBodyParts())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("exercises")
                .isEqualTo(defaultExercise1.getBodyPartsSortedById());

        assertThat(responseDto.get(1).getBodyParts())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("exercises")
                .isEqualTo(defaultExercise2.getBodyPartsSortedById());

        assertThat(responseDto.get(0).getHttpRefs())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("exercises", "user")
                .isEqualTo(defaultExercise1.getHttpRefsSortedById());

        assertThat(responseDto.get(1).getHttpRefs())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("exercises", "user")
                .isEqualTo(defaultExercise2.getHttpRefsSortedById());
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void getCustomExerciseByIdTest_shouldReturnCustomExerciseDtoWith200_whenValidRequest() throws Exception {
        // Given
        BodyPart bodyPart1 = dbUtil.createBodyPart(1);
        BodyPart bodyPart2 = dbUtil.createBodyPart(2);
        HttpRef defaultHttpRef1 = dbUtil.createDefaultHttpRef(1);
        HttpRef defaultHttpRef2 = dbUtil.createDefaultHttpRef(2);
        boolean defaultExerciseNeedsEquipment = true;
        Exercise defaultExercise1 = dbUtil.createDefaultExercise(
                1, defaultExerciseNeedsEquipment, List.of(bodyPart1), List.of(defaultHttpRef1));
        Exercise defaultExercise2 = dbUtil.createDefaultExercise(
                2, defaultExerciseNeedsEquipment, List.of(bodyPart2), List.of(defaultHttpRef2));

        User user = dbUtil.createUser(1);
        HttpRef customHttpRef1 = dbUtil.createCustomHttpRef(3, user);
        HttpRef customHttpRef2 = dbUtil.createCustomHttpRef(4, user);
        boolean customExerciseNeedsEquipment = true;
        Exercise customExercise1 = dbUtil.createCustomExercise(
                3,
                customExerciseNeedsEquipment,
                List.of(bodyPart1, bodyPart2),
                List.of(defaultHttpRef1, customHttpRef1),
                user);
        Exercise customExercise2 = dbUtil.createCustomExercise(
                4,
                customExerciseNeedsEquipment,
                List.of(bodyPart1, bodyPart2),
                List.of(defaultHttpRef2, customHttpRef2),
                user);

        // When
        MvcResult mvcResult = mockMvc.perform(
                        get(URL.CUSTOM_EXERCISE_ID, customExercise1.getId()).contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        ExerciseResponseDto responseDto =
                objectMapper.readValue(responseContent, new TypeReference<ExerciseResponseDto>() {});

        assertThat(responseDto)
                .usingRecursiveComparison()
                .ignoringFields("bodyParts", "httpRefs", "user")
                .isEqualTo(customExercise1);

        assertThat(responseDto.getBodyParts())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("exercises")
                .isEqualTo(customExercise1.getBodyPartsSortedById());

        assertThat(responseDto.getHttpRefs())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("exercises", "user")
                .isEqualTo(customExercise1.getHttpRefsSortedById());
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void getCustomExerciseByIdTest_shouldReturnErrorMessageWith404_whenCustomExerciseNotFound() throws Exception {
        // Given
        long nonExistentCustomExerciseId = 1000L;

        // When
        mockMvc.perform(get(URL.CUSTOM_EXERCISE_ID, nonExistentCustomExerciseId)
                        .contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(ErrorMessage.NOT_FOUND.getName())))
                .andExpect(jsonPath("$.code", is(HttpStatus.NOT_FOUND.value())))
                .andDo(print());
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void getCustomExerciseByIdTest_shouldReturnErrorMessageWith400_whenRequestedExerciseBelongsToAnotherUser()
            throws Exception {
        // Given
        Role role = dbUtil.createUserRole();
        Country country = dbUtil.createCountry(1);
        BodyPart bodyPart = dbUtil.createBodyPart(1);
        HttpRef defaultHttpRef = dbUtil.createDefaultHttpRef(1);
        User user1 = dbUtil.createUser(1, role, country);
        User user2 = dbUtil.createUser(2, role, country);
        boolean needsEquipment = true;
        Exercise customExercise =
                dbUtil.createCustomExercise(3, needsEquipment, List.of(bodyPart), List.of(defaultHttpRef), user2);

        // When
        mockMvc.perform(get(URL.CUSTOM_EXERCISE_ID, customExercise.getId()).contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(ErrorMessage.USER_RESOURCE_MISMATCH.getName())))
                .andExpect(jsonPath("$.code", is(HttpStatus.BAD_REQUEST.value())))
                .andDo(print());
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void getCustomExercisesTest_shouldReturnCustomExercisesDtoListWith200_whenValidRequest() throws Exception {
        // Given
        Role role = dbUtil.createUserRole();
        Country country = dbUtil.createCountry(1);
        BodyPart bodyPart1 = dbUtil.createBodyPart(1);
        BodyPart bodyPart2 = dbUtil.createBodyPart(2);
        BodyPart bodyPart3 = dbUtil.createBodyPart(3);
        BodyPart bodyPart4 = dbUtil.createBodyPart(4);
        HttpRef defaultHttpRef1 = dbUtil.createDefaultHttpRef(1);
        HttpRef defaultHttpRef2 = dbUtil.createDefaultHttpRef(2);
        boolean defaultExerciseNeedsEquipment = true;
        Exercise defaultExercise1 = dbUtil.createDefaultExercise(
                1, defaultExerciseNeedsEquipment, List.of(bodyPart1), List.of(defaultHttpRef1));
        Exercise defaultExercise2 = dbUtil.createDefaultExercise(
                2, defaultExerciseNeedsEquipment, List.of(bodyPart2), List.of(defaultHttpRef2));

        User user1 = dbUtil.createUser(1, role, country);
        HttpRef customHttpRef1 = dbUtil.createCustomHttpRef(3, user1);
        HttpRef customHttpRef2 = dbUtil.createCustomHttpRef(4, user1);
        boolean customExerciseNeedsEquipment1 = true;
        Exercise customExercise1 = dbUtil.createCustomExercise(
                3,
                customExerciseNeedsEquipment1,
                List.of(bodyPart1, bodyPart2),
                List.of(defaultHttpRef1, customHttpRef1),
                user1);
        Exercise customExercise2 = dbUtil.createCustomExercise(
                4,
                customExerciseNeedsEquipment1,
                List.of(bodyPart1, bodyPart2),
                List.of(defaultHttpRef2, customHttpRef2),
                user1);

        User user2 = dbUtil.createUser(2, role, country);
        HttpRef customHttpRef3 = dbUtil.createCustomHttpRef(5, user2);
        HttpRef customHttpRef4 = dbUtil.createCustomHttpRef(6, user2);
        boolean customExerciseNeedsEquipment2 = true;
        Exercise customExercise3 = dbUtil.createCustomExercise(
                5,
                customExerciseNeedsEquipment2,
                List.of(bodyPart1, bodyPart2),
                List.of(defaultHttpRef1, customHttpRef3),
                user2);
        Exercise customExercise4 = dbUtil.createCustomExercise(
                6,
                customExerciseNeedsEquipment2,
                List.of(bodyPart1, bodyPart2),
                List.of(defaultHttpRef2, customHttpRef4),
                user2);

        // When
        MvcResult mvcResult = mockMvc.perform(get(URL.CUSTOM_EXERCISES).contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        List<ExerciseResponseDto> responseDto =
                objectMapper.readValue(responseContent, new TypeReference<List<ExerciseResponseDto>>() {});

        assertEquals(2, responseDto.size());

        assertThat(responseDto)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("bodyParts", "httpRefs", "user")
                .isEqualTo(List.of(customExercise1, customExercise2));

        assertThat(responseDto.get(0).getBodyParts())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("exercises")
                .isEqualTo(customExercise1.getBodyPartsSortedById());

        assertThat(responseDto.get(1).getBodyParts())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("exercises")
                .isEqualTo(customExercise2.getBodyPartsSortedById());

        assertThat(responseDto.get(0).getHttpRefs())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("exercises", "user")
                .isEqualTo(customExercise1.getHttpRefsSortedById());

        assertThat(responseDto.get(1).getHttpRefs())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("exercises", "user")
                .isEqualTo(customExercise2.getHttpRefsSortedById());
    }

    @ParameterizedTest
    @MethodSource("updateCustomExerciseMultipleValidInputs")
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void updateCustomExerciseTest_shouldReturnExerciseDtoWith200_whenValidRequest(
            String updateTitle, String updateDescription, Boolean updateNeedsEquipment) throws Exception {
        // Given
        User user = dbUtil.createUser(1);
        BodyPart bodyPart1 = dbUtil.createBodyPart(1);
        BodyPart bodyPart2 = dbUtil.createBodyPart(2);
        HttpRef customHttpRef1 = dbUtil.createCustomHttpRef(1, user);
        HttpRef customHttpRef2 = dbUtil.createCustomHttpRef(2, user);
        HttpRef defaultHttpRef1 = dbUtil.createDefaultHttpRef(3);
        HttpRef defaultHttpRef2 = dbUtil.createDefaultHttpRef(4);
        boolean customExerciseNeedsEquipment = true;
        Exercise customExercise = dbUtil.createCustomExercise(
                1,
                customExerciseNeedsEquipment,
                List.of(bodyPart1, bodyPart2),
                List.of(customHttpRef1, customHttpRef2, defaultHttpRef1, defaultHttpRef2),
                user);

        BodyPart bodyPartToAdd = dbUtil.createBodyPart(3);
        HttpRef customHttpRefToAdd = dbUtil.createCustomHttpRef(5, user);
        HttpRef defaultHttpRefToAdd = dbUtil.createDefaultHttpRef(6);
        ExerciseUpdateRequestDto requestDto = dtoUtil.exerciseUpdateRequestDto(
                2,
                false,
                List.of(bodyPart1.getId(), bodyPartToAdd.getId()),
                List.of(
                        customHttpRef1.getId(),
                        defaultHttpRef1.getId(),
                        customHttpRefToAdd.getId(),
                        defaultHttpRefToAdd.getId()));
        requestDto.setTitle(updateTitle);
        requestDto.setDescription(updateDescription);
        requestDto.setNeedsEquipment(updateNeedsEquipment);

        String initialTitle = customExercise.getTitle();
        String initialDescription = customExercise.getDescription();
        boolean initialNeedsEquipment = customExercise.isNeedsEquipment();

        // Expected nested objects
        List<BodyPart> expectedBodyParts = List.of(bodyPart1, bodyPartToAdd);
        List<HttpRef> expectedHttpRefs =
                List.of(customHttpRef1, defaultHttpRef1, customHttpRefToAdd, defaultHttpRefToAdd);

        // When
        MvcResult mvcResult = mockMvc.perform(patch(URL.CUSTOM_EXERCISE_ID, customExercise.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(notNullValue())))
                .andDo(print())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        ExerciseResponseDto responseDto = objectMapper.readValue(responseContent, ExerciseResponseDto.class);

        assertTrue(responseDto.isCustom());

        if (nonNull(updateTitle)) assertEquals(requestDto.getTitle(), responseDto.getTitle());
        else assertEquals(initialTitle, responseDto.getTitle());

        if (nonNull(updateDescription)) assertEquals(requestDto.getDescription(), responseDto.getDescription());
        else assertEquals(initialDescription, responseDto.getDescription());

        if (nonNull(updateNeedsEquipment)) assertEquals(requestDto.getNeedsEquipment(), responseDto.isNeedsEquipment());
        else assertEquals(initialNeedsEquipment, responseDto.isNeedsEquipment());

        assertThat(responseDto.getBodyParts()).usingRecursiveComparison().isEqualTo(expectedBodyParts);
        assertThat(responseDto.getHttpRefs()).usingRecursiveComparison().isEqualTo(expectedHttpRefs);
    }

    static Stream<Arguments> updateCustomExerciseMultipleValidInputs() {
        return Stream.of(
                Arguments.of("Update title", "Update description", false),
                Arguments.of("Update title", "Update description", null),
                Arguments.of("Update title", null, false),
                Arguments.of(null, "Update description", false),
                Arguments.of(null, "Update description", null),
                Arguments.of(null, null, false));
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void updateCustomExerciseTest_shouldReturnExerciseDtoWith200_whenEmptyHttpRefsIdsListGiven() throws Exception {
        // Given
        User user = dbUtil.createUser(1);
        BodyPart bodyPart = dbUtil.createBodyPart(1);
        HttpRef customHttpRef = dbUtil.createCustomHttpRef(1, user);
        HttpRef defaultHttpRef = dbUtil.createDefaultHttpRef(2);
        boolean needsEquipment = true;
        Exercise customExercise = dbUtil.createCustomExercise(
                1, needsEquipment, List.of(bodyPart), List.of(customHttpRef, defaultHttpRef), user);

        // Http refs should be removed from the target exercise. Other fields should remain the same.
        ExerciseUpdateRequestDto requestDto = dtoUtil.exerciseUpdateRequestDtoEmpty();
        List<Long> newHttpRefs = Collections.emptyList();
        requestDto.setHttpRefIds(newHttpRefs);
        requestDto.setBodyPartIds(customExercise.getBodyPartsIdsSorted());

        // When
        MvcResult mvcResult = mockMvc.perform(patch(URL.CUSTOM_EXERCISE_ID, customExercise.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(notNullValue())))
                .andDo(print())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        ExerciseResponseDto responseDto = objectMapper.readValue(responseContent, ExerciseResponseDto.class);

        assertTrue(responseDto.isCustom());
        assertEquals(customExercise.getTitle(), responseDto.getTitle());
        assertEquals(customExercise.getDescription(), responseDto.getDescription());
        assertEquals(customExercise.isNeedsEquipment(), responseDto.isNeedsEquipment());
        assertThat(responseDto.getBodyParts())
                .usingRecursiveComparison()
                .isEqualTo(customExercise.getBodyPartsSortedById());
        assertThat(responseDto.getHttpRefs()).usingRecursiveComparison().isEqualTo(Collections.emptyList());
    }

    @ParameterizedTest
    @MethodSource("updateCustomExerciseMultipleInvalidInputs")
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void updateCustomExerciseTest_shouldReturnValidationMessageWith400_whenInvalidRequest(
            String updateTitle,
            String updateDescription,
            Boolean updateNeedsEquipment,
            List<Long> updateBodyPartsIds,
            List<Long> updateHttpRefsIds,
            String errorFieldName,
            String errorMessage)
            throws Exception {
        // Given
        User user = dbUtil.createUser(1);
        BodyPart bodyPart = dbUtil.createBodyPart(1);
        HttpRef customHttpRef = dbUtil.createCustomHttpRef(1, user);
        HttpRef defaultHttpRef = dbUtil.createDefaultHttpRef(2);
        boolean needsEquipment = true;
        Exercise customExercise = dbUtil.createCustomExercise(
                1, needsEquipment, List.of(bodyPart), List.of(customHttpRef, defaultHttpRef), user);

        ExerciseUpdateRequestDto requestDto = dtoUtil.exerciseUpdateRequestDtoEmpty();
        requestDto.setTitle(updateTitle);
        requestDto.setDescription(updateDescription);
        requestDto.setNeedsEquipment(updateNeedsEquipment);
        requestDto.setBodyPartIds(updateBodyPartsIds);
        requestDto.setHttpRefIds(updateHttpRefsIds);

        // When
        mockMvc.perform(patch(URL.CUSTOM_EXERCISE_ID, customExercise.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(errorFieldName, is(errorMessage)))
                .andDo(print());
    }

    static Stream<Arguments> updateCustomExerciseMultipleInvalidInputs() {
        return Stream.of(
                // Invalid title
                Arguments.of(
                        "Update title$%",
                        null, null, List.of(1L), Collections.emptyList(), "$.title", "Not allowed symbols"),
                // Invalid description
                Arguments.of(
                        null,
                        "Update description#@!",
                        null,
                        List.of(1L),
                        Collections.emptyList(),
                        "$.description",
                        "Not allowed symbols"),
                // Empty list of bodyPartIds
                Arguments.of(
                        null,
                        null,
                        null,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        "$.bodyPartIds",
                        "Should be not empty list"),
                // Null bodyPartIds
                Arguments.of(null, null, null, null, Collections.emptyList(), "$.bodyPartIds", "must not be null"),
                // Null httpRefIds
                Arguments.of(null, null, null, List.of(1L), null, "$.httpRefIds", "must not be null"));
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void updateCustomExerciseTest_shouldReturnErrorMessageWith400_whenEmptyRequest() throws Exception {
        // Given
        User user = dbUtil.createUser(1);
        BodyPart bodyPart = dbUtil.createBodyPart(1);
        HttpRef customHttpRef = dbUtil.createCustomHttpRef(1, user);
        HttpRef defaultHttpRef = dbUtil.createDefaultHttpRef(2);
        Exercise customExercise =
                dbUtil.createCustomExercise(1, true, List.of(bodyPart), List.of(customHttpRef, defaultHttpRef), user);

        ExerciseUpdateRequestDto requestDto = dtoUtil.exerciseUpdateRequestDtoEmpty();
        requestDto.setBodyPartIds(customExercise.getBodyPartsIdsSorted());
        requestDto.setHttpRefIds(customExercise.getHttpRefsIdsSorted());

        // When
        mockMvc.perform(patch(URL.CUSTOM_EXERCISE_ID, customExercise.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(ErrorMessage.NO_UPDATES_REQUEST.getName())))
                .andDo(print());
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void updateCustomExerciseTest_shouldReturnErrorMessageWith404_whenExerciseNotFound() throws Exception {
        // Given
        User user = dbUtil.createUser(1);
        ExerciseUpdateRequestDto requestDto = dtoUtil.exerciseUpdateRequestDtoEmpty();
        requestDto.setBodyPartIds(List.of(1L));
        requestDto.setHttpRefIds(List.of(1L));
        long nonExistentExerciseId = 1000L;

        // When
        mockMvc.perform(patch(URL.CUSTOM_EXERCISE_ID, nonExistentExerciseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(ErrorMessage.NOT_FOUND.getName())))
                .andDo(print());
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void updateCustomExerciseTest_shouldReturnErrorMessageWith404_whenExerciseDoesntBelongToUser() throws Exception {
        // Given
        Role role = dbUtil.createUserRole();
        Country country = dbUtil.createCountry(1);
        User user = dbUtil.createUser(1, role, country);
        BodyPart bodyPart = dbUtil.createBodyPart(1);
        HttpRef customHttpRef = dbUtil.createCustomHttpRef(1, user);
        HttpRef defaultHttpRef = dbUtil.createDefaultHttpRef(2);
        boolean needsEquipment = true;
        Exercise customExercise = dbUtil.createCustomExercise(
                1, needsEquipment, List.of(bodyPart), List.of(customHttpRef, defaultHttpRef), user);

        User user2 = dbUtil.createUser(2, role, country);
        Exercise customExercise2 =
                dbUtil.createCustomExercise(2, needsEquipment, List.of(bodyPart), List.of(defaultHttpRef), user2);

        ExerciseUpdateRequestDto requestDto = dtoUtil.exerciseUpdateRequestDtoEmpty();
        requestDto.setBodyPartIds(customExercise.getBodyPartsIdsSorted());
        requestDto.setHttpRefIds(Collections.emptyList());

        // When
        mockMvc.perform(patch(URL.CUSTOM_EXERCISE_ID, customExercise2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(ErrorMessage.NOT_FOUND.getName())))
                .andDo(print());
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void updateCustomExerciseTest_shouldReturnErrorMessageWith400_whenExerciseWithNewTitleAlreadyExists()
            throws Exception {
        // Given
        User user = dbUtil.createUser(1);
        BodyPart bodyPart = dbUtil.createBodyPart(1);
        HttpRef customHttpRef = dbUtil.createCustomHttpRef(1, user);
        HttpRef defaultHttpRef = dbUtil.createDefaultHttpRef(2);
        boolean needsEquipment = true;
        Exercise customExercise = dbUtil.createCustomExercise(
                1, needsEquipment, List.of(bodyPart), List.of(customHttpRef, defaultHttpRef), user);
        Exercise customExercise2 =
                dbUtil.createCustomExercise(2, needsEquipment, List.of(bodyPart), Collections.emptyList(), user);

        ExerciseUpdateRequestDto requestDto = dtoUtil.exerciseUpdateRequestDtoEmpty();
        requestDto.setBodyPartIds(customExercise.getBodyPartsIdsSorted());
        requestDto.setHttpRefIds(Collections.emptyList());
        requestDto.setTitle(customExercise2.getTitle());

        // When
        mockMvc.perform(patch(URL.CUSTOM_EXERCISE_ID, customExercise.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(ErrorMessage.TITLE_DUPLICATE.getName())))
                .andDo(print());
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void updateCustomExerciseTest_shouldReturnErrorMessageWith400_whenBodyPartNotFound() throws Exception {
        // Given
        User user = dbUtil.createUser(1);
        BodyPart bodyPart = dbUtil.createBodyPart(1);
        HttpRef customHttpRef = dbUtil.createCustomHttpRef(1, user);
        HttpRef defaultHttpRef = dbUtil.createDefaultHttpRef(2);
        boolean needsEquipment = true;
        Exercise customExercise = dbUtil.createCustomExercise(
                1, needsEquipment, List.of(bodyPart), List.of(customHttpRef, defaultHttpRef), user);

        ExerciseUpdateRequestDto requestDto = dtoUtil.exerciseUpdateRequestDtoEmpty();
        long nonExistentBodyPartId = 1000L;
        requestDto.setBodyPartIds(List.of(nonExistentBodyPartId));
        requestDto.setHttpRefIds(Collections.emptyList());

        // When
        mockMvc.perform(patch(URL.CUSTOM_EXERCISE_ID, customExercise.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(ErrorMessage.INVALID_NESTED_OBJECT.getName())))
                .andDo(print());
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void updateCustomExerciseTest_shouldReturnErrorMessageWith400_whenNewHttpRefDoesntBelongToUser() throws Exception {
        // Given
        Role role = dbUtil.createUserRole();
        Country country = dbUtil.createCountry(1);
        User user = dbUtil.createUser(1, role, country);
        BodyPart bodyPart = dbUtil.createBodyPart(1);
        HttpRef customHttpRef = dbUtil.createCustomHttpRef(1, user);
        HttpRef defaultHttpRef = dbUtil.createDefaultHttpRef(2);
        boolean needsEquipment = true;
        Exercise customExercise = dbUtil.createCustomExercise(
                1, needsEquipment, List.of(bodyPart), List.of(customHttpRef, defaultHttpRef), user);

        User user2 = dbUtil.createUser(2, role, country);
        HttpRef customHttpRef2 = dbUtil.createCustomHttpRef(2, user2);

        ExerciseUpdateRequestDto requestDto = dtoUtil.exerciseUpdateRequestDtoEmpty();
        requestDto.setBodyPartIds(customExercise.getBodyPartsIdsSorted());
        requestDto.setHttpRefIds(List.of(customHttpRef2.getId()));

        // When
        mockMvc.perform(patch(URL.CUSTOM_EXERCISE_ID, customExercise.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(ErrorMessage.USER_RESOURCE_MISMATCH.getName())))
                .andDo(print());
    }

    @ParameterizedTest
    @MethodSource("updateCustomExerciseMultipleValidButNotDifferentInputs")
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void updateCustomExerciseTest_shouldReturnErrorMessageWith400_whenNewFieldValueIsNotDifferent(
            String updateTitle, String updateDescription, Boolean updateNeedsEquipment) throws Exception {
        // Given
        User user = dbUtil.createUser(1);
        BodyPart bodyPart = dbUtil.createBodyPart(1);
        HttpRef customHttpRef = dbUtil.createCustomHttpRef(1, user);
        HttpRef defaultHttpRef = dbUtil.createDefaultHttpRef(2);
        boolean needsEquipment = true;
        Exercise customExercise = dbUtil.createCustomExercise(
                1, needsEquipment, List.of(bodyPart), List.of(customHttpRef, defaultHttpRef), user);

        ExerciseUpdateRequestDto requestDto = dtoUtil.exerciseUpdateRequestDtoEmpty();
        requestDto.setBodyPartIds(customExercise.getBodyPartsIdsSorted());
        requestDto.setHttpRefIds(Collections.emptyList());
        requestDto.setTitle(updateTitle);
        requestDto.setDescription(updateDescription);
        requestDto.setNeedsEquipment(updateNeedsEquipment);

        String errorMessage = nonNull(updateTitle)
                ? ErrorMessage.TITLES_ARE_NOT_DIFFERENT.getName()
                : nonNull(updateDescription)
                        ? ErrorMessage.DESCRIPTIONS_ARE_NOT_DIFFERENT.getName()
                        : ErrorMessage.NEEDS_EQUIPMENT_ARE_NOT_DIFFERENT.getName();

        // When
        mockMvc.perform(patch(URL.CUSTOM_EXERCISE_ID, customExercise.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(errorMessage)))
                .andDo(print());
    }

    static Stream<Arguments> updateCustomExerciseMultipleValidButNotDifferentInputs() {
        return Stream.of(
                Arguments.of("Exercise 1", null, null),
                Arguments.of(null, "Desc 1", null),
                Arguments.of(null, null, true));
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void deleteCustomExerciseTest_shouldReturnVoidWith204_whenValidRequest() throws Exception {
        // Given
        User user = dbUtil.createUser(1);
        BodyPart bodyPart = dbUtil.createBodyPart(1);
        HttpRef customHttpRef = dbUtil.createCustomHttpRef(1, user);
        HttpRef defaultHttpRef = dbUtil.createDefaultHttpRef(2);
        boolean needsEquipment = true;
        Exercise customExercise = dbUtil.createCustomExercise(
                1, needsEquipment, List.of(bodyPart), List.of(customHttpRef, defaultHttpRef), user);
        long exerciseIdToBeRemoved = customExercise.getId();

        // When
        mockMvc.perform(delete(URL.CUSTOM_EXERCISE_ID, customExercise.getId()).contentType(MediaType.APPLICATION_JSON))

                // Then
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist())
                .andDo(print());

        assertNull(dbUtil.getExerciseById(exerciseIdToBeRemoved));
        assertTrue(dbUtil.httpRefsExistByIds(List.of(customHttpRef.getId(), defaultHttpRef.getId())));
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void deleteCustomExerciseTest_shouldReturnErrorMessageWith400_whenWrongExerciseIdGiven() throws Exception {
        // Given
        User user = dbUtil.createUser(1);
        BodyPart bodyPart = dbUtil.createBodyPart(1);
        HttpRef customHttpRef = dbUtil.createCustomHttpRef(1, user);
        HttpRef defaultHttpRef = dbUtil.createDefaultHttpRef(2);
        boolean needsEquipment = true;
        Exercise customExercise = dbUtil.createCustomExercise(
                1, needsEquipment, List.of(bodyPart), List.of(customHttpRef, defaultHttpRef), user);
        long wrongExerciseId = customExercise.getId() + 1;

        // When
        mockMvc.perform(delete(URL.CUSTOM_EXERCISE_ID, wrongExerciseId).contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(ErrorMessage.NOT_FOUND.getName())))
                .andDo(print());
    }
}
