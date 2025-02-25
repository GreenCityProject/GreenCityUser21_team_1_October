package greencity.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import greencity.security.filters.AccessTokenAuthenticationFilter;
import greencity.security.jwt.JwtTool;
import greencity.security.providers.JwtAuthenticationProvider;
import greencity.service.UserService;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.Collections;

import static greencity.constant.AppConstant.*;
import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * Config for security.
 *
 * @author Nazar Stasyuk && Yurii Koval
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtTool jwtTool;
    private final UserService userService;
    private static final String USER_LINK = "/user";
    private final AuthenticationConfiguration authenticationConfiguration;

    /**
     * Constructor.
     */

    @Autowired
    public SecurityConfig(JwtTool jwtTool, UserService userService,
                          AuthenticationConfiguration authenticationConfiguration) {
        this.jwtTool = jwtTool;
        this.userService = userService;
        this.authenticationConfiguration = authenticationConfiguration;
    }

    /**
     * Bean {@link PasswordEncoder} that uses in coding password.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Method for configure security.
     *
     * @param http {@link HttpSecurity}
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(corsCustomizer -> corsCustomizer.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(Collections.singletonList("http://localhost:4200"));
                    config.setAllowedOrigins(Collections.singletonList("http://localhost:4205"));
                    config.setAllowedMethods(
                            Arrays.asList("GET", "POST", "OPTIONS", "DELETE", "PUT", "PATCH"));
                    config.setAllowedHeaders(
                            Arrays.asList("Access-Control-Allow-Origin", "Access-Control-Allow-Headers",
                                    "X-Requested-With", "Origin", "Content-Type", "Accept", "Authorization"));
                    config.setAllowCredentials(true);
                    config.setAllowedHeaders(Collections.singletonList("*"));
                    config.setMaxAge(3600L);
                    return config;
                }))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .addFilterBefore(
                        new AccessTokenAuthenticationFilter(jwtTool, authenticationManager(), userService),
                        UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((req, resp, exc) -> resp.sendError(
                                SC_UNAUTHORIZED, "Authorize first."))
                        .accessDeniedHandler((req, resp, exc) -> resp.sendError(
                                SC_FORBIDDEN, "You don't have authorities.")))
                .authorizeHttpRequests(req -> req
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/static/css/**", "/static/img/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/v2/api-docs/**",
                                "/v3/api-docs/**",
                                "/swagger.json",
                                "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers(
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/swagger-ui/**")
                        .permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/ownSecurity/verifyEmail",
                                "/ownSecurity/updateAccessToken",
                                "/ownSecurity/restorePassword",
                                "/googleAuth/getToken",
                                "/facebookSecurity/generateFacebookAuthorizeURL",
                                "/facebookSecurity/facebook",
                                "/user/activatedUsersAmount",
                                "/user/{userId}/habit/assign",
                                "/token",
                                "/socket/**",
                                "/user/findAllByEmailNotification",
                                "/user/checkByUuid",
                                "/user/get-user-rating")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/ownSecurity/signUp",
                                "/ownSecurity/signIn",
                                "/ownSecurity/updatePassword")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,"/user/isOnline/{userId}/").authenticated()
                        .requestMatchers(HttpMethod.GET, USER_LINK,
                                "/user/shopping-list-items/habits/{habitId}/shopping-list",
                                "/user/{userId}/{habitId}/custom-shopping-list-items/available",
                                "/user/{userId}/profile/",
                                "/user/isOnline/{userId}/",
                                "/user/{userId}/profileStatistics/",
                                "/user/userAndSixFriendsWithOnlineStatus",
                                "/user/userAndAllFriendsWithOnlineStatus",
                                "/user/findByIdForAchievement",
                                "/user/findNotDeactivatedByEmail",
                                "/user/findByEmail",
                                "/user/findIdByEmail",
                                "/user/findAllUsersCities",
                                "/user/findById",
                                "/user/findUserByName/**",
                                "/user/findByUuId",
                                "/user/findUuidByEmail",
                                "/user/lang",
                                "/user/createUbsRecord",
                                "/user/{userId}/sixUserFriends/",
                                "/ownSecurity/password-status",
                                "/user/emailNotifications")
                        .hasAnyRole(USER, ADMIN, UBS_EMPLOYEE, MODERATOR, EMPLOYEE)
                        .requestMatchers(HttpMethod.POST, USER_LINK,
                                "/user/shopping-list-items",
                                "/user/{userId}/habit",
                                "/ownSecurity/set-password",
                                "/email/sendReport",
                                "/email/sendHabitNotification",
                                "/email/addEcoNews",
                                "/email/changePlaceStatus",
                                "/email/general/notification")
                        .hasAnyRole(USER, ADMIN, UBS_EMPLOYEE, MODERATOR, EMPLOYEE)
                        .requestMatchers(HttpMethod.PUT,
                                "/ownSecurity/changePassword",
                                "/user/profile",
                                "/user/{id}/updateUserLastActivityTime/{date}",
                                "/user/language/{languageId}",
                                "/user/employee-email")
                        .hasAnyRole(USER, ADMIN, UBS_EMPLOYEE, MODERATOR, EMPLOYEE)
                        .requestMatchers(HttpMethod.PUT,
                                "/user/edit-authorities",
                                "/user/authorities",
                                "/user/deactivate-employee",
                                "/user/markUserAsDeactivated",
                                "/user/markUserAsActivated")
                        .hasAnyRole(ADMIN, UBS_EMPLOYEE, MODERATOR, EMPLOYEE)
                        .requestMatchers(HttpMethod.GET,
                                "/user/get-all-authorities",
                                "/user/get-positions-authorities",
                                "/user/get-employee-login-positions",
                                "/user",
                                "/user/isOnline/{userId}/")
                        .hasAnyRole(ADMIN, UBS_EMPLOYEE, MODERATOR, EMPLOYEE)
                        .requestMatchers(HttpMethod.PATCH,
                                "/user/shopping-list-items/{userShoppingListItemId}",
                                "/user/profilePicture",
                                "/user/deleteProfilePicture")
                        .hasAnyRole(USER, ADMIN, UBS_EMPLOYEE, MODERATOR, EMPLOYEE)
                        .requestMatchers(HttpMethod.DELETE,
                                "/user/shopping-list-items/user-shopping-list-items",
                                "/user/shopping-list-items")
                        .hasAnyRole(USER, ADMIN, UBS_EMPLOYEE, MODERATOR, EMPLOYEE)
                        .requestMatchers(HttpMethod.GET,
                                "/user/all",
                                "/user/roles",
                                "/user/findUserForManagement",
                                "/user/searchBy",
                                "/user/findAll")
                        .hasAnyRole(ADMIN, MODERATOR, EMPLOYEE)
                        .requestMatchers(HttpMethod.POST,
                                "/ownSecurity/sign-up-employee")
                        .hasAnyRole(UBS_EMPLOYEE)
                        .requestMatchers(HttpMethod.POST,
                                "/user/filter",
                                "/ownSecurity/register")
                        .hasAnyRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/user/{id}").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PATCH,
                                "/user/status",
                                "/user/{id}/role",
                                "/user/update/role")
                        .hasAnyRole(ADMIN)
                        .requestMatchers(HttpMethod.POST, "/management/login")
                        // .not().fullyAuthenticated()
                        .rememberMe()
                        .requestMatchers(HttpMethod.GET, "/management/login")
                        .permitAll()
                        .requestMatchers("/css/**", "/img/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.PUT, "/user/user-rating")
                        .hasAnyRole(ADMIN, MODERATOR, EMPLOYEE, UBS_EMPLOYEE, USER)
                        .anyRequest().hasAnyRole(ADMIN));
        return http.build();
    }

    /**
     * Method for configure type of authentication provider.
     *
     * @param auth {@link AuthenticationManagerBuilder}
     */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(new JwtAuthenticationProvider(jwtTool));
    }

    /**
     * Provides AuthenticationManager.
     *
     * @return {@link AuthenticationManager}
     */
    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Bean {@link GoogleIdTokenVerifier} that uses in verify googleIdToken.
     */
    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier() {
        return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
                GsonFactory.getDefaultInstance()).build();
    }

    @Bean
    public HttpClient googleAccessTokenVerifier() {
        return HttpClients.createDefault();
    }
}
