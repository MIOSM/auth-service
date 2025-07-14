package MIOSM.auth_service.service;

import MIOSM.auth_service.dto.*;

public interface AuthService {
    void register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    void logout(LogoutRequest request);
    UserInfoResponse getMe(String accessToken);
} 