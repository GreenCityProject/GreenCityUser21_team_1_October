package greencity.controller;

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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTestBug282 {

    private MockMvc mockMvc;
    private UserService mockUserService;
    private EmailService mockEmailService;
    private RequestPostProcessor adminCredentials;
    private RequestPostProcessor userCredentials;

    @BeforeEach
    void setUp() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        mockUserService = Mockito.mock(UserService.class);
        mockEmailService = Mockito.mock(EmailService.class);
        UserController userController = new UserController(mockUserService, mockEmailService);
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
    void shouldReturn200WhenAdminAccessesWithValidParameter() throws Exception {
        Long userId = 101L;
        when(mockUserService.checkIfTheUserIsOnline(userId)).thenReturn(true);
        mockMvc.perform(get("/user/isOnline/{userId}/", userId)
                        .with(adminCredentials)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(mockUserService, times(1)).checkIfTheUserIsOnline(userId);
    }

    @Test
    void shouldReturn400WhenUserAccessesWithInvalidParameter() throws Exception {
        mockMvc.perform(get("/user/isOnline/error/", 1L)
                        .with(adminCredentials)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    private static RequestPostProcessor basicAuth(String username, String password) {
        return request -> {
            String base64Credentials = new String(java.util.Base64.getEncoder().encode((username + ":" + password).getBytes()));
            request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + base64Credentials);
            return request;
        };
    }
}