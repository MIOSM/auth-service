package MIOSM.auth_service.service;

import MIOSM.auth_service.dto.*;

import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void logout(LogoutRequest request);

    UserInfoResponse getMe(String accessToken);

    LoginResponse refreshToken(String refreshToken);

    void updateUser(UpdateUserRequest request, String accessToken);

    String uploadAvatar(HttpServletRequest request, String accessToken);

    String uploadCover(HttpServletRequest request, String accessToken);
}
