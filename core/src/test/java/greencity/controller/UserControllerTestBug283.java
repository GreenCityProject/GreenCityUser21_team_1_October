package greencity.controller;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTestBug283 {

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
    void shouldReturn403WhenUserAccessesWithValidParameters() throws Exception {
        Long userId = 101L;
        mockMvc.perform(get("/user/isOnline/{userId}/", userId)
                        .with(userCredentials)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    private static RequestPostProcessor basicAuth(String username, String password) {
        return request -> {
            String base64Credentials = new String(java.util.Base64.getEncoder().encode((username + ":" + password).getBytes()));
            request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + base64Credentials);
            return request;
        };
    }
}