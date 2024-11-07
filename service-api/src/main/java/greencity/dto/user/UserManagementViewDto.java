package greencity.dto.user;

import greencity.enums.Role;
import greencity.enums.UserStatus;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Builder
public class UserManagementViewDto {
    private Long id;
    private String name;
    private String email;
    private String userCredo;
    private Role role;
    private UserStatus userStatus;
}
