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
    private String id;
    private String name;
    private String email;
    private String userCredo;
    private String role;
    private String userStatus;
}
