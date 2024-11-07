package greencity.controller;

import greencity.service.EmailService;
import greencity.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
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

class UserControllerTestBug279 {

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
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        adminCredentials = basicAuth("admin", "adminpass");
        userCredentials = basicAuth("user", "userpass");
    }

    @Test
    void shouldReturn200WhenAdminAccessesWithValidValue() throws Exception {
        mockMvc.perform(get("/user/findUserForManagement")
                        .with(adminCredentials)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", ""))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn403WhenUserAccessesWithValidValue() throws Exception {
        mockMvc.perform(get("/user/findUserForManagement", 1L)
                        .with(userCredentials)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", ""))
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