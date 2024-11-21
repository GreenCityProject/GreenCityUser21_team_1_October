package greencity.security.controller;

import greencity.annotations.ApiLocale;
import greencity.annotations.ValidLanguage;
import greencity.constant.HttpStatuses;
import greencity.security.dto.SuccessSignInDto;
import greencity.security.service.GoogleAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Locale;
import static greencity.constant.ErrorMessage.INVALID_GOOGLE_TOKEN;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/googleAuth")
public class GoogleAuthController {
    private final GoogleAuthService googleSecurityService;

    @Operation(summary = "Perform authentication by Google")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = HttpStatuses.OK,
                    content = @Content(schema = @Schema(implementation = SuccessSignInDto.class))),
            @ApiResponse(responseCode = "400", description = INVALID_GOOGLE_TOKEN)
    })
    @GetMapping("/getToken")
    @ApiLocale
    public SuccessSignInDto authenticate(@RequestParam @NotBlank String token,
                                         @Parameter(hidden = true) @ValidLanguage Locale locale) {
        return googleSecurityService.authGoogle(token, locale.getLanguage());
    }
}