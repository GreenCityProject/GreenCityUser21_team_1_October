package greencity.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import static greencity.constant.AppConstant.*;
import greencity.constant.ErrorMessage;
import greencity.dto.user.UserInfoDto;
import greencity.dto.user.UserVO;
import greencity.entity.Language;
import greencity.entity.User;
import greencity.enums.EmailNotification;
import greencity.enums.Role;
import greencity.enums.UserStatus;
import greencity.exception.exceptions.UserDeactivatedException;
import greencity.repository.UserRepo;
import greencity.security.dto.SuccessSignInDto;
import greencity.security.jwt.JwtTool;
import greencity.service.UserService;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthServiceImpl implements GoogleAuthService {
    private final UserService userService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final JwtTool jwtTool;
    private final ModelMapper modelMapper;
    private final UserRepo userRepo;
    private final PlatformTransactionManager transactionManager;
    private final HttpClient googleAccessTokenVerifier;
    private final ObjectMapper objectMapper;

    @Value("${google.resource.userInfoUri}")
    private String userInfoUri;

    @Override
    public SuccessSignInDto authGoogle(String googleToken, String language) {
        try {
            GoogleIdToken googleIdToken = googleIdTokenVerifier.verify(googleToken);
            if (googleIdToken == null) {
                throw new IllegalArgumentException(ErrorMessage.EXPIRED_GOOGLE_TOKEN);
            }
            String email = googleIdToken.getPayload().getEmail();
            String userName = (String) googleIdToken.getPayload().get("name");
            String profilePicture = (String) googleIdToken.getPayload().get("picture");
            return processAuthentication(email, userName, profilePicture, language);
        } catch (IllegalArgumentException e) {
            return authenticateByGoogleAccessToken(googleToken, language);
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalArgumentException(ErrorMessage.INVALID_GOOGLE_TOKEN + e.getMessage());
        }
    }

    public SuccessSignInDto authenticateByGoogleAccessToken(String googleAccessToken, String language) {
        try {
            UserInfoDto userInfoDto = retrieveGoogleUserInfo(googleAccessToken);
            if (userInfoDto.getEmail() == null) {
                throw new IllegalArgumentException(ErrorMessage.INVALID_GOOGLE_TOKEN);
            }
            String email = userInfoDto.getEmail();
            String userName = userInfoDto.getName();
            String profilePicture = userInfoDto.getPicture();
            return processAuthentication(email, userName, profilePicture, language);
        } catch (IOException e) {
            throw new IllegalArgumentException(ErrorMessage.INVALID_GOOGLE_TOKEN + e.getMessage());
        }
    }

    public SuccessSignInDto processAuthentication(String email, String userName, String profilePicture,
                                                  String language) {
        UserVO userVO = userService.findByEmail(email);
        if (userVO == null) {
            log.error(ErrorMessage.USER_NOT_FOUND_BY_EMAIL + "{}", email);
            return handleNewUser(email, userName, profilePicture, language);
        } else {
            if (userVO.getUserStatus() == UserStatus.DEACTIVATED) {
                throw new UserDeactivatedException(ErrorMessage.USER_DEACTIVATED);
            }
            return getSuccessSignInDto(userVO);
        }
    }

    private SuccessSignInDto handleNewUser(String email, String userName, String profilePicture, String language) {
        User newUser = createNewUser(email, userName, profilePicture, language);
        User createdUser = saveCreatedUser(newUser);
        UserVO userVO = modelMapper.map(createdUser, UserVO.class);
        return getSuccessSignInDto(userVO);
    }

    private User createNewUser(String email, String userName, String profilePicture, String language) {
        User user = User.builder()
                .email(email)
                .name(userName)
                .role(Role.ROLE_USER)
                .language(Language.builder().id(modelMapper.map(language, Long.class)).build())
                .refreshTokenKey(jwtTool.generateTokenKey())
                .userStatus(UserStatus.ACTIVATED)
                .emailNotification(EmailNotification.DISABLED)
                .dateOfRegistration(LocalDateTime.now())
                .lastActivityTime(LocalDateTime.now())
                .profilePicturePath(profilePicture)
                .rating(DEFAULT_RATING)
                .build();
        return user;
    }
    private SuccessSignInDto getSuccessSignInDto(UserVO user) {
        String accessToken = jwtTool.createAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtTool.createRefreshToken(user);
        return new SuccessSignInDto(user.getId(), accessToken, refreshToken, user.getName(), false);
    }

    private UserInfoDto retrieveGoogleUserInfo(String tokenAccess) throws IOException {
        String requestUrl = userInfoUri + tokenAccess;
        HttpGet request = new HttpGet(requestUrl);
        HttpResponse response = googleAccessTokenVerifier.execute(request);
        String jsonResponse = EntityUtils.toString(response.getEntity());
        return objectMapper.readValue(jsonResponse, UserInfoDto.class);
    }

    private User saveCreatedUser(User createdUser) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return transactionTemplate.execute(status -> {
            createdUser.setUuid(UUID.randomUUID().toString());
            Long id = userRepo.save(createdUser).getId();
            createdUser.setId(id);
            return createdUser;
        });
    }
}
