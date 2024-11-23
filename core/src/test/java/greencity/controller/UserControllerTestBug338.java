package greencity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import greencity.dto.user.UserRoleDto;
import greencity.enums.Role;
import greencity.security.service.OwnSecurityService;
import greencity.service.EmailService;
import greencity.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTestBug338 {

    private MockMvc mockMvc;
    private UserService mockUserService;
    private EmailService mockEmailService;
    private RequestPostProcessor adminCredentials;
    private RequestPostProcessor userCredentials;
    private OwnSecurityService securityService;
    @BeforeEach
    void setUp() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        mockUserService = Mockito.mock(UserService.class);
        mockEmailService = Mockito.mock(EmailService.class);
        securityService = Mockito.mock(OwnSecurityService.class);
        UserController userController = new UserController(mockUserService, mockEmailService, securityService);
        context.register(UserControllerTestSecurityContext.class);
        context.refresh();
        SecurityFilterChain securityFilterChain = context.getBean(SecurityFilterChain.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .addFilters(new FilterChainProxy(securityFilterChain))
                .build();
        adminCredentials = basicAuth("admin", "adminpass");
        userCredentials = basicAuth("user", "userpass");
    }

    @Test
    void shouldReturn200WhenAdminAccessesWithValidParameters() throws Exception {
        Long userId = 1L;
        String userRole = "ROLE_USER";
        Map<String, String> body = Map.of("role", userRole);
        UserRoleDto userRoleDto = new UserRoleDto(userId, Role.valueOf(userRole));
        when(mockUserService.updateRole(userId, Role.valueOf(userRole), "admin")).thenReturn(userRoleDto);
        mockMvc.perform(patch("/user/{id}/role", userId)
                        .with(adminCredentials)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(body)))
                .andExpect(status().isOk());
        verify(mockUserService, times(1)).updateRole(userId, Role.valueOf(userRole), "admin");
    }

    private static RequestPostProcessor basicAuth(String username, String password) {
        return request -> {
            String base64Credentials = new String(java.util.Base64.getEncoder().encode((username + ":" + password).getBytes()));
            request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + base64Credentials);
            return request;
        };
    }
}