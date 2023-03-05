package ch.uzh.ifi.hase.soprafs23.controller;

import ch.uzh.ifi.hase.soprafs23.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs23.entity.User;
import ch.uzh.ifi.hase.soprafs23.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs23.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {
    private final String username = "user";
    private final String password = "password";
    private final String encoded = Base64.getEncoder().encodeToString((username +":" + password).getBytes());


  @Autowired
  private MockMvc mockMvc;


  @MockBean
  private UserService userService;

  @WithMockUser("user")
  @Test
  public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
    // given
    User user = new User();
    user.setName("Firstname Lastname");
    user.setUsername("firstname@lastname");
    user.setStatus(UserStatus.OFFLINE);

    List<User> allUsers = Collections.singletonList(user);

    // this mocks the UserService -> we define above what the userService should
    // return when getUsers() is called
    given(userService.getUsers()).willReturn(allUsers);

    // when
    MockHttpServletRequestBuilder getRequest = get("/users").contentType(MediaType.APPLICATION_JSON).with(csrf());
    // then
    mockMvc.perform(getRequest).andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name", is(user.getName())))
        .andExpect(jsonPath("$[0].username", is(user.getUsername())))
        .andExpect(jsonPath("$[0].status", is(user.getStatus().toString())));
  }


  @Test
  @WithMockUser("user")
  public void getSpecificUser_thenReturnJsonArray() throws Exception {
      User user = new User();
      user.setName("Firstname Lastname");
      user.setUsername("username");
      user.setPassword("password");
      user.setId(1L);
      given(userService.getUserById(1L)).willReturn(user);

      MockHttpServletRequestBuilder getRequest = get("/users/1").contentType(MediaType.APPLICATION_JSON);
      mockMvc.perform(getRequest)
              .andExpect(jsonPath("$.password", is(user.getPassword())))
              .andExpect(jsonPath("$.name", is(user.getName())))
              .andExpect(jsonPath("$.username", is(user.getUsername())))
              .andExpect(status().isOk());
  }

  @Test
  @WithMockUser("user")
  public void getSpecificUser_notFoundWhenInvalid() throws Exception {
      given(userService.getUserById(1L)).willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
      MockHttpServletRequestBuilder getUser = get("/users/1").contentType(MediaType.APPLICATION_JSON);
      mockMvc.perform(getUser).andExpect(status().isNotFound());
  }


    @Test
    @WithMockUser("user")
    public void createUserFail_alreadyExists() throws Exception {
        // given
        User user = new User();
        user.setName("Test User");
        user.setUsername("testUsername");
        user.setPassword("testPassword");

        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setName("Test User");
        userPostDTO.setUsername("testUsername");

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder postRequest = post("/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO))
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encoded);

        //First creation of User with username "testUsername" should work fine
        given(userService.createUser(Mockito.any())).willReturn(user);
        mockMvc.perform(postRequest)
                .andExpect(jsonPath("$.password", is(user.getPassword())))
                .andExpect(jsonPath("$.name", is(user.getName())))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(status().isCreated());
        //Second creation of User with username "testUsername" should throw Conflict response
        given(userService.createUser(Mockito.any())).willThrow(new ResponseStatusException(HttpStatus.CONFLICT));
        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());

    }

  @Test
  @WithMockUser("user")
  public void createUser_validInput_userCreated() throws Exception {
    // given
    User user = new User();
    user.setId(1L);
    user.setName("Test User");
    user.setUsername("testUsername");
    user.setToken("1");
    user.setPassword("testPassword");
    user.setStatus(UserStatus.ONLINE);

    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setName("Test User");
    userPostDTO.setUsername("testUsername");

    given(userService.createUser(Mockito.any())).willReturn(user);
    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder postRequest = post("/users")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPostDTO))
        .header(HttpHeaders.AUTHORIZATION, "Basic " + encoded);

    // then
    mockMvc.perform(postRequest)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", is(user.getId().intValue())))
        .andExpect(jsonPath("$.password", is(user.getPassword())))
        .andExpect(jsonPath("$.name", is(user.getName())))
        .andExpect(jsonPath("$.username", is(user.getUsername())))
        .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
  }


  @Test
  @WithMockUser("user")
  public void updateUser_validId() throws Exception{
      User user = new User();
      user.setId(1L);
      user.setName("First User");
      user.setUsername("firstUsername");
      user.setToken("1");
      user.setPassword("firstPassword");

      UserPostDTO userPostDTO = new UserPostDTO();
      userPostDTO.setName("First User");
      userPostDTO.setUsername("firstUsername");

      given(userService.createUser(Mockito.any())).willReturn(user);
      // when/then -> do the request + validate the result
      MockHttpServletRequestBuilder postRequest = post("/users")
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(asJsonString(userPostDTO))
              .header(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
      mockMvc.perform(postRequest)
              .andExpect(jsonPath("$.id", is(user.getId().intValue())))
              .andExpect(jsonPath("$.password", is(user.getPassword())))
              .andExpect(jsonPath("$.name", is(user.getName())))
              .andExpect(jsonPath("$.username", is(user.getUsername())));

      User newUser = new User();
      newUser.setName("secondName");
      newUser.setUsername("secondUsername");
      newUser.setId(1L);
      newUser.setPassword("secondPassword");

      UserPostDTO secondUserPostDTO = new UserPostDTO();
      secondUserPostDTO.setName("secondName");
      secondUserPostDTO.setUsername("secondUsername");
      secondUserPostDTO.setPassword("secondPassword");
      userService.updateUser(user.getId(), newUser);

      MockHttpServletRequestBuilder putRequest = put("/users/1")
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(asJsonString(secondUserPostDTO));
      mockMvc.perform(putRequest).andExpect(status().isNoContent());

      given(userService.getUserById(user.getId())).willReturn(newUser);
      MockHttpServletRequestBuilder getRequest = get("/users/1")
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON);
      mockMvc.perform(getRequest)
              .andExpect(jsonPath("$.id", is(user.getId().intValue())))
              .andExpect(jsonPath("$.password", is(newUser.getPassword())))
              .andExpect(jsonPath("$.name", is(newUser.getName())))
              .andExpect(jsonPath("$.username", is(newUser.getUsername())));

  }

  @Test
  @WithMockUser("user")
  public void updateUser_failWhenInvalidId() throws Exception{
      UserPostDTO userPostDTO = new UserPostDTO();

      Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)).when(userService).updateUser(Mockito.anyLong(), Mockito.any());
      MockHttpServletRequestBuilder putRequest = put("/users/1")
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(asJsonString(userPostDTO));
      mockMvc.perform(putRequest).andExpect(status().isNotFound());

  }
  /**
   * Helper Method to convert userPostDTO into a JSON string such that the input
   * can be processed
   * Input will look like this: {"name": "Test User", "username": "testUsername"}
   * 
   * @param object
   * @return string
   */
  private String asJsonString(final Object object) {
    try {
      return new ObjectMapper().writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          String.format("The request body could not be created.%s", e.toString()));
    }
  }
}