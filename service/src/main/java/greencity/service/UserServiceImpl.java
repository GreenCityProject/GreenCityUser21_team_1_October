package greencity.service;

import greencity.constant.UpdateConstants;
import greencity.dto.ubs.UbsTableCreationDto;
import greencity.dto.user.*;
import greencity.entity.Language;
import greencity.entity.UserDeactivationReason;
import greencity.filters.SearchCriteria;
import greencity.client.RestClient;
import greencity.constant.ErrorMessage;
import greencity.constant.LogMessage;
import greencity.dto.PageableAdvancedDto;
import greencity.dto.PageableDto;
import greencity.dto.filter.FilterUserDto;
import greencity.dto.shoppinglist.CustomShoppingListItemResponseDto;
import greencity.entity.User;
import greencity.entity.VerifyEmail;
import greencity.enums.EmailNotification;
import greencity.enums.Role;
import greencity.enums.UserStatus;
import greencity.exception.exceptions.*;
import greencity.filters.UserSpecification;
import greencity.repository.LanguageRepo;
import greencity.repository.UserDeactivationRepo;
import greencity.repository.UserRepo;
import greencity.repository.options.UserFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The class provides implementation of the {@code UserService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    /**
     * Autowired greencity.repository.
     */
    private final UserRepo userRepo;
    private final RestClient restClient;
    private final LanguageRepo languageRepo;
    private final UserDeactivationRepo userDeactivationRepo;
    /**
     * Autowired mapper.
     */
    private final ModelMapper modelMapper;
    @Value("${greencity.time.after.last.activity}")
    private long timeAfterLastActivity;

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public UserVO save(UserVO userVO) {
        if (userVO.getId() != null && userRepo.existsById(userVO.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User with id " + userVO.getId() + " already exists.");
        }
        User user = modelMapper.map(userVO, User.class);
        return modelMapper.map(userRepo.save(user), UserVO.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserVO findById(Long id) {
        User user = userRepo.findById(id)
            .orElseThrow(() -> new WrongIdException(ErrorMessage.USER_NOT_FOUND_BY_ID + id));
        if (user.getLanguage() == null) {
            Language defaultLanguage = languageRepo.findById(1L)
                    .orElseThrow(() -> new NotFoundException(ErrorMessage.LANGUAGE_NOT_FOUND_BY_ID + 1L));
            user.setLanguage(defaultLanguage);
        }
        return modelMapper.map(user, UserVO.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageableDto<UserForListDto> findByPage(Pageable pageable) {
        Page<User> users = userRepo.findAll(pageable);
        List<UserForListDto> userForListDtos =
            users.getContent().stream()
                .map(user -> modelMapper.map(user, UserForListDto.class))
                .collect(Collectors.toList());
        return new PageableDto<>(
            userForListDtos,
            users.getTotalElements(),
            users.getPageable().getPageNumber(),
            users.getTotalPages());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageableAdvancedDto<UserManagementDto> findUserForManagementByPage(Pageable pageable) {
        Page<User> users = userRepo.findAll(pageable);
        List<UserManagementDto> userManagementDtos =
            users.getContent().stream()
                .map(user -> modelMapper.map(user, UserManagementDto.class))
                .collect(Collectors.toList());
        return new PageableAdvancedDto<>(
            userManagementDtos,
            users.getTotalElements(),
            users.getPageable().getPageNumber(),
            users.getTotalPages(),
            users.getNumber(),
            users.hasPrevious(),
            users.hasNext(),
            users.isFirst(),
            users.isLast());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void updateUser(Long userId, UserManagementUpdateDto dto) {
        User user = findUserById(userId);
        updateUserFromDto(dto, user);
    }

    /**
     * Method for setting data from {@link UserManagementDto} to {@link UserVO}.
     *
     * @param dto  - dto {@link UserManagementDto} with updated fields.
     * @param user {@link UserVO} to be updated.
     * @author Vasyl Zhovnir
     */
    private void updateUserFromDto(UserManagementUpdateDto dto, User user) {
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        user.setUserCredo(dto.getUserCredo());
        user.setUserStatus(dto.getUserStatus());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteById(Long id) {
        UserVO userVO = findById(id);
        userRepo.delete(modelMapper.map(userVO, User.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserVO findByEmail(String email) {
        Optional<User> optionalUser = userRepo.findByEmail(email);
        return optionalUser.isEmpty() ? null : modelMapper.map(optionalUser.get(), UserVO.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserVO> findAll() {
        return modelMapper.map(userRepo.findAll(), new TypeToken<List<UserVO>>() {
        }.getType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageableAdvancedDto<UserManagementVO> search(Pageable pageable,
        UserManagementViewDto userManagementViewDto) {
        Page<User> found = userRepo.findAll(buildSpecification(userManagementViewDto), pageable);

        if (found.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No users found for the specified criteria.");
        }
        return buildPageableAdvanceDtoFromPage(found);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UbsTableCreationDto createUbsRecord(UserVO currentUser) {
        User user = userRepo.findById(currentUser.getId()).orElseThrow(
            () -> new NotFoundException(ErrorMessage.USER_NOT_FOUND_BY_ID));
        String uuid = user.getUuid();

        return UbsTableCreationDto.builder().uuid(uuid).build();
    }

    /**
     * {@inheritDoc}
     */
    private PageableAdvancedDto<UserManagementVO> buildPageableAdvanceDtoFromPage(Page<User> pageTags) {
        List<UserManagementVO> usersVOs = pageTags.getContent().stream()
            .map(t -> modelMapper.map(t, UserManagementVO.class))
            .collect(Collectors.toList());

        return new PageableAdvancedDto<>(
            usersVOs,
            pageTags.getTotalElements(), pageTags.getPageable().getPageNumber(),
            pageTags.getTotalPages(), pageTags.getNumber(),
            pageTags.hasPrevious(), pageTags.hasNext(),
            pageTags.isFirst(), pageTags.isLast());
    }

    /**
     * {@inheritDoc}
     */
    private UserSpecification buildSpecification(UserManagementViewDto userViewDto) {
        List<SearchCriteria> searchCriteriaList = buildSearchCriteriaList(userViewDto);

        return new UserSpecification(searchCriteriaList);
    }

    /**
     * {@inheritDoc}
     */
    private List<SearchCriteria> buildSearchCriteriaList(UserManagementViewDto userViewDto) {
        List<SearchCriteria> searchCriteriaList = new ArrayList<>();
        setValueIfNotEmpty(searchCriteriaList, "id", userViewDto.getId());
        setValueIfNotEmpty(searchCriteriaList, "name", userViewDto.getName());
        setValueIfNotEmpty(searchCriteriaList, "email", userViewDto.getEmail());
        setValueIfNotEmpty(searchCriteriaList, "userCredo", userViewDto.getUserCredo());
        setValueIfNotEmpty(searchCriteriaList, "role", userViewDto.getRole());
        setValueIfNotEmpty(searchCriteriaList, "userStatus", userViewDto.getUserStatus());
        return searchCriteriaList;
    }

    /**
     * {@inheritDoc}
     */
    private void setValueIfNotEmpty(List<SearchCriteria> searchCriteria, String key, String value) {
        if (!StringUtils.isEmpty(value)) {
            searchCriteria.add(SearchCriteria.builder()
                .key(key)
                .type(key)
                .value(value)
                .build());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Optional<UserVO> findNotDeactivatedByEmail(String email) {
        log.info("email {}", email);
        User notDeactivatedByEmail = userRepo.findNotDeactivatedByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND_BY_EMAIL));
        log.info("user: {}", notDeactivatedByEmail);
        return Optional.of(modelMapper.map(notDeactivatedByEmail, UserVO.class));
    }

    /**
     * {@inheritDoc}
     *
     * @author Zakhar Skaletskyi
     */
    @Override
    public Long findIdByEmail(String email) {
        log.info(LogMessage.IN_FIND_ID_BY_EMAIL, email);
        return userRepo.findIdByEmail(email).orElseThrow(
            () -> new WrongEmailException(ErrorMessage.USER_NOT_FOUND_BY_EMAIL));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String findUuIdByEmail(String email) {
        log.info(LogMessage.IN_FIND_UUID_BY_EMAIL, email);
        return userRepo.findUuidByEmail(email).orElseThrow(
            () -> new WrongEmailException(ErrorMessage.USER_NOT_FOUND_BY_EMAIL));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserRoleDto updateRole(Long id, Role role, String email) {
        User user = findUserById(id);
        checkIfUserCanUpdate(user, email);
        user.setRole(role);
        return modelMapper.map(user, UserRoleDto.class);
    }

    private User findUserById(Long id) {
        return userRepo.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND_BY_ID));
    }

    private void checkIfUserCanUpdate(User user, String email) {
        if (email.equals(user.getEmail())) {
            throw new BadUpdateRequestException(ErrorMessage.USER_CANT_UPDATE_THEMSELVES);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserStatusDto updateStatus(Long id, UserStatus userStatus, String email) {
        checkUpdatableUser(id, email);
        accessForUpdateUserStatus(id, email);
        UserVO userVO = findById(id);
        userVO.setUserStatus(userStatus);
        User map = modelMapper.map(userVO, User.class);
        return modelMapper.map(userRepo.save(map), UserStatusDto.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoleDto getRoles() {
        return new RoleDto(Role.class.getEnumConstants());
    }

    /**
     * {@inheritDoc}
     *
     * @author Nazar Vladyka
     */
    @Override
    public List<EmailNotification> getEmailNotificationsStatuses() {
        return Arrays.asList(EmailNotification.class.getEnumConstants());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserVO updateLastVisit(UserVO userVO) {
        UserVO user = findById(userVO.getId());
        log.info(user.getLastActivityTime() + "s");
        userVO.setLastActivityTime(LocalDateTime.now());
        User updatable = modelMapper.map(userVO, User.class);
        return modelMapper.map(userRepo.save(updatable), UserVO.class);
    }

    /**
     * {@inheritDoc}
     */
    public PageableDto<UserForListDto> getUsersByFilter(FilterUserDto filterUserDto, Pageable pageable) {
        Page<User> users = userRepo.findAll(new UserFilter(filterUserDto), pageable);
        List<UserForListDto> userForListDtos =
            users.getContent().stream()
                .map(user -> modelMapper.map(user, UserForListDto.class))
                .collect(Collectors.toList());
        return new PageableDto<>(
            userForListDtos,
            users.getTotalElements(),
            users.getPageable().getPageNumber(),
            users.getTotalPages());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserUpdateDto getUserUpdateDtoByEmail(String email) {
        UserVO user = findByEmail(email);

        if (user.getRole() == Role.ROLE_USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access is denied");
        }

        User userEntity = userRepo.findByEmail(email)
                .orElseThrow(() -> new WrongEmailException(ErrorMessage.USER_NOT_FOUND_BY_EMAIL + email));

        return modelMapper.map(userEntity, UserUpdateDto.class);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public UserUpdateDto update(UserUpdateDto dto, String email) {
        User user = userRepo
            .findByEmail(email)
            .orElseThrow(() -> new WrongEmailException(ErrorMessage.USER_NOT_FOUND_BY_EMAIL + email));
        user.setName(dto.getName());
        user.setEmailNotification(dto.getEmailNotification());
        userRepo.save(user);
        return dto;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int updateUserRefreshToken(String refreshTokenKey, Long id) {
        return userRepo.updateUserRefreshToken(refreshTokenKey, id);
    }

    /**
     * Method which check that, if admin/moderator update role/status of himself,
     * then throw exception.
     *
     * @param id    id of updatable user.
     * @param email email of admin/moderator.
     * @author Rostyslav Khasanov
     */
    private void checkUpdatableUser(Long id, String email) {
        UserVO user = findByEmail(email);
        if (id.equals(user.getId())) {
            throw new BadUpdateRequestException(ErrorMessage.USER_CANT_UPDATE_THEMSELVES);
        }
    }

    /**
     * Method which check that, if moderator trying update status of admins or
     * moderators, then throw exception.
     *
     * @param id    id of updatable user.
     * @param email email of admin/moderator.
     * @author Rostyslav Khasanov
     */
    private void accessForUpdateUserStatus(Long id, String email) {
        UserVO user = findByEmail(email);
        if (user.getRole() == Role.ROLE_MODERATOR) {
            Role role = findById(id).getRole();
            if ((role == Role.ROLE_MODERATOR) || (role == Role.ROLE_ADMIN)) {
                throw new LowRoleLevelException(ErrorMessage.IMPOSSIBLE_UPDATE_USER_STATUS);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @author Bogdan Kuzenko
     */
    @Transactional
    @Override
    public List<CustomShoppingListItemResponseDto> getAvailableCustomShoppingListItems(Long userId, Long habitId) {
        return restClient.getAllAvailableCustomShoppingListItems(userId, habitId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getActivatedUsersAmount() {
        return userRepo.countAllByUserStatus(UserStatus.ACTIVATED);
    }

    /**
     * Get profile picture path {@link String}.
     *
     * @return profile picture path {@link String}
     */
    @Override
    public String getProfilePicturePathByUserId(Long id) {
        return userRepo
            .getProfilePicturePathByUserId(id)
            .orElseThrow(() -> new NotFoundException(ErrorMessage.PROFILE_PICTURE_NOT_FOUND_BY_ID + id.toString()));
    }

    /**
     * Update user profile picture {@link UserVO}.
     *
     * @param image  {@link MultipartFile}
     * @param email  {@link String} - email of user that need to update.
     * @param base64 {@link String} - picture in base 64 format.
     * @return {@link UserVO}.
     * @author Marian Datsko
     */
    @Override
    public UserVO updateUserProfilePicture(MultipartFile image, String email,
        String base64) {
        User user = userRepo
            .findByEmail(email)
            .orElseThrow(() -> new WrongEmailException(ErrorMessage.USER_NOT_FOUND_BY_EMAIL + email));
        if (base64 != null) {
            image = modelMapper.map(base64, MultipartFile.class);
        }
        if (image != null) {
            String profilePicturePath;
            profilePicturePath = restClient.uploadImage(image);
            user.setProfilePicturePath(profilePicturePath);
        } else {
            throw new BadRequestException(ErrorMessage.IMAGE_EXISTS);
        }
        return modelMapper.map(userRepo.save(user), UserVO.class);
    }

    /**
     * Delete user profile picture {@link UserVO}.
     *
     * @param email {@link String} - email of user that need to update.
     */
    @Override
    public void deleteUserProfilePicture(String email) {
        User user = userRepo
            .findByEmail(email)
            .orElseThrow(() -> new WrongEmailException(ErrorMessage.USER_NOT_FOUND_BY_EMAIL + email));
        user.setProfilePicturePath(null);
        userRepo.save(user);
    }

    private PageableDto<UserProfilePictureDto> getPageableDto(
        List<UserProfilePictureDto> userProfilePictureDtoList, Page<User> pageUsers) {
        return new PageableDto<>(
            userProfilePictureDtoList,
            pageUsers.getTotalElements(),
            pageUsers.getPageable().getPageNumber(),
            pageUsers.getTotalPages());
    }

    /**
     * Save user profile information {@link UserVO}.
     *
     * @author Marian Datsko
     */
    @Override
    public String saveUserProfile(UserProfileDtoRequest userProfileDtoRequest, String email) {
        User user = userRepo
            .findByEmail(email)
            .orElseThrow(() -> new WrongEmailException(ErrorMessage.USER_NOT_FOUND_BY_EMAIL + email));
        user.setName(userProfileDtoRequest.getName());
        user.setCity(userProfileDtoRequest.getCity());
        user.setUserCredo(userProfileDtoRequest.getUserCredo());
        user.setShowLocation(userProfileDtoRequest.getShowLocation());
        user.setShowEcoPlace(userProfileDtoRequest.getShowEcoPlace());
        user.setShowShoppingList(userProfileDtoRequest.getShowShoppingList());
        userRepo.save(user);
        return UpdateConstants.getResultByLanguageCode(user.getLanguage().getCode());
    }

    /**
     * Method return user profile information {@link UserVO}.
     *
     * @author Marian Datsko
     */
    @Override
    public UserProfileDtoResponse getUserProfileInformation(Long userId) {
        User user = userRepo
            .findById(userId)
            .orElseThrow(() -> new WrongIdException(ErrorMessage.USER_NOT_FOUND_BY_ID + userId));
        return modelMapper.map(user, UserProfileDtoResponse.class);
    }

    /**
     * Updates last activity time for a given user.
     *
     * @param userId               - {@link UserVO}'s id
     * @param userLastActivityTime - new {@link UserVO}'s last activity time
     * @author Yurii Zhurakovskyi
     */
    @Override
    public void updateUserLastActivityTime(Long userId, LocalDateTime userLastActivityTime) {
        userRepo.updateUserLastActivityTime(userId, userLastActivityTime);
    }

    /**
     * The method checks by id if a {@link UserVO} is online.
     *
     * @param userId {@link Long}
     * @return {@link Boolean}.
     * @author Yurii Zhurakovskyi
     */
    @Override
    public boolean checkIfTheUserIsOnline(Long userId) {
        if (userRepo.findById(userId).isEmpty()) {
            throw new WrongIdException(ErrorMessage.USER_NOT_FOUND_BY_ID + userId);
        }
        Optional<Timestamp> lastActivityTime = userRepo.findLastActivityTimeById(userId);
        if (lastActivityTime.isPresent()) {
            LocalDateTime userLastActivityTime = lastActivityTime.get().toLocalDateTime();
            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime lastActivityTimeZDT = ZonedDateTime.of(userLastActivityTime, ZoneId.systemDefault());
            long result = now.toInstant().toEpochMilli() - lastActivityTimeZDT.toInstant().toEpochMilli();
            return result <= timeAfterLastActivity;
        }
        return false;
    }

    /**
     * Method return user profile statistics {@link UserVO}.
     *
     * @param userId - {@link UserVO}'s id
     * @author Marian Datsko
     */
    @Override
    public UserProfileStatisticsDto getUserProfileStatistics(Long userId, String email) {
        var currentUserID = userRepo.findUserIdByEmail(email);
        if (currentUserID.isPresent()&&currentUserID.get()==userId){
        Long amountOfPublishedNewsByUserId = restClient.findAmountOfPublishedNews(userId);
        Long amountOfAcquiredHabitsByUserId = restClient.findAmountOfAcquiredHabits(userId);
        Long amountOfHabitsInProgressByUserId = restClient.findAmountOfHabitsInProgress(userId);

        return UserProfileStatisticsDto.builder()
            .amountPublishedNews(amountOfPublishedNewsByUserId)
            .amountHabitsAcquired(amountOfAcquiredHabitsByUserId)
            .amountHabitsInProgress(amountOfHabitsInProgressByUserId)
            .build();
        } else {
            throw new AccessDeniedException(ErrorMessage.USER_DOESNT_HAVE_ACCESS_TO_DATA
                    +" Requested data of user with id:"+userId);
        }

    }

    @Override
    public UserDeactivationReasonDto deactivateUser(Long id, List<String> userReasons) {
        User foundUser =
            userRepo.findById(id).orElseThrow(() -> new WrongIdException(ErrorMessage.USER_NOT_FOUND_BY_ID + id));
        foundUser.setUserStatus(UserStatus.DEACTIVATED);
        userRepo.save(foundUser);
        String reasons = userReasons.stream().map(Object::toString).collect(Collectors.joining("/"));
        userDeactivationRepo.save(UserDeactivationReason.builder()
            .dateTimeOfDeactivation(LocalDateTime.now())
            .reason(reasons)
            .user(foundUser)
            .build());
        return UserDeactivationReasonDto.builder()
            .email(foundUser.getEmail())
            .name(foundUser.getName())
            .deactivationReasons(filterReasons(foundUser.getLanguage().getCode(), reasons))
            .lang(foundUser.getLanguage().getCode())
            .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getDeactivationReason(Long id, String adminLang) {
        UserDeactivationReason userReason = userDeactivationRepo.getLastDeactivationReasons(id)
            .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_DEACTIVATION_REASON_IS_EMPTY));
        if (adminLang.equals("uk")) {
            adminLang = "ua";
        }
        return filterReasons(adminLang,
            userReason.getReason());
    }

    private List<String> filterReasons(String lang, String reasons) {
        List<String> result = null;
        List<String> forAll = List.of(reasons.split("/"));
        if (lang.equals("en")) {
            result = forAll.stream().filter(s -> s.contains("{en}"))
                .map(filterEn -> filterEn.replace("{en}", "").trim()).collect(Collectors.toList());
        }
        if (lang.equals("ua")) {
            result = forAll.stream().filter(s -> s.contains("{ua}"))
                .map(filterEn -> filterEn.replace("{ua}", "").trim()).collect(Collectors.toList());
        }
        return result;
    }

    @Transactional
    @Override
    public UserActivationDto setActivatedStatus(Long id) {
        User foundUser =
            userRepo.findById(id).orElseThrow(() -> new WrongIdException(ErrorMessage.USER_NOT_FOUND_BY_ID + id));
        foundUser.setUserStatus(UserStatus.ACTIVATED);
        userRepo.save(foundUser);
        return UserActivationDto.builder()
            .email(foundUser.getEmail())
            .name(foundUser.getName())
            .lang(foundUser.getLanguage().getCode())
            .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateUserLanguage(Long userId, Long languageId) {
        Language language = languageRepo.findById(languageId)
            .orElseThrow(() -> new NotFoundException(ErrorMessage.LANGUAGE_NOT_FOUND_BY_ID + languageId));
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND_BY_ID + userId));
        user.setLanguage(language);
        userRepo.save(user);
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public List<Long> deactivateAllUsers(List<Long> listId) {
        userRepo.deactivateSelectedUsers(listId);
        return listId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UserVO> findByIdAndToken(Long userId, String token) {
        User foundUser = modelMapper.map(findById(userId), User.class);

        VerifyEmail verifyEmail = foundUser.getVerifyEmail();
        if (verifyEmail != null && verifyEmail.getToken().equals(token)) {
            return Optional.of(modelMapper.map(foundUser, UserVO.class));
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageableAdvancedDto<UserManagementDto> searchBy(Pageable paging, String query) {
        Page<User> page = userRepo.searchBy(paging, query);
        List<UserManagementDto> users = page.stream()
            .map(user -> modelMapper.map(user, UserManagementDto.class))
            .collect(Collectors.toList());
        return new PageableAdvancedDto<>(
            users,
            page.getTotalElements(),
            page.getPageable().getPageNumber(),
            page.getTotalPages(),
            page.getNumber(),
            page.hasPrevious(),
            page.hasNext(),
            page.isFirst(),
            page.isLast());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserVO> findAllByEmailNotification(EmailNotification emailNotification) {
        return userRepo.findAllByEmailNotification(emailNotification).stream()
            .map(user -> modelMapper.map(user, UserVO.class))
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int scheduleDeleteDeactivatedUsers() {
        return userRepo.scheduleDeleteDeactivatedUsers();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> findAllUsersCities() {
        return userRepo.findAllUsersCities();
    }

    @Override
    public UserVO findAdminById(Long id) {
        User user = userRepo.findById(id)
            .orElseThrow(() -> new WrongIdException(ErrorMessage.USER_NOT_FOUND_BY_ID));

        boolean isAdmin = user.getRole().equals(Role.ROLE_ADMIN);

        if (isAdmin) {
            return modelMapper.map(user, UserVO.class);
        }

        throw new LowRoleLevelException("You do not have authorities");
    }

    @Override
    public boolean existsUserByEmail(String email) {
        return userRepo.existsUserByEmail(email);
    }
}
