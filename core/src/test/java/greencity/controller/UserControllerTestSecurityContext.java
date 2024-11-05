package greencity.controller;

import greencity.config.SecurityConfig;
import greencity.security.filters.AccessTokenAuthenticationFilter;
import greencity.security.jwt.JwtTool;
import greencity.service.UserService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class UserControllerTestSecurityContext {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        UserService mockUserService = Mockito.mock(UserService.class);
        JwtTool mockJwtTool = Mockito.mock(JwtTool.class);
        AuthenticationConfiguration mockAuthenticationConfiguration = Mockito.mock(AuthenticationConfiguration.class);
        SecurityConfig securityConfig = new SecurityConfig(mockJwtTool, mockUserService, mockAuthenticationConfiguration);
        SecurityFilterChain securityFilterChain = securityConfig.securityFilterChain(http);
        int index = 0;
        for (var securityFilter : securityFilterChain.getFilters()) {
            if (securityFilter instanceof AccessTokenAuthenticationFilter) {
                securityFilterChain.getFilters().set(index, new BasicAuthenticationFilter(authenticationManager));
            }
            index++;
        }
        return securityFilterChain;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.withUsername("admin")
                .password(passwordEncoder.encode("adminpass"))
                .roles("ADMIN")
                .build();

        UserDetails user = User.withUsername("user")
                .password(passwordEncoder.encode("userpass"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(admin, user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
