package MIOSM.auth_service.service;

import MIOSM.auth_service.dto.*;

import java.util.UUID;

public interface AuthService {

    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void logout(LogoutRequest request);

    UserInfoResponse getMe(String accessToken);

    LoginResponse refreshToken(String refreshToken);
}
