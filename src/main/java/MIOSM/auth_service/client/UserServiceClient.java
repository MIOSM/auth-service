package MIOSM.auth_service.client;

import MIOSM.auth_service.dto.CreateUserRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import MIOSM.auth_service.dto.UpdateUserRequest;
import MIOSM.auth_service.dto.UpdateProfileRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.Map;

@FeignClient(name = "user-service", url = "http://localhost:8083", path = "/api/users", configuration = MIOSM.auth_service.config.FeignConfig.class)
public interface UserServiceClient {
    @PostMapping
    void createUser(@RequestBody CreateUserRequest request);

    @GetMapping("/id/{id}")
    Map<String, Object> getUser(@PathVariable UUID id);

    @PatchMapping("/{id}")
    void updateProfile(@PathVariable UUID id, @RequestBody UpdateProfileRequest request);

    @PostMapping(value = "/{id}/avatar", consumes = "multipart/form-data")
    Map<String, Object> uploadAvatar(@PathVariable UUID id, @RequestPart("file") MultipartFile file);

    @PostMapping(value = "/{id}/coverImage", consumes = "multipart/form-data")
    Map<String, Object> uploadCoverImage(@PathVariable UUID id, @RequestPart("file") MultipartFile file);
}