package greencity.security.service;

import greencity.constant.AppConstant;
import greencity.constant.ErrorMessage;
import greencity.dto.user.UserAdminRegistrationDto;
import greencity.dto.user.UserManagementDto;
import greencity.dto.user.UserVO;
import greencity.entity.Language;
import greencity.entity.OwnSecurity;
import greencity.entity.RestorePasswordEmail;
import greencity.entity.User;
import greencity.entity.VerifyEmail;
import greencity.enums.EmailNotification;
import greencity.enums.Role;
import greencity.enums.UserStatus;
import greencity.exception.exceptions.*;
import greencity.repository.UserRepo;
import greencity.security.dto.AccessRefreshTokensDto;
import greencity.security.dto.SuccessSignInDto;
import greencity.security.dto.SuccessSignUpDto;
import greencity.security.dto.ownsecurity.EmployeeSignUpDto;
import greencity.security.dto.ownsecurity.OwnSignInDto;
import greencity.security.dto.ownsecurity.OwnSignUpDto;
import greencity.security.dto.ownsecurity.SetPasswordDto;
import greencity.security.dto.ownsecurity.UpdatePasswordDto;
import greencity.security.jwt.JwtTool;
import greencity.security.repository.OwnSecurityRepo;
import greencity.security.repository.RestorePasswordEmailRepo;
import greencity.service.EmailService;
import greencity.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@inheritDoc}
 */
@Service
@Slf4j
public class OwnSecurityServiceImpl implements OwnSecurityService {
    private final OwnSecurityRepo ownSecurityRepo;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTool jwtTool;
    private final Integer expirationTime;
    private final RestorePasswordEmailRepo restorePasswordEmailRepo;
    private final ModelMapper modelMapper;
    private final UserRepo userRepo;
    private static final String VALID_PW_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+{}[]|:;<>?,./";
    private final EmailService emailService;

