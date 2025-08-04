package MIOSM.auth_service.client;

import MIOSM.auth_service.dto.CreateUserRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import MIOSM.auth_service.dto.UpdateUserRequest;
import MIOSM.auth_service.dto.UpdateProfileRequest;

import java.util.UUID;
import java.util.Map;

@FeignClient(name = "user-service", url = "http://localhost:8083", path = "/api/users")
public interface UserServiceClient {
    @PostMapping
    void createUser(@RequestBody CreateUserRequest request);

    @PatchMapping("/{id}")
    void updateProfile(@PathVariable UUID id, @RequestBody UpdateProfileRequest request);
}