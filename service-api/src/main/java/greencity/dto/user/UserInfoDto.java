package greencity.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfoDto {
    private String sub;

    private String name;

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("email_verified")
    private String emailVerified;

    private String locale;

    private String error;

    @JsonProperty("error_description")
    private String errorDescription;

    @JsonProperty("family_name")
    private String familyName;

    private String picture;

    private String email;
}