    /**
     * Constructor.
     */
    @Autowired
    public OwnSecurityServiceImpl(OwnSecurityRepo ownSecurityRepo,
                                  UserService userService,
                                  PasswordEncoder passwordEncoder,
                                  JwtTool jwtTool,
                                  @Value("${verifyEmailTimeHour}") Integer expirationTime,
                                  RestorePasswordEmailRepo restorePasswordEmailRepo,
                                  ModelMapper modelMapper,
                                  UserRepo userRepo,
                                  EmailService emailService) {
        this.ownSecurityRepo = ownSecurityRepo;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTool = jwtTool;
        this.expirationTime = expirationTime;
        this.restorePasswordEmailRepo = restorePasswordEmailRepo;
        this.modelMapper = modelMapper;
        this.userRepo = userRepo;
        this.emailService = emailService;
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public SuccessSignUpDto signUp(OwnSignUpDto dto, String language) {
        User user = createNewRegisteredUser(dto, jwtTool.generateTokenKey(), language);
        setUsersFields(dto, user);
        user.setVerifyEmail(createVerifyEmail(user, jwtTool.generateTokenKey()));
        user.setUuid(UUID.randomUUID().toString());
        try {
            User savedUser = userRepo.save(user);
            user.setId(savedUser.getId());
            emailService.sendVerificationEmail(savedUser.getId(), savedUser.getName(), savedUser.getEmail(),
                    savedUser.getVerifyEmail().getToken(), language, dto.isUbs());
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyRegisteredException(ErrorMessage.USER_ALREADY_REGISTERED_WITH_THIS_EMAIL);
        }
        user.setShowLocation(true);
        user.setShowEcoPlace(true);
        user.setShowShoppingList(true);
        return new SuccessSignUpDto(user.getId(), user.getName(), user.getEmail(), true);
    }

    private User createNewRegisteredUser(OwnSignUpDto dto, String refreshTokenKey, String language) {
        return User.builder()
                .name(dto.getName())
                .firstName(dto.getName())
                .email(dto.getEmail())
                .dateOfRegistration(LocalDateTime.now())
                .role(Role.ROLE_USER)
                .refreshTokenKey(refreshTokenKey)
                .lastActivityTime(LocalDateTime.now())
                .userStatus(UserStatus.CREATED)
                .emailNotification(EmailNotification.DISABLED)
                .rating(AppConstant.DEFAULT_RATING)
                .language(Language.builder()
                        .id(modelMapper.map(language, Long.class))
                        .build())
                .build();
    }

    private void setUsersFields(OwnSignUpDto dto, User user) {
        OwnSecurity ownSecurity = createOwnSecurity(dto, user);
        user.setOwnSecurity(ownSecurity);
    }

    private RestorePasswordEmail createRestorePasswordEmail(User user, String emailVerificationToken) {
        return RestorePasswordEmail.builder()
                .user(user)
                .token(emailVerificationToken)
                .expiryDate(calculateExpirationDateTime())
                .build();
    }

    private OwnSecurity createOwnSecurity(OwnSignUpDto dto, User user) {
        return OwnSecurity.builder()
                .password(passwordEncoder.encode(dto.getPassword()))
                .user(user)
                .build();
    }

    private VerifyEmail createVerifyEmail(User user, String emailVerificationToken) {
        return VerifyEmail.builder()
                .user(user)
                .token(emailVerificationToken)
                .expiryDate(calculateExpirationDateTime())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    public SuccessSignUpDto signUpEmployee(EmployeeSignUpDto employeeSignUpDto, String language) {
        String password = generatePassword();
        employeeSignUpDto.setPassword(password);
        OwnSignUpDto dto = modelMapper.map(employeeSignUpDto, OwnSignUpDto.class);
        User employee = createNewRegisteredUser(dto, jwtTool.generateTokenKey(), language);
        setUsersFields(dto, employee);
        employee.setRole(Role.ROLE_UBS_EMPLOYEE);
        employee.setRestorePasswordEmail(createRestorePasswordEmail(employee, jwtTool.generateTokenKeyWithCodedDate()));
        employee.setUuid(employeeSignUpDto.getUuid());
        employee.setShowLocation(true);
        employee.setShowEcoPlace(true);
        employee.setShowShoppingList(true);

        try {
            User savedUser = userRepo.save(employee);
            employee.setId(savedUser.getId());
            emailService.sendRestoreEmail(savedUser.getId(), savedUser.getFirstName(), employee.getEmail(),
                    savedUser.getRestorePasswordEmail().getToken(), language, dto.isUbs());
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyRegisteredException(ErrorMessage.USER_ALREADY_REGISTERED_WITH_THIS_EMAIL);
        }

        return new SuccessSignUpDto(employee.getId(), employee.getName(), employee.getEmail(), true);
    }

    private LocalDateTime calculateExpirationDateTime() {
        LocalDateTime now = LocalDateTime.now();
        return now.plusHours(this.expirationTime);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SuccessSignInDto signIn(final OwnSignInDto dto) {
        UserVO user = userService.findByEmail(dto.getEmail());
        if (user == null) {
            throw new WrongEmailException(ErrorMessage.USER_NOT_FOUND_BY_EMAIL + dto.getEmail());
        }
        if (!isPasswordCorrect(dto, user)) {
            throw new WrongPasswordException(ErrorMessage.BAD_PASSWORD);
        }
        if (user.getVerifyEmail() != null) {
            throw new EmailNotVerified("You should verify the email first, check your email box!");
        }
        if (user.getUserStatus() == UserStatus.DEACTIVATED) {
            throw new BadUserStatusException(ErrorMessage.USER_DEACTIVATED);
        }
        if (user.getUserStatus() == UserStatus.BLOCKED) {
            throw new BadUserStatusException(ErrorMessage.USER_BLOCKED);
        }
        if (user.getUserStatus() == UserStatus.CREATED) {
            throw new BadUserStatusException(ErrorMessage.USER_CREATED);
        }
        String accessToken = jwtTool.createAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtTool.createRefreshToken(user);
        return new SuccessSignInDto(user.getId(), accessToken, refreshToken, user.getName(), true);
    }

    private boolean isPasswordCorrect(OwnSignInDto signInDto, UserVO user) {
        if (user.getOwnSecurity() == null) {
            return false;
        }
        return passwordEncoder.matches(signInDto.getPassword(), user.getOwnSecurity().getPassword());
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public AccessRefreshTokensDto updateAccessTokens(String refreshToken) {
        String email;
        try {
            email = jwtTool.getEmailOutOfAccessToken(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new BadRefreshTokenException(ErrorMessage.REFRESH_TOKEN_NOT_VALID);
        }
        UserVO user = userService.findByEmail(email);
        checkUserStatus(user);
        String newRefreshTokenKey = jwtTool.generateTokenKey();
        userService.updateUserRefreshToken(newRefreshTokenKey, user.getId());
        if (jwtTool.isTokenValid(refreshToken, user.getRefreshTokenKey())) {
            user.setRefreshTokenKey(newRefreshTokenKey);
            return new AccessRefreshTokensDto(
                    jwtTool.createAccessToken(user.getEmail(), user.getRole()),
                    jwtTool.createRefreshToken(user));
        }
        throw new BadRefreshTokenException(ErrorMessage.REFRESH_TOKEN_NOT_VALID);
    }

    private void checkUserStatus(UserVO user) {
        UserStatus status = user.getUserStatus();
        if (status == UserStatus.BLOCKED) {
            throw new UserBlockedException(ErrorMessage.USER_DEACTIVATED);
        } else if (status == UserStatus.DEACTIVATED) {
            throw new UserDeactivatedException(ErrorMessage.USER_DEACTIVATED);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @author Dmytro Dovhal
     */
    @Override
    public void updatePassword(String pass, Long id) {
        String password = passwordEncoder.encode(pass);
        ownSecurityRepo.updatePassword(password, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void updateCurrentPassword(UpdatePasswordDto updatePasswordDto, String email) {
        UserVO user = userService.findByEmail(email);

        if (user.getUserStatus() != UserStatus.ACTIVATED) {
            throw new EmailNotVerified(ErrorMessage.USER_EMAIL_IS_NOT_VERIFIED);
        }

        if (!updatePasswordDto.getPassword().equals(updatePasswordDto.getConfirmPassword())) {
            throw new PasswordsDoNotMatchesException(ErrorMessage.PASSWORDS_DO_NOT_MATCH);
        }
        updatePassword(updatePasswordDto.getPassword(), user.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public UserAdminRegistrationDto managementRegisterUser(UserManagementDto dto) {
        if (userRepo.findByEmail(dto.getEmail()).isPresent()) {
            throw new UserAlreadyRegisteredException(ErrorMessage.USER_ALREADY_REGISTERED_WITH_THIS_EMAIL);
        }
        User user = managementCreateNewRegisteredUser(dto, jwtTool.generateTokenKey());
        OwnSecurity ownSecurity = managementCreateOwnSecurity(user);
        user.setOwnSecurity(ownSecurity);
        return modelMapper.map(
                savePasswordRestorationTokenForUser(user, jwtTool.generateTokenKey()), UserAdminRegistrationDto.class);
    }

    private User managementCreateNewRegisteredUser(UserManagementDto dto, String refreshTokenKey) {
        return User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .dateOfRegistration(LocalDateTime.now())
                .role(dto.getRole())
                .refreshTokenKey(refreshTokenKey)
                .lastActivityTime(LocalDateTime.now())
                .userStatus(dto.getUserStatus())
                .emailNotification(EmailNotification.DISABLED)
                .rating(AppConstant.DEFAULT_RATING)
                .language(Language.builder()
                        .id(2L)
                        .code("en")
                        .build())
                .build();
    }

    private OwnSecurity managementCreateOwnSecurity(User user) {
        return OwnSecurity.builder()
                .password(passwordEncoder.encode(generatePassword()))
                .user(user)
                .build();
    }

    /**
     * Method that generates random password.
     *
     * @return {@link String} generated password.
     */
    private String generatePassword() {
        SecureRandom secureRandom = new SecureRandom();
        String upperCaseLetters =
                RandomStringUtils.random(2, 0, 27, true, true, VALID_PW_CHARS.toCharArray(), secureRandom);
        String lowerCaseLetters =
                RandomStringUtils.random(2, 27, 53, true, true, VALID_PW_CHARS.toCharArray(), secureRandom);
        String numbers = String.valueOf(secureRandom.nextInt(100));
        String specialChar =
                RandomStringUtils
                        .random(2, 0, 0, false, false, "!@#$%^&*()-_=+{}[]|:;<>?,./".toCharArray(), secureRandom);
        String totalChars = RandomStringUtils.random(2, 0, 0, true, true,
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray(), secureRandom);
        String combinedChars = upperCaseLetters.concat(lowerCaseLetters)
                .concat(numbers)
                .concat(specialChar)
                .concat(totalChars);
        List<Character> pwdChars = combinedChars.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
        Collections.shuffle(pwdChars);
        return pwdChars.stream()
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    /**
     * Creates and saves password restoration token for a given user and publishes
     * event of sending user approval email.
     *
     * @param user  {@link User} - User whose password is to be recovered
     * @param token {@link String} - token for password restoration
     */
    private User savePasswordRestorationTokenForUser(User user, String token) {
        RestorePasswordEmail restorePasswordEmail =
                RestorePasswordEmail.builder()
                        .user(user)
                        .token(token)
                        .expiryDate(calculateExpirationDate(expirationTime))
                        .build();
        restorePasswordEmailRepo.save(restorePasswordEmail);
        user = userRepo.save(user);
        emailService.sendApprovalEmail(user.getId(), user.getName(), user.getEmail(), token);
        return user;
    }

    /**
     * Calculates token expiration date. The amount of hours, after which token will
     * be expired, is set by method parameter.
     *
     * @param expirationTimeInHours - Token expiration delay in hours
     * @return {@link LocalDateTime} - Time at which token will be expired
     */
    private LocalDateTime calculateExpirationDate(Integer expirationTimeInHours) {
        LocalDateTime now = LocalDateTime.now();
        return now.plusHours(expirationTimeInHours);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasPassword(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new WrongEmailException(ErrorMessage.USER_NOT_FOUND_BY_EMAIL));
        return user.getOwnSecurity() != null;
    }

    /**
     * {@inheritDoc}
     */
    public void setPassword(SetPasswordDto dto, String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new WrongEmailException(ErrorMessage.USER_NOT_FOUND_BY_EMAIL));
        if (hasPassword(email)) {
            throw new UserAlreadyHasPasswordException(ErrorMessage.USER_ALREADY_HAS_PASSWORD);
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new PasswordsDoNotMatchesException(ErrorMessage.PASSWORDS_DO_NOT_MATCH);
        }
        user.setOwnSecurity(OwnSecurity.builder()
                .password(passwordEncoder.encode(dto.getPassword()))
                .user(user)
                .build());
        userRepo.save(user);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        OwnSecurity ownSecurity = ownSecurityRepo.findByUserId(userId)
                .orElseThrow(() -> new WrongPasswordException("Incorrect current password"));
        if (!passwordEncoder.matches(currentPassword, ownSecurity.getPassword())) {
            throw new WrongPasswordException("Current password is incorrect");
        }
        if (!isValidPassword(newPassword)) {
            throw new BadRequestException("New password does not meet security criteria");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new PasswordsDoNotMatchesException("New password and confirm password do not match");
        }
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        ownSecurity.setPassword(encodedNewPassword);
        ownSecurityRepo.save(ownSecurity);
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 8) {
            return false;
        }
        if (!password.matches(".*\\d.*")) { // содержит цифру
            return false;
        }
        if (!password.matches(".*[a-z].*")) { // содержит маленькую букву
            return false;
        }
        if (!password.matches(".*[A-Z].*")) { // содержит большую букву
            return false;
        }
        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) { // содержит специальный символ
            return false;
        }
        return true;
    }

}