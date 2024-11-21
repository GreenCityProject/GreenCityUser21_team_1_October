package greencity.security.service;

import greencity.security.dto.SuccessSignInDto;

public interface GoogleAuthService {
    SuccessSignInDto authGoogle(String token, String language);
}