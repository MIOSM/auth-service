package MIOSM.auth_service.service;

import MIOSM.auth_service.dto.*;
import MIOSM.auth_service.exception.AuthServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.Map;
import java.util.UUID;
import MIOSM.auth_service.client.UserServiceClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final WebClient webClient;
    private final UserServiceClient userServiceClient;

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
    public LoginResponse register(RegisterRequest request) {
        String token = getAdminAccessToken();
        String url = String.format("%s/admin/realms/%s/users", keycloakUrl, realm);
        Map<String, Object> userPayload = Map.of(
            "username", request.getUsername(),
            "email", request.getEmail(),
            "firstName", request.getFirstName(),
            "lastName", request.getLastName(),
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
            ResponseEntity<Void> response = webClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userPayload)
                .retrieve()
                .toBodilessEntity()
                .block();
            String location = response != null && response.getHeaders().getLocation() != null
                ? response.getHeaders().getLocation().toString()
                : null;
            if (location != null && location.contains("/users/")) {
                String userIdStr = location.substring(location.lastIndexOf("/users/") + 7);
                UUID userId = UUID.fromString(userIdStr);
                log.info("User {} registered successfully with id {}", request.getEmail(), userId);
                try {
                    CreateUserRequest userRequest = new CreateUserRequest();
                    userRequest.setId(userId);
                    userRequest.setUsername(request.getUsername());
                    userRequest.setBio("");
                    log.info("Creating user in user-service: id={}, username={}, bio={}", 
                        userRequest.getId(), userRequest.getUsername(), userRequest.getBio());
                    userServiceClient.createUser(userRequest);
                    log.info("User created in user-service with id {}", userId);
                } catch (Exception e) {
                    log.error("Failed to create user in user-service: {}", e.getMessage());
                }

                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsername(request.getUsername());
                loginRequest.setEmail(request.getEmail());
                loginRequest.setPassword(request.getPassword());
                LoginResponse loginResponse = login(loginRequest);
                return loginResponse;
            } else {
                log.error("User registered but could not extract user id from Location header");
                throw new AuthServiceException("Registration failed: could not extract user id");
            }
        } catch (Exception e) {
            log.error("Error registering user: {}", e.getMessage());
            throw new AuthServiceException("Registration failed: " + e.getMessage());
        }
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String url = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, realm);
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "password");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            String login = request.getUsername() != null && !request.getUsername().isBlank()
                ? request.getUsername()
                : request.getEmail();
            form.add("username", login);
            form.add("password", request.getPassword());
            form.add("scope", "openid profile email");
            log.info("Login attempt: clientId={}, username={}, password={}", clientId, login, request.getPassword());
            Map<String, Object> response = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
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
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            form.add("refresh_token", request.getRefreshToken());
            log.info("Logout: client_id={}, client_secret={}, refresh_token={}", clientId, clientSecret, request.getRefreshToken());
            webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                    response.bodyToMono(String.class).flatMap(body -> {
                        log.error("Logout failed with status: {}, body: {}", response.statusCode(), body);
                        return response.createException();
                    })
                )
                .toBodilessEntity()
                .block();
            log.info("Logout successful");
        } catch (Exception e) {
            log.error("Logout outer catch: {}", e.getMessage(), e);
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

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        String url = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, realm);
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "refresh_token");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            form.add("refresh_token", refreshToken);
            Map<String, Object> response = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
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
            log.error("Refresh token failed: {}", e.getMessage());
            throw new AuthServiceException("Refresh token failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateUser(UpdateUserRequest request, String accessToken) {
        String token = getAdminAccessToken();
        UUID userId = null;
        Map<String, Object> userInfo = null;
        try {
            String userInfoUrl = String.format("%s/realms/%s/protocol/openid-connect/userinfo", keycloakUrl, realm);
            userInfo = webClient.get()
                .uri(userInfoUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(java.util.Map.class)
                .block();
            if (userInfo == null || userInfo.get("sub") == null) {
                throw new AuthServiceException("Cannot extract user id from token");
            }
            String keycloakId = userInfo.get("sub").toString();
            userId = UUID.fromString(keycloakId);
        } catch (Exception e) {
            log.error("Failed to extract userId from JWT: {}", e.getMessage());
            throw new AuthServiceException("Failed to extract userId from JWT: " + e.getMessage());
        }

        try {
            java.util.Map<String, String> usernamePayload = java.util.Map.of("username", request.getUsername());
            userServiceClient.updateUsername(userId, usernamePayload);
        } catch (Exception e) {
            log.error("Failed to update user in user-service: {}", e.getMessage());
            throw new AuthServiceException("Update failed in user-service: " + e.getMessage());
        }

        String keycloakUserUrl = String.format("%s/admin/realms/%s/users/%s", keycloakUrl, realm, userId);
        Map<String, Object> keycloakPayload = Map.of(
            "id", userId.toString(),
            "username", request.getUsername(),
            "firstName", request.getFirstName(),
            "lastName", request.getLastName(),
            "enabled", true
        );
        try {
            webClient.put()
                .uri(keycloakUserUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(keycloakPayload)
                .retrieve()
                .toBodilessEntity()
                .block();
        } catch (Exception e) {
            log.error("Failed to update user in Keycloak, rolling back user-service: {}", e.getMessage());
            try {
                String oldUsername = userInfo.get("preferred_username").toString();
                java.util.Map<String, String> rollbackPayload = java.util.Map.of("username", oldUsername);
                userServiceClient.updateUsername(userId, rollbackPayload);
            } catch (Exception rollbackEx) {
                log.error("Rollback in user-service failed: {}", rollbackEx.getMessage());
            }
            throw new AuthServiceException("Update failed in Keycloak: " + e.getMessage());
        }
    }

    private String getAdminAccessToken() {
        String url = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, realm);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", adminUsername);
        form.add("password", adminPassword);
        try {
            Map<String, Object> response = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
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