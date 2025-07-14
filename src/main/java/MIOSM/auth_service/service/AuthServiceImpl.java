package MIOSM.auth_service.service;

import MIOSM.auth_service.dto.*;
import MIOSM.auth_service.exception.AuthServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final WebClient webClient;

    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Value("${keycloak.admin.username}")
    private String adminUsername;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    @Override
    public void register(RegisterRequest request) {
        String token = getAdminAccessToken();
        String url = String.format("%s/admin/realms/%s/users", keycloakUrl, realm);
        Map<String, Object> userPayload = Map.of(
            "username", request.getEmail(),
            "email", request.getEmail(),
            "enabled", true,
            "credentials", new Object[] {
                Map.of(
                    "type", "password",
                    "value", request.getPassword(),
                    "temporary", false
                )
            }
        );
        try {
            webClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userPayload)
                .retrieve()
                .toBodilessEntity()
                .block();
            log.info("User {} registered successfully", request.getEmail());
        } catch (Exception e) {
            log.error("Error registering user: {}", e.getMessage());
            throw new AuthServiceException("Registration failed: " + e.getMessage());
        }
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String url = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, realm);
        try {
            Map<String, String> form = Map.of(
                "grant_type", "password",
                "client_id", clientId,
                "client_secret", clientSecret,
                "username", request.getUsername(),
                "password", request.getPassword()
            );
            Map<String, Object> response = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            return new LoginResponse(
                (String) response.get("access_token"),
                (String) response.get("refresh_token"),
                (String) response.get("token_type"),
                ((Number) response.get("expires_in")).longValue()
            );
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            throw new AuthServiceException("Login failed: " + e.getMessage());
        }
    }

    @Override
    public void logout(LogoutRequest request) {
        String url = String.format("%s/realms/%s/protocol/openid-connect/logout", keycloakUrl, realm);
        try {
            Map<String, String> form = Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "refresh_token", request.getRefreshToken()
            );
            webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .toBodilessEntity()
                .block();
            log.info("Logout successful");
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            throw new AuthServiceException("Logout failed: " + e.getMessage());
        }
    }

    @Override
    public UserInfoResponse getMe(String accessToken) {
        String url = String.format("%s/realms/%s/protocol/openid-connect/userinfo", keycloakUrl, realm);
        try {
            Map<String, Object> response = webClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            return new UserInfoResponse(
                (String) response.get("preferred_username"),
                (String) response.get("email"),
                (String) response.get("given_name"),
                (String) response.get("family_name")
            );
        } catch (Exception e) {
            log.error("Get user info failed: {}", e.getMessage());
            throw new AuthServiceException("Get user info failed: " + e.getMessage());
        }
    }

    private String getAdminAccessToken() {
        String url = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, realm);
        Map<String, String> form = Map.of(
            "grant_type", "password",
            "client_id", "admin-cli",
            "username", adminUsername,
            "password", adminPassword
        );
        try {
            Map<String, Object> response = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            return (String) response.get("access_token");
        } catch (Exception e) {
            log.error("Failed to get admin access token: {}", e.getMessage());
            throw new AuthServiceException("Failed to get admin access token: " + e.getMessage());
        }
    }
} 