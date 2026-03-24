package com.dadumserver.common.config;

import com.dadumserver.common.util.JwtTokenProvider;
import com.dadumserver.user.application.dto.LoginResult;
import com.dadumserver.user.application.service.UserService;
import com.dadumserver.user.domain.model.Role;
import com.dadumserver.user.domain.model.User;
import com.dadumserver.user.presentation.rest.AuthController;
import com.dadumserver.user.presentation.rest.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, UserController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class SecurityConfigTest {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private JwtTokenProvider jwtTokenProvider;

  @Test
  void loginEndpointIsPublic() throws Exception {
    when(userService.login(any())).thenReturn(new LoginResult("access", 3600L, "refresh", 1209600L));

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"kim@example.com","password":"password1234"}
                """))
        .andExpect(status().isOk());
  }

  @Test
  void signupEndpointIsPublic() throws Exception {
    when(userService.createUser(anyString(), anyString()))
        .thenReturn(new User(UUID.randomUUID(), "kim@example.com", "encoded-password"));

    mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"kim@example.com","password":"password1234"}
                """))
        .andExpect(status().isCreated());
  }

  @Test
  void signupValidationErrorDoesNotBecomeUnauthorized() throws Exception {
    when(userService.createUser(anyString(), anyString()))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid email is required"));

    mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"string","password":"password1234"}
                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void protectedEndpointRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/user"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void userEndpointRequiresAdminRole() throws Exception {
    when(jwtTokenProvider.validateAccessToken("user-token")).thenReturn(true);
    when(jwtTokenProvider.getUserId("user-token")).thenReturn(UUID.randomUUID());
    when(jwtTokenProvider.getUserRole("user-token")).thenReturn(Role.user);

    mockMvc.perform(get("/user")
            .header("Authorization", "Bearer user-token"))
        .andExpect(status().isForbidden());
  }

  @Test
  void userEndpointAllowsAdminRole() throws Exception {
    when(jwtTokenProvider.validateAccessToken("admin-token")).thenReturn(true);
    when(jwtTokenProvider.getUserId("admin-token")).thenReturn(UUID.randomUUID());
    when(jwtTokenProvider.getUserRole("admin-token")).thenReturn(Role.admin);
    when(userService.getUsers()).thenReturn(List.of(
        new User(UUID.randomUUID(), "admin@example.com", "encoded-password", Role.admin)
    ));

    mockMvc.perform(get("/user")
            .header("Authorization", "Bearer admin-token"))
        .andExpect(status().isOk());
  }
}
