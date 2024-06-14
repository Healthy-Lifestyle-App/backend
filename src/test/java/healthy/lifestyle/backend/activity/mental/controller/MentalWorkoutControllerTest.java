package healthy.lifestyle.backend.activity.mental.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import healthy.lifestyle.backend.activity.mental.dto.MentalWorkoutCreateRequestDto;
import healthy.lifestyle.backend.activity.mental.dto.MentalWorkoutResponseDto;
import healthy.lifestyle.backend.activity.mental.model.MentalActivity;
import healthy.lifestyle.backend.activity.mental.model.MentalType;
import healthy.lifestyle.backend.activity.mental.model.MentalWorkout;
import healthy.lifestyle.backend.activity.workout.model.HttpRef;
import healthy.lifestyle.backend.shared.exception.ApiException;
import healthy.lifestyle.backend.shared.exception.ErrorMessage;
import healthy.lifestyle.backend.testconfig.BeanConfig;
import healthy.lifestyle.backend.testconfig.ContainerConfig;
import healthy.lifestyle.backend.testutil.DbUtil;
import healthy.lifestyle.backend.testutil.DtoUtil;
import healthy.lifestyle.backend.testutil.URL;
import healthy.lifestyle.backend.user.model.Country;
import healthy.lifestyle.backend.user.model.Role;
import healthy.lifestyle.backend.user.model.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
public class MentalWorkoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    DbUtil dbUtil;

    @Autowired
    DtoUtil dtoUtil;

    @MockBean
    FirebaseMessaging firebaseMessaging;

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
    void createCustomMentalWorkout_shouldReturnCreatedDtoWith201_whenValidFields() throws Exception {
        // Given
        User user = dbUtil.createUser(1);
        MentalType mentalType1 = dbUtil.createAffirmationType();
        HttpRef defaultHttpRef = dbUtil.createDefaultHttpRef(1);
        HttpRef customHttpRef = dbUtil.createCustomHttpRef(2, user);
        MentalActivity defaultMental1 = dbUtil.createDefaultMentalActivity(1, List.of(defaultHttpRef), mentalType1);
        MentalActivity customMental1 =
                dbUtil.createCustomMentalActivity(2, List.of(defaultHttpRef, customHttpRef), mentalType1, user);

        MentalWorkoutCreateRequestDto mentalWorkoutCreateRequestDto =
                dtoUtil.mentalWorkoutCreateRequestDto(1, List.of(defaultMental1.getId(), customMental1.getId()));

        // When
        MvcResult mvcResult = mockMvc.perform(post(URL.CUSTOM_MENTAL_WORKOUTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mentalWorkoutCreateRequestDto)))

                // Then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(notNullValue())))
                .andExpect(jsonPath("$.title", is(mentalWorkoutCreateRequestDto.getTitle())))
                .andExpect(jsonPath("$.description", is(mentalWorkoutCreateRequestDto.getDescription())))
                .andExpect(jsonPath("$.isCustom", is(true)))
                .andDo(print())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        MentalWorkoutResponseDto responseDto =
                objectMapper.readValue(responseContent, new TypeReference<MentalWorkoutResponseDto>() {});

        // Db
        MentalWorkout createdMentalWorkout = dbUtil.getMentalWorkoutById(responseDto.getId());
        assertEquals(user.getId(), createdMentalWorkout.getUser().getId());
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void createCustomMentalWorkout_shouldReturnErrorMessageWith400_whenAlreadyExistsWithSameTitle() throws Exception {
        // Given
        User user = dbUtil.createUser(1);
        MentalType mentalType1 = dbUtil.createAffirmationType();
        HttpRef defaultHttpRef = dbUtil.createDefaultHttpRef(1);
        HttpRef customHttpRef = dbUtil.createCustomHttpRef(2, user);
        MentalActivity defaultMental1 = dbUtil.createDefaultMentalActivity(1, List.of(defaultHttpRef), mentalType1);
        MentalActivity customMental1 =
                dbUtil.createCustomMentalActivity(2, List.of(defaultHttpRef, customHttpRef), mentalType1, user);

        MentalWorkout customMentalWorkout =
                dbUtil.createCustomMentalWorkout(1, List.of(defaultMental1, customMental1), user);

        MentalWorkoutCreateRequestDto requestDto =
                dtoUtil.mentalWorkoutCreateRequestDto(1, List.of(defaultMental1.getId(), customMental1.getId()));
        requestDto.setTitle(customMentalWorkout.getTitle());

        ApiException expectedException = new ApiException(ErrorMessage.TITLE_DUPLICATE, null, HttpStatus.BAD_REQUEST);

        // When
        MvcResult mvcResult = mockMvc.perform(post(URL.CUSTOM_MENTAL_WORKOUTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(expectedException.getMessage())))
                .andDo(print())
                .andReturn();
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void createCustomMentalWorkout_shouldReturnErrorMessageWith404_whenMentalActivityNotFound() throws Exception {
        // Given
        User user = dbUtil.createUser(1);
        long nonExistentMentalActivityId = 1000L;
        MentalWorkoutCreateRequestDto requestDto =
                dtoUtil.mentalWorkoutCreateRequestDto(1, List.of(nonExistentMentalActivityId));
        ApiException expectedException =
                new ApiException(ErrorMessage.MENTAL_NOT_FOUND, nonExistentMentalActivityId, HttpStatus.NOT_FOUND);

        // When
        MvcResult mvcResult = mockMvc.perform(post(URL.CUSTOM_MENTAL_WORKOUTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))

                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(expectedException.getMessageWithResourceId())))
                .andDo(print())
                .andReturn();
    }

    @Test
    @WithMockUser(username = "Username-1", password = "Password-1", roles = "USER")
    void createCustomMentalWorkout_shouldReturnErrorMessageWith400_whenMentalActivityUserMismatch() throws Exception {
        // Given
        Role role = dbUtil.createUserRole();
        Country country = dbUtil.createCountry(1);

        User user1 = dbUtil.createUser(1, role, country);
        User user2 = dbUtil.createUser(2, role, country);
        MentalType mentalType1 = dbUtil.createAffirmationType();
        HttpRef defaultHttpRef = dbUtil.createDefaultHttpRef(1);
        HttpRef customHttpRef = dbUtil.createCustomHttpRef(2, user2);

        MentalActivity defaultMental1 = dbUtil.createDefaultMentalActivity(1, List.of(defaultHttpRef), mentalType1);
        MentalActivity customMental1 =
                dbUtil.createCustomMentalActivity(2, List.of(defaultHttpRef, customHttpRef), mentalType1, user2);

        MentalWorkoutCreateRequestDto mentalWorkoutCreateRequestDto =
                dtoUtil.mentalWorkoutCreateRequestDto(1, List.of(defaultMental1.getId(), customMental1.getId()));

        ApiException expectedException =
                new ApiException(ErrorMessage.USER_MENTAL_MISMATCH, customMental1.getId(), HttpStatus.BAD_REQUEST);

        // When
        MvcResult mvcResult = mockMvc.perform(post(URL.CUSTOM_MENTAL_WORKOUTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mentalWorkoutCreateRequestDto)))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(expectedException.getMessageWithResourceId())))
                .andDo(print())
                .andReturn();
    }
}
