package greencity.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import greencity.constant.ErrorMessage;
import greencity.dto.user.UserVO;
import greencity.enums.UserStatus;
import greencity.exception.exceptions.UserDeactivatedException;
import greencity.repository.UserRepo;
import greencity.security.jwt.JwtTool;
import greencity.service.UserService;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.transaction.PlatformTransactionManager;
import java.io.IOException;
import java.security.GeneralSecurityException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleAuthServiceImplTest {
    @Mock
    private UserService userService;
    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;
    @Mock
    private JwtTool jwtTool;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private UserRepo userRepo;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private HttpClient googleAccessTokenVerifier;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private GoogleAuthServiceImpl googleAuthService;

    private final String googleToken = "testToken";
    private final String language = "en";
    private final String email = "test@gmail.com";
    private final String name = "Test User";
    private final String profilePicture = "test_picture_url";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void authGoogle_InvalidToken() throws GeneralSecurityException, IOException {
        when(googleIdTokenVerifier.verify(googleToken)).thenReturn(null);

        doThrow(new IllegalArgumentException(ErrorMessage.INVALID_GOOGLE_TOKEN))
                .when(googleAccessTokenVerifier).execute(any(HttpGet.class));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> googleAuthService.authGoogle(googleToken, language));

        assertEquals(ErrorMessage.INVALID_GOOGLE_TOKEN, exception.getMessage());
    }

    @Test
    void processAuthentication_UserDeactivated() {
        UserVO userVO = new UserVO();
        userVO.setEmail(email);
        userVO.setUserStatus(UserStatus.DEACTIVATED);

        when(userService.findByEmail(email)).thenReturn(userVO);

        UserDeactivatedException exception = assertThrows(UserDeactivatedException.class,
                () -> googleAuthService.processAuthentication(email, name, profilePicture, language));

        assertEquals(ErrorMessage.USER_DEACTIVATED, exception.getMessage());
    }
}