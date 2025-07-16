package MIOSM.auth_service.service;

import MIOSM.auth_service.dto.*;

import java.util.UUID;

public interface AuthService {

    UUID register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void logout(LogoutRequest request);

    UserInfoResponse getMe(String accessToken);

    void createProfile(UUID userId, String username, String bio);

    LoginResponse refreshToken(String refreshToken);
}
