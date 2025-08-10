package MIOSM.auth_service.service;

import MIOSM.auth_service.dto.*;

import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {

    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void logout(LogoutRequest request);

    UserInfoResponse getMe(String accessToken);

    LoginResponse refreshToken(String refreshToken);

    void updateUser(UpdateUserRequest request, String accessToken);

    String uploadAvatar(MultipartFile file, String accessToken);

    String uploadCover(MultipartFile file, String accessToken);
}
