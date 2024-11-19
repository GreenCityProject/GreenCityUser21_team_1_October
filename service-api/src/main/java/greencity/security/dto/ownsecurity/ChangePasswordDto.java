package greencity.security.dto.ownsecurity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ChangePasswordDto {
    @NotBlank(message = "Current password must not be blank")
    private String currentPassword;

    @NotBlank(message = "New password must not be blank")
    @Size(min = 8, message = "New password must be at least 8 characters long")
    @Pattern(regexp = ".*\\d.*", message = "New password must contain at least one digit")
    @Pattern(regexp = ".*[a-z].*", message = "New password must contain at least one lowercase letter")
    @Pattern(regexp = ".*[A-Z].*", message = "New password must contain at least one uppercase letter")
    @Pattern(regexp = ".*[!@#$%^&*(),.?\":{}|<>].*", message = "New password must contain at least one special character")
    private String newPassword;

    @NotBlank(message = "Confirm password must not be blank")
    private String confirmPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
