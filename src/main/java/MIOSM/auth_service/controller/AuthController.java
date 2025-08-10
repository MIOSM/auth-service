package MIOSM.auth_service.controller;

import MIOSM.auth_service.dto.*;
import MIOSM.auth_service.exception.AuthServiceException;
import MIOSM.auth_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            LoginResponse loginResponse = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Registration successful!",
                "accessToken", loginResponse.getAccessToken(),
                "refreshToken", loginResponse.getRefreshToken(),
                "tokenType", loginResponse.getTokenType(),
                "expiresIn", loginResponse.getExpiresIn()
            ));
        } catch (AuthServiceException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse loginResponse = authService.login(request);
            UserInfoResponse userInfo = authService.getMe(loginResponse.getAccessToken());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Login successful!",
                "token", loginResponse.getAccessToken(),
                "refreshToken", loginResponse.getRefreshToken(),
                "tokenType", loginResponse.getTokenType(),
                "expiresIn", loginResponse.getExpiresIn(),
                "user", userInfo
            ));
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Login failed."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest request) {
        try {
            authService.logout(request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Logout error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshTokenRequest request) {
        try {
            LoginResponse response = authService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Refresh token error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse("Refresh failed", "", "", 0L));
        }
    }

    @PatchMapping("/update")
    public ResponseEntity<?> updateUser(@RequestBody UpdateUserRequest request, HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        log.info("Received update request. Auth header: {}", authHeader);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("No valid authorization header found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "No token"));
        }
        String token = authHeader.substring(7);
        log.info("Extracted token: {}", token.substring(0, Math.min(20, token.length())) + "...");
        
        try {
            authService.updateUser(request, token);

            UserInfoResponse userInfo = authService.getMe(token);
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "User updated successfully!",
                "user", userInfo
            ));
        } catch (AuthServiceException e) {
            log.error("Update user failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping(value = "/upload-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAvatar(@RequestPart("file") MultipartFile file, 
                                         @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "No token"));
        }
        String token = authHeader.substring(7);
        try {
            String avatarUrl = authService.uploadAvatar(file, token);

            UserInfoResponse userInfo = authService.getMe(token);
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Avatar uploaded successfully!", 
                "avatarUrl", avatarUrl,
                "user", userInfo
            ));
        } catch (AuthServiceException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping(value = "/upload-cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCover(@RequestPart("file") MultipartFile file, 
                                        @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "No token"));
        }
        String token = authHeader.substring(7);
        try {
            String coverUrl = authService.uploadCover(file, token);

            UserInfoResponse userInfo = authService.getMe(token);
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Cover uploaded successfully!", 
                "coverUrl", coverUrl,
                "user", userInfo
            ));
        } catch (AuthServiceException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getMe(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = authHeader.substring(7);
        try {
            UserInfoResponse userInfo = authService.getMe(token);
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.error("Get user info error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}