package healthy.lifestyle.backend.workout.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import healthy.lifestyle.backend.data.DataConfiguration;
import healthy.lifestyle.backend.data.DataHelper;
import healthy.lifestyle.backend.users.model.Role;
import healthy.lifestyle.backend.users.model.User;
import healthy.lifestyle.backend.workout.dto.*;
import healthy.lifestyle.backend.workout.model.BodyPart;
import healthy.lifestyle.backend.workout.model.Exercise;
import healthy.lifestyle.backend.workout.model.HttpRef;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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

/**
 * @see ExerciseController
 * @see healthy.lifestyle.backend.workout.service.ExerciseServiceImpl
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(DataConfiguration.class)
class ExerciseControllerTest {
    @Container
    static PostgreSQLContainer<?> postgresqlContainer =
            (PostgreSQLContainer<?>) new PostgreSQLContainer(DockerImageName.parse("postgres:12.15"))
                    .withDatabaseName("test_db")
                    .withUsername("test_user")
                    .withPassword("test_password")
                    .withReuse(true);

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> String.format(
                        "jdbc:postgresql://localhost:%s/%s",
                        postgresqlContainer.getFirstMappedPort(), postgresqlContainer.getDatabaseName()));
        registry.add("spring.datasource.username", postgresqlContainer::getUsername);
        registry.add("spring.datasource.password", postgresqlContainer::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    DataHelper dataHelper;

    @Autowired
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @BeforeEach
    void beforeEach() {
        dataHelper.deleteAll();
    }

    private static final String URL = "/api/v1/exercises";

    @Test
    void postgresqlContainerTest() {
        assertThat(postgresqlContainer.isRunning()).isTrue();
    }

    @Test
    @WithMockUser(username = "test-username", password = "test-password", roles = "USER")
    void createCustomExercisePositive() throws Exception {
        Role role = dataHelper.createRole("ROLE_USER");
        User user = dataHelper.createUser(
                "Test Full Name",
                "test-username",
                "test@email.com",
                passwordEncoder().encode("test-password"),
                role,
                null);
        BodyPart bodyPart1 = dataHelper.createBodyPart("Body Part 1");
        BodyPart bodyPart2 = dataHelper.createBodyPart("Body Part 2");
        HttpRef httpRef1 = dataHelper.createHttpRef("Ref 1", "http://ref1.com", "Desc 1");
        HttpRef httpRef2 = dataHelper.createHttpRef("Ref 2", "http://ref2.com");

        BodyPartRequestDto bodyPartRequestDto1 =
                new BodyPartRequestDto.Builder().id(bodyPart1.getId()).build();
        BodyPartRequestDto bodyPartRequestDto2 =
                new BodyPartRequestDto.Builder().id(bodyPart2.getId()).build();
        HttpRefRequestDto httpRefRequestDto1 =
                new HttpRefRequestDto.Builder().id(httpRef1.getId()).build();
        HttpRefRequestDto httpRefRequestDto2 =
                new HttpRefRequestDto.Builder().id(httpRef2.getId()).build();
        CreateExerciseRequestDto requestDto = new CreateExerciseRequestDto.Builder()
                .title("Test title")
                .description("Test desc")
                .bodyParts(Set.of(bodyPartRequestDto1, bodyPartRequestDto2))
                .httpRefs(Set.of(httpRefRequestDto1, httpRefRequestDto2))
                .build();

        MvcResult mvcResult = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(notNullValue())))
                .andExpect(jsonPath("$.title", is(notNullValue())))
                .andExpect(jsonPath("$.description", is(notNullValue())))
                .andExpect(jsonPath("$.bodyParts", is(notNullValue())))
                .andExpect(jsonPath("$.httpRefs", is(notNullValue())))
                .andDo(print())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        CreateExerciseResponseDto responseDto =
                objectMapper.readValue(responseContent, CreateExerciseResponseDto.class);

        assertNotNull(responseDto.getId());
        assertEquals(requestDto.getTitle(), responseDto.getTitle());
        assertEquals(requestDto.getDescription(), responseDto.getDescription());
        assertEquals(2, responseDto.getBodyParts().size());
        assertEquals(2, responseDto.getHttpRefs().size());

        List<BodyPartRequestDto> expectedBodyParts = requestDto.getBodyParts().stream()
                .sorted(Comparator.comparingLong(BodyPartRequestDto::getId))
                .toList();
        List<BodyPartResponseDto> actualBodyParts = responseDto.getBodyParts().stream()
                .sorted(Comparator.comparingLong(BodyPartResponseDto::getId))
                .toList();
        assertEquals(expectedBodyParts.get(0).getId(), actualBodyParts.get(0).getId());
        assertEquals(bodyPart1.getName(), actualBodyParts.get(0).getName());
        assertEquals(expectedBodyParts.get(1).getId(), actualBodyParts.get(1).getId());
        assertEquals(bodyPart2.getName(), actualBodyParts.get(1).getName());

        List<HttpRefRequestDto> expectedHttpRefs = requestDto.getHttpRefs().stream()
                .sorted(Comparator.comparingLong(HttpRefRequestDto::getId))
                .toList();
        List<HttpRefResponseDto> actualHttpRefs = responseDto.getHttpRefs().stream()
                .sorted(Comparator.comparingLong(HttpRefResponseDto::getId))
                .toList();
        assertEquals(expectedHttpRefs.get(0).getId(), actualHttpRefs.get(0).getId());
        assertEquals(httpRef1.getName(), actualHttpRefs.get(0).getName());
        assertEquals(httpRef1.getRef(), actualHttpRefs.get(0).getRef());
        assertEquals(httpRef1.getDescription(), actualHttpRefs.get(0).getDescription());

        assertEquals(expectedHttpRefs.get(1).getId(), actualHttpRefs.get(1).getId());
        assertEquals(httpRef2.getName(), actualHttpRefs.get(1).getName());
        assertEquals(httpRef2.getRef(), actualHttpRefs.get(1).getRef());
        assertEquals(httpRef2.getDescription(), actualHttpRefs.get(1).getDescription());
    }

    @Test
    @WithMockUser(username = "test-username", password = "test-password", roles = "USER")
    void createCustomExerciseNegativeNoHttpRefs() throws Exception {
        Role role = dataHelper.createRole("ROLE_USER");
        User user = dataHelper.createUser(
                "Test Full Name",
                "test-username",
                "test@email.com",
                passwordEncoder().encode("test-password"),
                role,
                null);
        BodyPart bodyPart1 = dataHelper.createBodyPart("Body Part 1");
        BodyPart bodyPart2 = dataHelper.createBodyPart("Body Part 2");

        BodyPartRequestDto bodyPartRequestDto1 =
                new BodyPartRequestDto.Builder().id(bodyPart1.getId()).build();
        BodyPartRequestDto bodyPartRequestDto2 =
                new BodyPartRequestDto.Builder().id(bodyPart2.getId()).build();

        CreateExerciseRequestDto requestDto = new CreateExerciseRequestDto.Builder()
                .title("Test title")
                .description("Test desc")
                .bodyParts(Set.of(bodyPartRequestDto1, bodyPartRequestDto2))
                .build();

        MvcResult mvcResult = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(notNullValue())))
                .andExpect(jsonPath("$.title", is(notNullValue())))
                .andExpect(jsonPath("$.description", is(notNullValue())))
                .andExpect(jsonPath("$.bodyParts", is(notNullValue())))
                .andExpect(jsonPath("$.httpRefs", is(nullValue())))
                .andDo(print())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        CreateExerciseResponseDto responseDto =
                objectMapper.readValue(responseContent, CreateExerciseResponseDto.class);
        assertEquals(2, responseDto.getBodyParts().size());

        List<BodyPartRequestDto> expectedBodyParts = requestDto.getBodyParts().stream()
                .sorted(Comparator.comparingLong(BodyPartRequestDto::getId))
                .toList();
        List<BodyPartResponseDto> actualBodyParts = responseDto.getBodyParts().stream()
                .sorted(Comparator.comparingLong(BodyPartResponseDto::getId))
                .toList();
        assertEquals(expectedBodyParts.get(0).getId(), actualBodyParts.get(0).getId());
        assertEquals(bodyPart1.getName(), actualBodyParts.get(0).getName());
        assertEquals(expectedBodyParts.get(1).getId(), actualBodyParts.get(1).getId());
        assertEquals(bodyPart2.getName(), actualBodyParts.get(1).getName());
    }

    @Test
    @WithMockUser(username = "test-username", password = "test-password", roles = "USER")
    void createCustomExerciseNegativeNoBodyParts() throws Exception {
        Role role = dataHelper.createRole("ROLE_USER");
        User user = dataHelper.createUser(
                "Test Full Name",
                "test-username",
                "test@email.com",
                passwordEncoder().encode("test-password"),
                role,
                null);
        HttpRef httpRef1 = dataHelper.createHttpRef("Ref 1", "http://ref1.com", "Desc 1");
        HttpRef httpRef2 = dataHelper.createHttpRef("Ref 2", "http://ref2.com");

        HttpRefRequestDto httpRefRequestDto1 =
                new HttpRefRequestDto.Builder().id(httpRef1.getId()).build();
        HttpRefRequestDto httpRefRequestDto2 =
                new HttpRefRequestDto.Builder().id(httpRef2.getId()).build();
        CreateExerciseRequestDto requestDto = new CreateExerciseRequestDto.Builder()
                .title("Test title")
                .description("Test desc")
                .httpRefs(Set.of(httpRefRequestDto1, httpRefRequestDto2))
                .build();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid nested object")))
                .andDo(print());
    }

    @Test
    @WithMockUser(username = "test-username", password = "test-password", roles = "USER")
    void createCustomExerciseNegativeWrongBodyPartId() throws Exception {
        Role role = dataHelper.createRole("ROLE_USER");
        User user = dataHelper.createUser(
                "Test Full Name",
                "test-username",
                "test@email.com",
                passwordEncoder().encode("test-password"),
                role,
                null);
        BodyPart bodyPart1 = dataHelper.createBodyPart("Body Part 1");
        long wrongBodyPartId = 1000L;
        BodyPartRequestDto bodyPartRequestDto1 =
                new BodyPartRequestDto.Builder().id(bodyPart1.getId()).build();
        BodyPartRequestDto bodyPartRequestDto2 =
                new BodyPartRequestDto.Builder().id(wrongBodyPartId).build();

        HttpRef httpRef1 = dataHelper.createHttpRef("Ref 1", "http://ref1.com", "Desc 1");
        HttpRef httpRef2 = dataHelper.createHttpRef("Ref 2", "http://ref2.com");
        HttpRefRequestDto httpRefRequestDto1 =
                new HttpRefRequestDto.Builder().id(httpRef1.getId()).build();
        HttpRefRequestDto httpRefRequestDto2 =
                new HttpRefRequestDto.Builder().id(httpRef2.getId()).build();

        CreateExerciseRequestDto requestDto = new CreateExerciseRequestDto.Builder()
                .title("Test title")
                .description("Test desc")
                .bodyParts(Set.of(bodyPartRequestDto1, bodyPartRequestDto2))
                .httpRefs(Set.of(httpRefRequestDto1, httpRefRequestDto2))
                .build();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid nested object")))
                .andDo(print());
    }

    @Test
    @WithMockUser(username = "test-username", password = "test-password", roles = "USER")
    void createCustomExerciseNegativeHttpRefId() throws Exception {
        Role role = dataHelper.createRole("ROLE_USER");
        User user = dataHelper.createUser(
                "Test Full Name",
                "test-username",
                "test@email.com",
                passwordEncoder().encode("test-password"),
                role,
                null);
        BodyPart bodyPart1 = dataHelper.createBodyPart("Body Part 1");
        BodyPart bodyPart2 = dataHelper.createBodyPart("Body Part 2");
        BodyPartRequestDto bodyPartRequestDto1 =
                new BodyPartRequestDto.Builder().id(bodyPart1.getId()).build();
        BodyPartRequestDto bodyPartRequestDto2 =
                new BodyPartRequestDto.Builder().id(bodyPart2.getId()).build();

        HttpRef httpRef1 = dataHelper.createHttpRef("Ref 1", "http://ref1.com", "Desc 1");
        long wrongHttpRefId = 1000L;
        HttpRefRequestDto httpRefRequestDto1 =
                new HttpRefRequestDto.Builder().id(httpRef1.getId()).build();
        HttpRefRequestDto httpRefRequestDto2 =
                new HttpRefRequestDto.Builder().id(wrongHttpRefId).build();

        CreateExerciseRequestDto requestDto = new CreateExerciseRequestDto.Builder()
                .title("Test title")
                .description("Test desc")
                .bodyParts(Set.of(bodyPartRequestDto1, bodyPartRequestDto2))
                .httpRefs(Set.of(httpRefRequestDto1, httpRefRequestDto2))
                .build();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid nested object")))
                .andDo(print());
    }

    @Test
    @WithMockUser(username = "test-username", password = "test-password", roles = "USER")
    void getExercises_DefaultAndCustom() throws Exception {
        BodyPart bodyPart1 = dataHelper.createBodyPart("Body Part 1");
        BodyPart bodyPart2 = dataHelper.createBodyPart("Body Part 2");
        BodyPart bodyPart3 = dataHelper.createBodyPart("Body Part 3");
        HttpRef httpRef1 = dataHelper.createHttpRef("Ref 1", "http://ref1.com", "Desc 1");
        HttpRef httpRef2 = dataHelper.createHttpRef("Ref 2", "http://ref2.com");
        HttpRef httpRef3 = dataHelper.createHttpRef("Ref 3", "http://ref1.com", "Desc 3");
        // Default exercises
        Exercise exercise1 =
                dataHelper.createExercise("Exercise 1", "Description 1", false, Set.of(bodyPart1), Set.of(httpRef1));
        Exercise exercise2 =
                dataHelper.createExercise("Exercise 2", "Description 2", false, Set.of(bodyPart2), Set.of(httpRef2));
        // Custom exercises
        Exercise exercise3 = dataHelper.createExercise(
                "Exercise 3", "Description 3", true, Set.of(bodyPart1, bodyPart2), Set.of(httpRef1, httpRef2));
        Exercise exercise4 =
                dataHelper.createExercise("Exercise 4", "Description 4", true, Set.of(bodyPart3), Set.of(httpRef3));
        Exercise exercise5 =
                dataHelper.createExercise("Exercise 5", "Description 5", true, Set.of(bodyPart3), Set.of(httpRef3));

        Role role = dataHelper.createRole("ROLE_USER");
        User user1 = dataHelper.createUser(
                "Test Full Name",
                "test-username",
                "test@email.com",
                passwordEncoder().encode("test-password"),
                role,
                Set.of(exercise3, exercise4));
        User user2 = dataHelper.createUser(
                "Test Full Name",
                "test-username-two",
                "test2@email.com",
                passwordEncoder().encode("test-password"),
                role,
                Set.of(exercise5));

        MvcResult mvcResult = mockMvc.perform(get(URL).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises", is(notNullValue())))
                .andDo(print())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        GetExercisesResponseDto responseDto = objectMapper.readValue(responseContent, GetExercisesResponseDto.class);
        assertEquals(4, responseDto.getExercises().size());

        // Exercise 1 (default)
        ExerciseResponseDto actualExercise = responseDto.getExercises().get(0);
        Exercise expectedExercise = exercise1;
        assertEquals(expectedExercise.getId(), actualExercise.getId());
        assertEquals(expectedExercise.getTitle(), actualExercise.getTitle());
        assertEquals(expectedExercise.getDescription(), actualExercise.getDescription());
        assertEquals(
                expectedExercise.getBodyParts().size(),
                actualExercise.getBodyParts().size());
        assertEquals(
                expectedExercise.getHttpRefs().size(),
                actualExercise.getHttpRefs().size());

        List<BodyPart> expectedBodyParts = expectedExercise.getBodyParts().stream()
                .sorted(Comparator.comparingLong(BodyPart::getId))
                .toList();
        List<BodyPartResponseDto> actualBodyParts = actualExercise.getBodyParts().stream()
                .sorted(Comparator.comparingLong(BodyPartResponseDto::getId))
                .toList();
        assertEquals(expectedBodyParts.size(), actualBodyParts.size());
        assertEquals(expectedBodyParts.get(0).getId(), actualBodyParts.get(0).getId());
        assertEquals(expectedBodyParts.get(0).getName(), actualBodyParts.get(0).getName());

        List<HttpRef> expectedHttpRefs = expectedExercise.getHttpRefs().stream()
                .sorted(Comparator.comparingLong(HttpRef::getId))
                .toList();
        List<HttpRefResponseDto> actualHttpRefs = actualExercise.getHttpRefs().stream()
                .sorted(Comparator.comparingLong(HttpRefResponseDto::getId))
                .toList();
        assertEquals(expectedHttpRefs.size(), actualHttpRefs.size());
        assertEquals(expectedHttpRefs.get(0).getId(), actualHttpRefs.get(0).getId());
        assertEquals(expectedHttpRefs.get(0).getName(), actualHttpRefs.get(0).getName());
        assertEquals(
                expectedHttpRefs.get(0).getDescription(), actualHttpRefs.get(0).getDescription());

        // Exercise 2 (default)
        actualExercise = responseDto.getExercises().get(1);
        expectedExercise = exercise2;
        assertEquals(expectedExercise.getId(), actualExercise.getId());
        assertEquals(expectedExercise.getTitle(), actualExercise.getTitle());
        assertEquals(expectedExercise.getDescription(), actualExercise.getDescription());
        assertEquals(
                expectedExercise.getBodyParts().size(),
                actualExercise.getBodyParts().size());
        assertEquals(
                expectedExercise.getHttpRefs().size(),
                actualExercise.getHttpRefs().size());

        expectedBodyParts = expectedExercise.getBodyParts().stream()
                .sorted(Comparator.comparingLong(BodyPart::getId))
                .toList();
        actualBodyParts = actualExercise.getBodyParts().stream()
                .sorted(Comparator.comparingLong(BodyPartResponseDto::getId))
                .toList();
        assertEquals(expectedBodyParts.size(), actualBodyParts.size());
        assertEquals(expectedBodyParts.get(0).getId(), actualBodyParts.get(0).getId());
        assertEquals(expectedBodyParts.get(0).getName(), actualBodyParts.get(0).getName());

        expectedHttpRefs = expectedExercise.getHttpRefs().stream()
                .sorted(Comparator.comparingLong(HttpRef::getId))
                .toList();
        actualHttpRefs = actualExercise.getHttpRefs().stream()
                .sorted(Comparator.comparingLong(HttpRefResponseDto::getId))
                .toList();
        assertEquals(expectedHttpRefs.size(), actualHttpRefs.size());
        assertEquals(expectedHttpRefs.get(0).getId(), actualHttpRefs.get(0).getId());
        assertEquals(expectedHttpRefs.get(0).getName(), actualHttpRefs.get(0).getName());
        assertEquals(
                expectedHttpRefs.get(0).getDescription(), actualHttpRefs.get(0).getDescription());

        // Exercise 3 (custom)
        actualExercise = responseDto.getExercises().get(2);
        expectedExercise = exercise3;
        assertEquals(expectedExercise.getId(), actualExercise.getId());
        assertEquals(expectedExercise.getTitle(), actualExercise.getTitle());
        assertEquals(expectedExercise.getDescription(), actualExercise.getDescription());
        assertEquals(
                expectedExercise.getBodyParts().size(),
                actualExercise.getBodyParts().size());
        assertEquals(
                expectedExercise.getHttpRefs().size(),
                actualExercise.getHttpRefs().size());

        expectedBodyParts = expectedExercise.getBodyParts().stream()
                .sorted(Comparator.comparingLong(BodyPart::getId))
                .toList();
        actualBodyParts = actualExercise.getBodyParts().stream()
                .sorted(Comparator.comparingLong(BodyPartResponseDto::getId))
                .toList();
        assertEquals(expectedBodyParts.size(), actualBodyParts.size());
        assertEquals(expectedBodyParts.get(0).getId(), actualBodyParts.get(0).getId());
        assertEquals(expectedBodyParts.get(0).getName(), actualBodyParts.get(0).getName());
        assertEquals(expectedBodyParts.get(1).getId(), actualBodyParts.get(1).getId());
        assertEquals(expectedBodyParts.get(1).getName(), actualBodyParts.get(1).getName());

        expectedHttpRefs = expectedExercise.getHttpRefs().stream()
                .sorted(Comparator.comparingLong(HttpRef::getId))
                .toList();
        actualHttpRefs = actualExercise.getHttpRefs().stream()
                .sorted(Comparator.comparingLong(HttpRefResponseDto::getId))
                .toList();
        assertEquals(expectedHttpRefs.size(), actualHttpRefs.size());
        assertEquals(expectedHttpRefs.get(0).getId(), actualHttpRefs.get(0).getId());
        assertEquals(expectedHttpRefs.get(0).getName(), actualHttpRefs.get(0).getName());
        assertEquals(
                expectedHttpRefs.get(0).getDescription(), actualHttpRefs.get(0).getDescription());
        assertEquals(expectedHttpRefs.get(1).getId(), actualHttpRefs.get(1).getId());
        assertEquals(expectedHttpRefs.get(1).getName(), actualHttpRefs.get(1).getName());
        assertEquals(
                expectedHttpRefs.get(1).getDescription(), actualHttpRefs.get(1).getDescription());

        // Exercise 4 (custom)
        actualExercise = responseDto.getExercises().get(3);
        expectedExercise = exercise4;
        assertEquals(expectedExercise.getId(), actualExercise.getId());
        assertEquals(expectedExercise.getTitle(), actualExercise.getTitle());
        assertEquals(expectedExercise.getDescription(), actualExercise.getDescription());
        assertEquals(
                expectedExercise.getBodyParts().size(),
                actualExercise.getBodyParts().size());
        assertEquals(
                expectedExercise.getHttpRefs().size(),
                actualExercise.getHttpRefs().size());

        expectedBodyParts = expectedExercise.getBodyParts().stream()
                .sorted(Comparator.comparingLong(BodyPart::getId))
                .toList();
        actualBodyParts = actualExercise.getBodyParts().stream()
                .sorted(Comparator.comparingLong(BodyPartResponseDto::getId))
                .toList();
        assertEquals(expectedBodyParts.size(), actualBodyParts.size());
        assertEquals(expectedBodyParts.get(0).getId(), actualBodyParts.get(0).getId());
        assertEquals(expectedBodyParts.get(0).getName(), actualBodyParts.get(0).getName());

        expectedHttpRefs = expectedExercise.getHttpRefs().stream()
                .sorted(Comparator.comparingLong(HttpRef::getId))
                .toList();
        actualHttpRefs = actualExercise.getHttpRefs().stream()
                .sorted(Comparator.comparingLong(HttpRefResponseDto::getId))
                .toList();
        assertEquals(expectedHttpRefs.size(), actualHttpRefs.size());
        assertEquals(expectedHttpRefs.get(0).getId(), actualHttpRefs.get(0).getId());
        assertEquals(expectedHttpRefs.get(0).getName(), actualHttpRefs.get(0).getName());
        assertEquals(
                expectedHttpRefs.get(0).getDescription(), actualHttpRefs.get(0).getDescription());
    }

    @Test
    @WithMockUser(username = "test-username", password = "test-password", roles = "USER")
    void getExercises_CustomOnly() throws Exception {
        BodyPart bodyPart1 = dataHelper.createBodyPart("Body Part 1");
        BodyPart bodyPart2 = dataHelper.createBodyPart("Body Part 2");
        BodyPart bodyPart3 = dataHelper.createBodyPart("Body Part 3");
        HttpRef httpRef1 = dataHelper.createHttpRef("Ref 1", "http://ref1.com", "Desc 1");
        HttpRef httpRef2 = dataHelper.createHttpRef("Ref 2", "http://ref2.com");
        HttpRef httpRef3 = dataHelper.createHttpRef("Ref 3", "http://ref1.com", "Desc 3");
        // Default exercises
        Exercise exercise1 =
                dataHelper.createExercise("Exercise 1", "Description 1", false, Set.of(bodyPart1), Set.of(httpRef1));
        Exercise exercise2 =
                dataHelper.createExercise("Exercise 2", "Description 2", false, Set.of(bodyPart2), Set.of(httpRef2));
        // Custom exercises
        Exercise exercise3 = dataHelper.createExercise(
                "Exercise 3", "Description 3", true, Set.of(bodyPart1, bodyPart2), Set.of(httpRef1, httpRef2));
        Exercise exercise4 =
                dataHelper.createExercise("Exercise 4", "Description 4", true, Set.of(bodyPart3), Set.of(httpRef3));
        Exercise exercise5 =
                dataHelper.createExercise("Exercise 5", "Description 5", true, Set.of(bodyPart3), Set.of(httpRef3));

        Role role = dataHelper.createRole("ROLE_USER");
        User user1 = dataHelper.createUser(
                "Test Full Name",
                "test-username",
                "test@email.com",
                passwordEncoder().encode("test-password"),
                role,
                Set.of(exercise3, exercise4));
        User user2 = dataHelper.createUser(
                "Test Full Name",
                "test-username-two",
                "test2@email.com",
                passwordEncoder().encode("test-password"),
                role,
                Set.of(exercise5));

        String postfix = "?isCustomOnly=true";
        MvcResult mvcResult = mockMvc.perform(get(URL + postfix).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises", is(notNullValue())))
                .andDo(print())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        GetExercisesResponseDto responseDto = objectMapper.readValue(responseContent, GetExercisesResponseDto.class);
        assertEquals(2, responseDto.getExercises().size());

        // Exercise 3 (custom)
        ExerciseResponseDto actualExercise = responseDto.getExercises().get(0);
        Exercise expectedExercise = exercise3;
        assertEquals(expectedExercise.getId(), actualExercise.getId());
        assertEquals(expectedExercise.getTitle(), actualExercise.getTitle());
        assertEquals(expectedExercise.getDescription(), actualExercise.getDescription());
        assertEquals(
                expectedExercise.getBodyParts().size(),
                actualExercise.getBodyParts().size());
        assertEquals(
                expectedExercise.getHttpRefs().size(),
                actualExercise.getHttpRefs().size());

        List<BodyPart> expectedBodyParts = expectedExercise.getBodyParts().stream()
                .sorted(Comparator.comparingLong(BodyPart::getId))
                .toList();
        List<BodyPartResponseDto> actualBodyParts = actualExercise.getBodyParts().stream()
                .sorted(Comparator.comparingLong(BodyPartResponseDto::getId))
                .toList();
        assertEquals(expectedBodyParts.size(), actualBodyParts.size());
        assertEquals(expectedBodyParts.get(0).getId(), actualBodyParts.get(0).getId());
        assertEquals(expectedBodyParts.get(0).getName(), actualBodyParts.get(0).getName());
        assertEquals(expectedBodyParts.get(1).getId(), actualBodyParts.get(1).getId());
        assertEquals(expectedBodyParts.get(1).getName(), actualBodyParts.get(1).getName());

        List<HttpRef> expectedHttpRefs = expectedExercise.getHttpRefs().stream()
                .sorted(Comparator.comparingLong(HttpRef::getId))
                .toList();
        List<HttpRefResponseDto> actualHttpRefs = actualExercise.getHttpRefs().stream()
                .sorted(Comparator.comparingLong(HttpRefResponseDto::getId))
                .toList();
        assertEquals(expectedHttpRefs.size(), actualHttpRefs.size());
        assertEquals(expectedHttpRefs.get(0).getId(), actualHttpRefs.get(0).getId());
        assertEquals(expectedHttpRefs.get(0).getName(), actualHttpRefs.get(0).getName());
        assertEquals(
                expectedHttpRefs.get(0).getDescription(), actualHttpRefs.get(0).getDescription());
        assertEquals(expectedHttpRefs.get(1).getId(), actualHttpRefs.get(1).getId());
        assertEquals(expectedHttpRefs.get(1).getName(), actualHttpRefs.get(1).getName());
        assertEquals(
                expectedHttpRefs.get(1).getDescription(), actualHttpRefs.get(1).getDescription());

        // Exercise 4 (custom)
        actualExercise = responseDto.getExercises().get(1);
        expectedExercise = exercise4;
        assertEquals(expectedExercise.getId(), actualExercise.getId());
        assertEquals(expectedExercise.getTitle(), actualExercise.getTitle());
        assertEquals(expectedExercise.getDescription(), actualExercise.getDescription());
        assertEquals(
                expectedExercise.getBodyParts().size(),
                actualExercise.getBodyParts().size());
        assertEquals(
                expectedExercise.getHttpRefs().size(),
                actualExercise.getHttpRefs().size());

        expectedBodyParts = expectedExercise.getBodyParts().stream()
                .sorted(Comparator.comparingLong(BodyPart::getId))
                .toList();
        actualBodyParts = actualExercise.getBodyParts().stream()
                .sorted(Comparator.comparingLong(BodyPartResponseDto::getId))
                .toList();
        assertEquals(expectedBodyParts.size(), actualBodyParts.size());
        assertEquals(expectedBodyParts.get(0).getId(), actualBodyParts.get(0).getId());
        assertEquals(expectedBodyParts.get(0).getName(), actualBodyParts.get(0).getName());

        expectedHttpRefs = expectedExercise.getHttpRefs().stream()
                .sorted(Comparator.comparingLong(HttpRef::getId))
                .toList();
        actualHttpRefs = actualExercise.getHttpRefs().stream()
                .sorted(Comparator.comparingLong(HttpRefResponseDto::getId))
                .toList();
        assertEquals(expectedHttpRefs.size(), actualHttpRefs.size());
        assertEquals(expectedHttpRefs.get(0).getId(), actualHttpRefs.get(0).getId());
        assertEquals(expectedHttpRefs.get(0).getName(), actualHttpRefs.get(0).getName());
        assertEquals(
                expectedHttpRefs.get(0).getDescription(), actualHttpRefs.get(0).getDescription());
    }
}
