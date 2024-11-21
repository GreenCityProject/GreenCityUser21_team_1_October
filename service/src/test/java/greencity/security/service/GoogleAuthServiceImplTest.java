package greencity.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import greencity.ModelUtils;
import greencity.TestConst;
import greencity.constant.ErrorMessage;
import greencity.dto.user.UserInfoDto;
import greencity.dto.user.UserVO;
import greencity.entity.User;
import greencity.enums.Role;
import greencity.enums.UserStatus;
import greencity.exception.exceptions.UserDeactivatedException;
import greencity.security.dto.SuccessSignInDto;
import greencity.security.jwt.JwtTool;
import greencity.service.UserService;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.any;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GoogleAuthServiceImplTest {
    @Mock
    private UserService userService;
    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;
    @Mock
    private JwtTool jwtTool;
    @Mock
    private GoogleIdToken googleIdToken;
    @Spy
    private GoogleIdToken.Payload payload;
    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpResponse httpResponse;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private GoogleAuthServiceImpl googleAuthService;

    @Test
    void authenticateWithIdTokenTest() throws GeneralSecurityException, IOException {
        User user = ModelUtils.getUser();
        UserVO userVO = ModelUtils.getUserVO();

        when(googleIdTokenVerifier.verify("idToken")).thenReturn(googleIdToken);
        when(googleIdToken.getPayload()).thenReturn(payload);
        when(payload.getEmail()).thenReturn("test@mail.com");
        when(userService.findByEmail("test@mail.com")).thenReturn(userVO);

        SuccessSignInDto result = googleAuthService.authGoogle("idToken", "ua");
        assertEquals(user.getName(), result.getName());
        assertEquals(user.getId(), result.getUserId());

        verify(googleIdTokenVerifier).verify("idToken");
        verify(googleIdToken, times(3)).getPayload();
        verify(payload).getEmail();
        verify(userService).findByEmail("test@mail.com");
    }

    @Test
    void authenticateWithAccessTokenTest() throws GeneralSecurityException, IOException {
        UserInfoDto userInfo = UserInfoDto.builder()
                .sub("sub")
                .name("name")
                .givenName("given name")
                .familyName("family name")
                .picture("picture")
                .email("test@mail.com")
                .emailVerified("test@mail.com")
                .locale("locale")
                .error("error")
                .errorDescription("error description")
                .build();
        UserVO userVO = ModelUtils.getUserVO();

        String expectedJsonResponse = new ObjectMapper().writeValueAsString(userInfo);
        HttpEntity httpEntity = new StringEntity(expectedJsonResponse);

        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(objectMapper.readValue(expectedJsonResponse, UserInfoDto.class)).thenReturn(userInfo);
        when(userService.findByEmail(userInfo.getEmail())).thenReturn(userVO);

        SuccessSignInDto result = googleAuthService.authGoogle("accessToken", "ua");

        assertEquals(userVO.getName(), result.getName());
        assertEquals(userVO.getId(), result.getUserId());

        verify(googleIdTokenVerifier).verify("accessToken");
        verify(httpClient).execute(any(HttpGet.class));
        verify(httpResponse).getEntity();
        verify(objectMapper).readValue(expectedJsonResponse, UserInfoDto.class);
        verify(userService).findByEmail(userInfo.getEmail());
    }

    @Test
    void processAuthentication_UserDeactivated() {
        UserVO userVO = new UserVO();
        userVO.setEmail("test@gmail.com");
        userVO.setUserStatus(UserStatus.DEACTIVATED);
        when(userService.findByEmail("test@gmail.com")).thenReturn(userVO);
        UserDeactivatedException exception = assertThrows(UserDeactivatedException.class,
                () -> googleAuthService.processAuthentication("test@gmail.com", "Test User", "test_picture_url", "en"));
        assertEquals(ErrorMessage.USER_DEACTIVATED, exception.getMessage());
    }

    @Test
    void authenticationThrowsUserDeactivatedExceptionTest() throws GeneralSecurityException, IOException {
        UserVO userVO = UserVO.builder().id(1L).email(TestConst.EMAIL).name(TestConst.NAME)
                .role(Role.ROLE_USER).userStatus(UserStatus.DEACTIVATED)
                .lastActivityTime(LocalDateTime.now()).dateOfRegistration(LocalDateTime.now())
                .build();

        when(googleIdTokenVerifier.verify("idToken")).thenReturn(googleIdToken);
        when(googleIdToken.getPayload()).thenReturn(payload);
        when(payload.getEmail()).thenReturn("test@mail.com");
        when(userService.findByEmail("test@mail.com")).thenReturn(userVO);

        assertThrows(UserDeactivatedException.class,
                () -> googleAuthService.authGoogle("idToken", "ua"));

        verify(googleIdTokenVerifier).verify("idToken");
        verify(googleIdToken, times(3)).getPayload();
        verify(payload).getEmail();
        verify(userService).findByEmail("test@mail.com");
    }

    @Test
    void authenticateThrowsIOExceptionTest() throws IOException {
        when(httpClient.execute(any(HttpGet.class))).thenThrow(IOException.class);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> googleAuthService.authGoogle("accessToken", "ua"));

        assertEquals(ErrorMessage.INVALID_GOOGLE_TOKEN + "null", exception.getMessage());

        verify(httpClient).execute(any(HttpGet.class));
    }
}