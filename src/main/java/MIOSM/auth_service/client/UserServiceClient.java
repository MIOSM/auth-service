package MIOSM.auth_service.client;

import MIOSM.auth_service.dto.CreateUserProfileRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", path = "/api/users")
public interface UserServiceClient {
    @PostMapping
    void createUserProfile(@RequestBody CreateUserProfileRequest request);
}