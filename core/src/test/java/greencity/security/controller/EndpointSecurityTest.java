package greencity.security.controller;

import greencity.controller.UserController;
import greencity.security.config.SecurityTestConfig;
import greencity.service.EmailService;
import greencity.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class EndpointSecurityTest {
    private MockMvc mockMvc;
    @Mock
    private UserService mockUserService;
    @Mock
    private EmailService mockEmailService;
    @InjectMocks
    private UserController userController;
    private final RequestPostProcessor adminCredentials = basicAuth("admin", "adminpass");
    private final RequestPostProcessor userCredentials = basicAuth("user", "userpass");
    private final String ENDPOINT = "/user";
    private final String json = """
            {
              "name": "string",
              "email": "string",
              "userCredo": "string",
              "role": "ROLE_USER",
              "userStatus": "BLOCKED"
            }
            """;

    @BeforeEach
    void setUp() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(SecurityTestConfig.class);
        context.refresh();
        SecurityFilterChain securityFilterChain = context.getBean(SecurityFilterChain.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .addFilters(new FilterChainProxy(securityFilterChain))
                .build();

    }

    @Test
    @WithAnonymousUser
    void testUnauthorized() throws Exception {
        mockMvc.perform(put(ENDPOINT + "/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUser() throws Exception {
        mockMvc.perform(put(ENDPOINT + "/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(userCredentials))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdmin() throws Exception {
        mockMvc.perform(put(ENDPOINT + "/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(userCredentials)
                        .with(adminCredentials))
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
