package greencity.service;

import greencity.ModelUtils;
import greencity.TestConst;
import greencity.client.RestClient;
import greencity.constant.ErrorMessage;
import greencity.constant.UpdateConstants;
import greencity.dto.PageableAdvancedDto;
import greencity.dto.PageableDto;
import greencity.dto.UbsCustomerDto;
import greencity.dto.filter.FilterUserDto;
import greencity.dto.shoppinglist.CustomShoppingListItemResponseDto;
import greencity.dto.ubs.UbsTableCreationDto;
import greencity.dto.user.*;
import greencity.entity.Language;
import greencity.entity.User;
import greencity.entity.UserDeactivationReason;
import greencity.entity.VerifyEmail;
import greencity.enums.EmailNotification;
import greencity.enums.Role;
import greencity.exception.exceptions.*;
import greencity.filters.UserSpecification;
import greencity.repository.LanguageRepo;
import greencity.repository.UserDeactivationRepo;
import greencity.repository.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static greencity.ModelUtils.*;
import static greencity.enums.Role.ROLE_USER;
import static greencity.enums.UserStatus.ACTIVATED;
import static greencity.enums.UserStatus.DEACTIVATED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {
    @Mock
    UserRepo userRepo;

    @Mock
    RestClient restClient;

    @Mock
    UserDeactivationRepo userDeactivationRepo;

    @Mock
    LanguageRepo languageRepo;

    private User user = User.builder()
            .id(1L)
            .name("Taras")
            .email("test@gmail.com")
            .role(ROLE_USER)
            .userStatus(ACTIVATED)
            .emailNotification(EmailNotification.DISABLED)
            .lastActivityTime(LocalDateTime.of(2020, 10, 10, 20, 10, 10))
            .dateOfRegistration(LocalDateTime.now())
            .build();

    private User user1 = User.builder()
            .uuid("444e66e8-8daa-4cb0-8269-a8d856e7dd15")
            .name("Nazar")
            .build();

    private UserVO userVO = UserVO.builder()
            .id(1L)
            .name("Test Testing")
            .email("test@gmail.com")
            .role(ROLE_USER)
            .userStatus(ACTIVATED)
            .emailNotification(EmailNotification.DISABLED)
            .lastActivityTime(LocalDateTime.of(2020, 10, 10, 20, 10, 10))
            .dateOfRegistration(LocalDateTime.now())
            .build();
    private User user2 = User.builder()
            .id(2L)
            .name("Test Testing")
            .email("test2@gmail.com")
            .role(Role.ROLE_MODERATOR)
            .userStatus(ACTIVATED)
            .emailNotification(EmailNotification.DISABLED)
            .lastActivityTime(LocalDateTime.of(2020, 10, 10, 20, 10, 10))
            .dateOfRegistration(LocalDateTime.now())
            .build();
    private UserVO userVO2 =
            UserVO.builder()
                    .id(2L)
                    .name("Test Testing")
                    .email("test@gmail.com")
                    .role(Role.ROLE_MODERATOR)
                    .userStatus(ACTIVATED)
                    .emailNotification(EmailNotification.DISABLED)
                    .lastActivityTime(LocalDateTime.of(2020, 10, 10, 20, 10, 10))
                    .dateOfRegistration(LocalDateTime.now())
                    .build();
    private UbsCustomerDto ubsCustomerDto =
            UbsCustomerDto.builder()
                    .name("Nazar")
                    .phoneNumber("09876543322")
                    .email("nazar98struk.gmail.com")
                    .build();

    private String language = "ua";
    private Long userId = user.getId();

    private Long habitId = 1L;
    private Long userId2 = user2.getId();
    private String userEmail = user.getEmail();

    @InjectMocks
    private UserServiceImpl userService;
    @Mock
    private ModelMapper modelMapper;

    @Test
    void findAllByEmailNotification() {
        when(userRepo.findAllByEmailNotification(any(EmailNotification.class)))
                .thenReturn(Collections.singletonList(user));
        when(modelMapper.map(user, UserVO.class)).thenReturn(userVO);
        assertEquals(Collections.singletonList(userVO),
                userService.findAllByEmailNotification(EmailNotification.IMMEDIATELY));
    }

    @Test
    void scheduleDeleteDeactivatedUsers() {
        when(userRepo.scheduleDeleteDeactivatedUsers()).thenReturn(1);
        assertEquals(1, userService.scheduleDeleteDeactivatedUsers());
    }

    @Test
    void findAllUsersCities() {
        List<String> expected = Collections.singletonList("city");
        when(userRepo.findAllUsersCities()).thenReturn(expected);
        assertEquals(expected, userService.findAllUsersCities());
    }

    @Test
    void saveTest() {
        when(userRepo.findByEmail(userEmail)).thenReturn(Optional.ofNullable(user));
        when(userService.findByEmail(userEmail)).thenReturn(userVO);
        when(modelMapper.map(userVO, User.class)).thenReturn(user);
        when(userRepo.save(user)).thenReturn(user);
        when(modelMapper.map(user, UserVO.class)).thenReturn(userVO);
        assertEquals(userVO, userService.save(userVO));
    }

    @Test
    void updateUserStatusDeactivatedTest() {
        Language defaultLanguage = new Language();
        defaultLanguage.setId(1L);
        defaultLanguage.setCode("ua");
        when(languageRepo.findById(1L)).thenReturn(Optional.of(defaultLanguage));

        when(userRepo.findById(userId2)).thenReturn(Optional.of(user2));
        when(modelMapper.map(user2, UserVO.class)).thenReturn(userVO2);
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(user2));
        when(modelMapper.map(Optional.of(user2), UserVO.class)).thenReturn(userVO2);
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserVO.class)).thenReturn(userVO);
        when(userRepo.save(any())).thenReturn(user);

        UserStatusDto value = new UserStatusDto();
        value.setUserStatus(DEACTIVATED);
        when(modelMapper.map(user, UserStatusDto.class)).thenReturn(value);
        assertEquals(DEACTIVATED, userService.updateStatus(userId, DEACTIVATED, any()).getUserStatus());
    }

    @Test
    void updateUserStatusLowRoleLevelException() {
        Language defaultLanguage = new Language();
        defaultLanguage.setId(1L);
        defaultLanguage.setCode("ua");
        when(languageRepo.findById(1L)).thenReturn(Optional.of(defaultLanguage));

        user.setRole(Role.ROLE_MODERATOR);
        userVO.setRole(Role.ROLE_MODERATOR);
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(user2));
        when(modelMapper.map(user2, UserVO.class)).thenReturn(userVO2);
        when(userRepo.findById(any())).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserVO.class)).thenReturn(userVO);
        assertThrows(LowRoleLevelException.class, () -> userService.updateStatus(userId, DEACTIVATED, "email"));
    }

    @Test
    void updateRoleTest() {
        // given
        ReflectionTestUtils.setField(userService, "modelMapper", new ModelMapper());
        UserRoleDto userRoleDto = new UserRoleDto();
        userRoleDto.setRole(Role.ROLE_MODERATOR);
        when(userRepo.findById(any())).thenReturn(Optional.of(user));
        when(userRepo.findByEmail(any())).thenReturn(Optional.of(user2));
        when(modelMapper.map(user, UserRoleDto.class)).thenReturn(userRoleDto);
        user.setRole(Role.ROLE_MODERATOR);

        // then
        assertEquals(
                Role.ROLE_MODERATOR,
                userService.updateRole(userId, Role.ROLE_MODERATOR, user2.getEmail()).getRole());
    }

    @Test
    void updateRoleOnTheSameUserTest() {
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        assertThrows(BadUpdateRequestException.class, () -> userService.updateRole(userId, null, userEmail));
    }

    @Test
    void updateRoleOfNonExistingUser() {
        when(userRepo.findById(userId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.updateRole(userId, null, userEmail));
    }

    @Test
    void findByIdTest() {
        Long id = 1L;
        Language defaultLanguage = new Language();
        defaultLanguage.setId(1L);
        defaultLanguage.setCode("ua");
        when(languageRepo.findById(1L)).thenReturn(Optional.of(defaultLanguage));

        User user = new User();
        user.setId(1L);
        user.setLanguage(defaultLanguage);

        when(userRepo.findById(id)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserVO.class)).thenReturn(userVO);
        assertEquals(userVO, userService.findById(id));
        verify(userRepo, times(1)).findById(id);
    }

    @Test
    void findByIdBadIdTest() {
        when(userRepo.findById(any())).thenThrow(WrongIdException.class);
        assertThrows(WrongIdException.class, () -> userService.findById(1L));
    }

    @Test
    void deleteByIdExceptionBadIdTest() {
        assertThrows(WrongIdException.class, () -> userService.deleteById(1L));
    }

    @Test
    void deleteByNullIdExceptionTest() {
        when(userRepo.findById(1L)).thenThrow(new WrongIdException(""));
        assertThrows(WrongIdException.class, () -> userService.deleteById(1L));
    }

    @Test
    void deleteByExistentIdTest() {
        Language defaultLanguage = new Language();
        defaultLanguage.setId(1L);
        defaultLanguage.setCode("ua");
        when(languageRepo.findById(1L)).thenReturn(Optional.of(defaultLanguage));

        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserVO.class)).thenReturn(userVO);
        when(modelMapper.map(userVO, User.class)).thenReturn(user);
        userService.deleteById(userId);
        verify(userRepo).delete(user);
    }

    @Test
    void findIdByEmail() {
        String email = "email";
        when(userRepo.findIdByEmail(email)).thenReturn(Optional.of(2L));
        assertEquals(2L, (long) userService.findIdByEmail(email));
    }

    @Test
    void findIdByEmailNotFound() {
        String email = "email";

        assertThrows(WrongEmailException.class, () -> userService.findIdByEmail(email));
    }

    @Test
    void findUuIdByEmailTest() {
        String email = "email";
        when(userRepo.findUuidByEmail(email)).thenReturn(Optional.of("email"));
        assertEquals("email", userService.findUuIdByEmail(email));
    }

    @Test
    void findUuIdByEmailNotFoundTest() {
        String email = "email";

        assertThrows(WrongEmailException.class, () -> userService.findUuIdByEmail(email));
    }

    @Test
    void findAllTest() {
        List<UserVO> userVO = List.of(getUserVO(), getUserVO(), getUserVO());
        when(modelMapper.map(userRepo.findAll(), new TypeToken<List<UserVO>>() {
        }.getType())).thenReturn(userVO);
        assertEquals(userVO, userService.findAll());

    }

    @Test
    void findByPage() {
        int pageNumber = 0;
        int pageSize = 1;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        User user = new User();
        user.setName("Roman Romanovich");

        UserForListDto userForListDto = new UserForListDto();
        userForListDto.setName("Roman Romanovich");

        Page<User> usersPage = new PageImpl<>(Collections.singletonList(user), pageable, 1);
        List<UserForListDto> userForListDtos = Collections.singletonList(userForListDto);

        PageableDto<UserForListDto> userPageableDto =
                new PageableDto<>(userForListDtos,
                        userForListDtos.size(), 0, 1);

        ReflectionTestUtils.setField(userService, "modelMapper", new ModelMapper());

        when(userRepo.findAll(pageable)).thenReturn(usersPage);

        assertEquals(userPageableDto, userService.findByPage(pageable));
        verify(userRepo, times(1)).findAll(pageable);
    }

    @Test
    void getRoles() {
        RoleDto roleDto = new RoleDto(Role.class.getEnumConstants());
        assertEquals(roleDto, userService.getRoles());
    }

    @Test
    void getEmailStatusesTest() {
        List<EmailNotification> placeStatuses =
                Arrays.asList(EmailNotification.class.getEnumConstants());

        assertEquals(placeStatuses, userService.getEmailNotificationsStatuses());
    }

    @Test
    void updateLastVisit() {
        Language defaultLanguage = new Language();
        defaultLanguage.setId(1L);
        defaultLanguage.setCode("ua");
        when(languageRepo.findById(1L)).thenReturn(Optional.of(defaultLanguage));

        when(modelMapper.map(userVO, User.class)).thenReturn(user);
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserVO.class)).thenReturn(userVO);
        when(userRepo.save(any())).thenReturn(user);
        LocalDateTime localDateTime = user.getLastActivityTime().minusHours(1);
        assertNotEquals(localDateTime, userService.updateLastVisit(userVO).getLastActivityTime());
    }

    @Test
    void getUsersByFilter() {
        int pageNumber = 0;
        int pageSize = 1;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        User user = new User();
        user.setName("Roman Bezos");

        UserForListDto userForListDto = new UserForListDto();
        userForListDto.setName("Roman Bezos");

        Page<User> usersPage = new PageImpl<>(Collections.singletonList(user), pageable, 1);
        List<UserForListDto> userForListDtos = Collections.singletonList(userForListDto);

        PageableDto<UserForListDto> userPageableDto =
                new PageableDto<>(userForListDtos,
                        userForListDtos.size(), 0, 1);

        ReflectionTestUtils.setField(userService, "modelMapper", new ModelMapper());

        when(userRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(usersPage);
        FilterUserDto filterUserDto = new FilterUserDto();
        assertEquals(userPageableDto, userService.getUsersByFilter(filterUserDto, pageable));
    }

    @Test
    void getUserUpdateDtoByEmail_AdminRole() {
        String email = "test@example.com";

        UserVO userVO = new UserVO();
        userVO.setRole(Role.ROLE_ADMIN);

        User userEntity = new User();
        userEntity.setEmail(email);
        userEntity.setName("Test User");

        UserUpdateDto expectedDto = new UserUpdateDto();
        expectedDto.setName("Test User");
        expectedDto.setEmailNotification(userEntity.getEmailNotification());

        when(userRepo.findByEmail(email)).thenReturn(Optional.of(userEntity));
        when(userService.findByEmail(email)).thenReturn(userVO);

        ReflectionTestUtils.setField(userService, "modelMapper", new ModelMapper());
        UserUpdateDto actualDto = userService.getUserUpdateDtoByEmail(email);

        assertEquals(expectedDto.getName(), actualDto.getName());
        assertEquals(expectedDto.getEmailNotification(), actualDto.getEmailNotification());
        verify(userRepo, times(3)).findByEmail(email);
    }
    @Test
    void getUserUpdateDtoByEmail_UserRole() {
        String email = "test@example.com";

        UserVO userVO = new UserVO();
        userVO.setRole(Role.ROLE_USER);

        when(userRepo.findByEmail(email)).thenReturn(Optional.of(new User()));
        when(userService.findByEmail(email)).thenReturn(userVO);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.getUserUpdateDtoByEmail(email);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Access is denied", exception.getReason());
        verify(userRepo, times(2)).findByEmail(email);
    }

    @Test
    void update() {
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userRepo.save(any())).thenReturn(user);
        UserUpdateDto userUpdateDto = new UserUpdateDto();
        userUpdateDto.setName(user.getName());
        userUpdateDto.setEmailNotification(user.getEmailNotification());
        assertEquals(userUpdateDto, userService.update(userUpdateDto, ""));
        verify(userRepo, times(1)).save(any());
    }

    @Test
    void updateUserRefreshTokenForUserWithExistentIdTest() {
        when(userRepo.updateUserRefreshToken("foo", userId)).thenReturn(1);
        int updatedRows = userService.updateUserRefreshToken("foo", userId);
        assertEquals(1, updatedRows);
    }

    @Test
    void getActivatedUsersAmountTest() {
        when(userRepo.countAllByUserStatus(ACTIVATED)).thenReturn(1L);
        long activatedUsersAmount = userService.getActivatedUsersAmount();
        assertEquals(1L, activatedUsersAmount);
    }

    @Test
    void getProfilePicturePathByUserIdNotFoundExceptionTest() {
        assertThrows(NotFoundException.class, () -> userService.getProfilePicturePathByUserId(1L));
    }

    @Test
    void getProfilePicturePathByUserIdTest() {
        when(userRepo.getProfilePicturePathByUserId(1L)).thenReturn(Optional.of(anyString()));
        userService.getProfilePicturePathByUserId(1L);
        verify(userRepo).getProfilePicturePathByUserId(1L);
    }

    @Test
    void updateUserProfilePictureNotUpdatedExceptionTest() {
        UserProfilePictureDto userProfilePictureDto = ModelUtils.getUserProfilePictureDto();
        userProfilePictureDto.setProfilePicturePath(null);
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(user));
        assertThrows(BadRequestException.class,
                () -> userService.updateUserProfilePicture(null, "testmail@gmail.com",
                        "test"));
    }

    @Test
    void geTUserProfileStatistics() {
        when(restClient.findAmountOfPublishedNews(TestConst.SIMPLE_LONG_NUMBER))
                .thenReturn(TestConst.SIMPLE_LONG_NUMBER);
        when(restClient.findAmountOfAcquiredHabits(TestConst.SIMPLE_LONG_NUMBER))
                .thenReturn(TestConst.SIMPLE_LONG_NUMBER);
        when(restClient.findAmountOfHabitsInProgress(TestConst.SIMPLE_LONG_NUMBER))
                .thenReturn(TestConst.SIMPLE_LONG_NUMBER);
        userService.getUserProfileStatistics(TestConst.SIMPLE_LONG_NUMBER, TestConst.EMAIL);
        assertEquals(ModelUtils.USER_PROFILE_STATISTICS_DTO,
                userService.getUserProfileStatistics(TestConst.SIMPLE_LONG_NUMBER, TestConst.EMAIL));
        assertNotEquals(ModelUtils.USER_PROFILE_STATISTICS_DTO,
                userService.getUserProfileStatistics(TestConst.SIMPLE_LONG_NUMBER_BAD_VALUE, TestConst.EMAIL));
    }

    @Test
    void searchBy() {
        Pageable pageable = PageRequest.of(1, 3);
        user.setUserCredo("credo");
        Page<User> userPages = new PageImpl<>(List.of(user, user, user), pageable, 3);
        when(userRepo.searchBy(pageable, "query"))
                .thenReturn(userPages);
        when(modelMapper.map(user, UserManagementDto.class)).thenReturn(ModelUtils.CREATE_USER_MANAGER_DTO);
        List<UserManagementDto> users = userPages.stream()
                .map(user -> modelMapper.map(user, UserManagementDto.class))
                .collect(Collectors.toList());
        PageableAdvancedDto<UserManagementDto> pageableAdvancedDto = new PageableAdvancedDto<>(
                users,
                userPages.getTotalElements(),
                userPages.getPageable().getPageNumber(),
                userPages.getTotalPages(),
                userPages.getNumber(),
                userPages.hasPrevious(),
                userPages.hasNext(),
                userPages.isFirst(),
                userPages.isLast());
        assertEquals(pageableAdvancedDto, userService.searchBy(pageable, "query"));
    }

    @Test
    void saveUserProfileTest() {
        var request = ModelUtils.getUserProfileDtoRequest();
        var user = ModelUtils.getUserWithoutSocialNetworks();
        when(userRepo.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
        when(userRepo.save(user)).thenReturn(user);
        assertEquals(UpdateConstants.SUCCESS_EN, userService.saveUserProfile(request, "test@gmail.com"));
        verify(userRepo).findByEmail("test@gmail.com");
        verify(userRepo).save(user);
    }

    @Test
    void saveUserProfileThrowWrongEmailExceptionTest() {
        var request = UserProfileDtoRequest.builder().build();
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        Exception thrown = assertThrows(WrongEmailException.class,
                () -> userService.saveUserProfile(request, "test@gmail.com"));
        assertEquals(ErrorMessage.USER_NOT_FOUND_BY_EMAIL + "test@gmail.com", thrown.getMessage());
        verify(userRepo).findByEmail(anyString());
    }

    @Test
    void getUserProfileInformationTest() {
        UserProfileDtoResponse response = new UserProfileDtoResponse();
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserProfileDtoResponse.class)).thenReturn(response);
        assertEquals(response, userService.getUserProfileInformation(1L));
    }

    @Test
    void getUserProfileInformationExceptionTest() {
        assertThrows(WrongIdException.class, () -> userService.getUserProfileInformation(null));
    }

    @Test
    void updateUserLastActivityTimeTest() {
        LocalDateTime currentTime = LocalDateTime.now();
        userService.updateUserLastActivityTime(userId, currentTime);
        verify(userRepo).updateUserLastActivityTime(userId, currentTime);
    }

    @Test
    void checkIfTheUserIsOnlineExceptionTest() {
        assertThrows(WrongIdException.class, () -> userService.checkIfTheUserIsOnline(null));
    }

    @Test
    void checkIfTheUserIsOnlineEqualsTrueTest() {
        ReflectionTestUtils.setField(userService, "timeAfterLastActivity", 300000);
        Timestamp userLastActivityTime = Timestamp.valueOf(LocalDateTime.now());
        User user = ModelUtils.getUser();

        when(userRepo.findById(anyLong())).thenReturn(Optional.of(user));
        when(userRepo.findLastActivityTimeById(anyLong())).thenReturn(Optional.of(userLastActivityTime));

        assertTrue(userService.checkIfTheUserIsOnline(1L));
    }

    @Test
    void checkIfTheUserIsOnlineEqualsFalseTest() {
        ReflectionTestUtils.setField(userService, "timeAfterLastActivity", 300000);
        LocalDateTime localDateTime = LocalDateTime.of(
                2015, Month.JULY, 29, 19, 30, 40);
        Timestamp userLastActivityTime = Timestamp.valueOf(localDateTime);
        User user = ModelUtils.getUser();

        when(userRepo.findById(anyLong())).thenReturn(Optional.of(user));
        when(userRepo.findLastActivityTimeById(anyLong())).thenReturn(Optional.empty());

        assertFalse(userService.checkIfTheUserIsOnline(1L));
    }

    @Test
    void findUserForManagementByPage() {
        int pageNumber = 5;
        int pageSize = 20;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        List<User> userList = Collections.singletonList(ModelUtils.getUser());
        Page<User> users = new PageImpl<>(userList, pageable, userList.size());
        List<UserManagementDto> userManagementDtos =
                users.getContent().stream()
                        .map(user -> modelMapper.map(user, UserManagementDto.class))
                        .collect(Collectors.toList());
        PageableAdvancedDto<UserManagementDto> userManagementDtoPageableDto = new PageableAdvancedDto<>(
                userManagementDtos,
                users.getTotalElements(),
                users.getPageable().getPageNumber(),
                users.getTotalPages(),
                users.getNumber(),
                users.hasPrevious(),
                users.hasNext(),
                users.isFirst(),
                users.isLast());
        when(userRepo.findAll(pageable)).thenReturn(users);
        assertEquals(userManagementDtoPageableDto, userService.findUserForManagementByPage(pageable));
    }

    @Test
    void updateUser() {
        UserManagementUpdateDto userManagementUpdateDto = ModelUtils.getUserManagementUpdateDto();
        User excepted = user;
        excepted.setName(userManagementUpdateDto.getName());
        excepted.setEmail(userManagementUpdateDto.getEmail());
        excepted.setRole(userManagementUpdateDto.getRole());
        excepted.setUserCredo(userManagementUpdateDto.getUserCredo());
        excepted.setUserStatus(userManagementUpdateDto.getUserStatus());
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserVO.class)).thenReturn(userVO);
        userService.updateUser(1L, userManagementUpdateDto);
        assertEquals(excepted, user);
    }

    @Test
    void findNotDeactivatedByEmail() {
        String email = "test@gmail.com";
        user.setEmail(email);
        when(userRepo.findNotDeactivatedByEmail(email)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserVO.class)).thenReturn(userVO);
        assertEquals(Optional.of(userVO), userService.findNotDeactivatedByEmail(email));
    }

    @Test
    void findNotDeactivatedByEmailShouldThrowNotFoundException() {
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.empty());

        Exception thrown = assertThrows(NotFoundException.class,
                () -> userService.findNotDeactivatedByEmail("test@gmail.com"));

        assertEquals(ErrorMessage.USER_NOT_FOUND_BY_EMAIL, thrown.getMessage());
    }

    @Test
    void deactivateUser() {
        List<String> test = List.of();
        User user = ModelUtils.getUser();
        user.setLanguage(Language.builder()
                .id(1L)
                .code("en")
                .build());
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        user.setUserStatus(DEACTIVATED);
        when(userRepo.save(user)).thenReturn(user);
        UserDeactivationReason userReason = UserDeactivationReason.builder()
                .dateTimeOfDeactivation(LocalDateTime.now())
                .reason("test")
                .user(user)
                .build();
        when(userDeactivationRepo.save(userReason)).thenReturn(userReason);
        assertEquals(UserDeactivationReasonDto.builder()
                .email(user.getEmail())
                .name(user.getName())
                .deactivationReasons(test)
                .lang(user.getLanguage().getCode())
                .build(), userService.deactivateUser(1L, test));
    }

    @Test
    void getDeactivationReason() {
        List<String> test1 = List.of();
        User user = ModelUtils.getUser();
        user.setLanguage(Language.builder()
                .id(1L)
                .code("en")
                .build());
        UserDeactivationReason test = UserDeactivationReason.builder()
                .id(1L)
                .user(user)
                .reason("test")
                .dateTimeOfDeactivation(LocalDateTime.now())
                .build();
        when(userDeactivationRepo.getLastDeactivationReasons(1L)).thenReturn(Optional.of(test));
        assertEquals(test1, userService.getDeactivationReason(1L, "en"));
        assertEquals(test1, userService.getDeactivationReason(1L, "ua"));
    }

    @Test
    void deactivateAllUsers() {
        List<Long> longList = List.of(1L, 2L);
        assertEquals(longList, userService.deactivateAllUsers(longList));
    }

    @Test
    void setActivatedStatus() {
        User user = ModelUtils.getUser();
        user.setLanguage(Language.builder()
                .id(1L)
                .code("en")
                .build());
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        user.setUserStatus(ACTIVATED);
        when(userRepo.save(user)).thenReturn(user);
        assertEquals(UserActivationDto.builder()
                .email(user.getEmail())
                .name(user.getName())
                .lang(user.getLanguage().getCode())
                .build(), userService.setActivatedStatus(userId));
    }

    @Test
    void updateUserLanguage() {
        Language language = ModelUtils.getLanguage();
        User user = ModelUtils.getUser();
        user.setLanguage(language);

        when(languageRepo.findById(1L)).thenReturn(Optional.of(language));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(userRepo.save(user)).thenReturn(user);
        userService.updateUserLanguage(1L, 1L);
        verify(userRepo).save(user);
    }

    @Test
    void updateUserLanguageNotFoundExeption() {
        Language language = ModelUtils.getLanguage();
        User user = ModelUtils.getUser();
        user.setLanguage(language);

        when(languageRepo.findById(10L)).thenThrow(NotFoundException.class);
        assertThrows(NotFoundException.class, () -> userService.updateUserLanguage(1L, 10L));
    }

    @Test
    void updateUserLanguageUserNotFoundExeption() {
        Language language = ModelUtils.getLanguage();
        User user = ModelUtils.getUser();
        user.setLanguage(language);

        when(languageRepo.findById(1L)).thenReturn(Optional.of(language));
        when(userRepo.findById(1L)).thenThrow(NotFoundException.class);
        assertThrows(NotFoundException.class, () -> userService.updateUserLanguage(1L, 1L));
    }

    @Test
    void findByIdAndToken() {
        Language defaultLanguage = new Language();
        defaultLanguage.setId(1L);
        defaultLanguage.setCode("ua");
        when(languageRepo.findById(1L)).thenReturn(Optional.of(defaultLanguage));

        VerifyEmail verifyEmail = new VerifyEmail();
        verifyEmail.setId(2L);
        verifyEmail.setExpiryDate(LocalDateTime.now());
        verifyEmail.setToken("test");
        verifyEmail.setUser(user2);
        user2.setVerifyEmail(verifyEmail);

        when(userRepo.findById(userId2)).thenReturn(Optional.of(user2));
        when(modelMapper.map(Optional.of(user2), UserVO.class)).thenReturn(userVO2);
        when(modelMapper.map(userVO2, User.class)).thenReturn(user2);
        when(modelMapper.map(user2, UserVO.class)).thenReturn(userVO2);

        assertEquals(Optional.of(userVO2), userService.findByIdAndToken(userId2, "test"));
    }

    @Test
    void findByIdAndToken2() {
        Language defaultLanguage = new Language();
        defaultLanguage.setId(1L);
        defaultLanguage.setCode("ua");
        when(languageRepo.findById(1L)).thenReturn(Optional.of(defaultLanguage));

        when(userRepo.findById(userId2)).thenReturn(Optional.of(user2));
        when(modelMapper.map(Optional.of(user2), UserVO.class)).thenReturn(userVO2);
        when(modelMapper.map(userVO2, User.class)).thenReturn(user2);
        when(modelMapper.map(user2, UserVO.class)).thenReturn(userVO2);
        assertEquals(Optional.empty(), userService.findByIdAndToken(userId2, "test"));
    }

    @Test
    void getAvailableCustomShoppingListItem() {
        CustomShoppingListItemResponseDto customShoppingListItemResponseDto =
                new CustomShoppingListItemResponseDto(1L, "test");
        when(restClient.getAllAvailableCustomShoppingListItems(userId, habitId))
                .thenReturn(Collections.singletonList(customShoppingListItemResponseDto));

        assertEquals(Collections.singletonList(customShoppingListItemResponseDto),
                userService.getAvailableCustomShoppingListItems(userId, habitId));
    }

    @Test
    void searchTest() {
        Pageable pageable = PageRequest.of(0, 20);
        UserManagementViewDto userViewDto =
                UserManagementViewDto.builder()
                        .id("1L")
                        .name("vivo")
                        .email("test@ukr.net")
                        .userCredo("Hello")
                        .role("1")
                        .userStatus("1")
                        .build();
        UserManagementVO userManagementVO =
                UserManagementVO.builder()
                        .id(1L)
                        .name("vivo")
                        .email("test@ukr.net")
                        .userCredo("Hello")
                        .role(ROLE_USER)
                        .userStatus(ACTIVATED)
                        .build();
        List<UserManagementVO> userManagementVOS = Collections.singletonList(userManagementVO);
        List<User> users = Collections.singletonList(new User());
        Page<User> pageUsers = new PageImpl<>(users, pageable, 0);
        when(userRepo.findAll(any(UserSpecification.class), eq(pageable))).thenReturn(pageUsers);
        when(modelMapper.map(users.get(0), UserManagementVO.class)).thenReturn(userManagementVO);
        PageableAdvancedDto<UserManagementVO> actual = new PageableAdvancedDto<>(userManagementVOS, 1, 0, 1, 0,
                false, false, true, true);
        PageableAdvancedDto<UserManagementVO> expected = userService.search(pageable, userViewDto);
        assertEquals(expected, actual);
    }

    @Test
    void createUbsRecordTest() {
        Long id = 1L;
        User user = new User();
        user.setId(1L);
        when(userRepo.findById(id)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserVO.class)).thenReturn(userVO);
        UbsTableCreationDto actual = UbsTableCreationDto.builder().uuid(user.getUuid()).build();
        assertEquals(actual, userService.createUbsRecord(userVO));
    }

    @Test
    void createUbsRecordThrowNotFoundExceptionTest() {
        when(userRepo.findById(1L)).thenReturn(Optional.empty());
        Exception thrown = assertThrows(NotFoundException.class,
                () -> userService.createUbsRecord(userVO));
        assertEquals(ErrorMessage.USER_NOT_FOUND_BY_ID, thrown.getMessage());
        verify(userRepo).findById(1L);
    }

    @Test
    void deleteUserProfilePictureTest() {
        String email = "test@gmail.com";
        String picture = "picture";
        User user = new User();
        user.setEmail(email);
        user.setProfilePicturePath(picture);
        when(userRepo.findByEmail(email)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserVO.class)).thenReturn(userVO);
        userService.deleteUserProfilePicture(email);
        assertNull(user.getProfilePicturePath());
    }

    @Test
    void findAdminByIdTest() {
        when(userRepo.findById(2L)).thenReturn(Optional.ofNullable(TEST_ADMIN));
        when(modelMapper.map(TEST_ADMIN, UserVO.class)).thenReturn(TEST_USER_VO);

        UserVO actual = userService.findAdminById(2L);

        assertEquals(TEST_USER_VO, actual);
    }

    @Test
    void findAdminByIdThrowsExceptionTest() {
        when(userRepo.findById(2L)).thenReturn(Optional.ofNullable(TEST_USER));

        assertThrows(LowRoleLevelException.class,
                () -> userService.findAdminById(2L));
    }

    private static Stream<Arguments> provideUuidOptionalUserResultForCheckIfUserExistsByUuidTest() {
        return Stream.of(
                Arguments.of("444e66e8-8daa-4cb0-8269-a8d856e7dd15", Optional.of(getUser()), true),
                Arguments.of("uuid", Optional.empty(), false));
    }
}
