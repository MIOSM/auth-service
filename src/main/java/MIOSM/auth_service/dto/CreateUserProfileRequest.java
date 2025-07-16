package MIOSM.auth_service.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateUserProfileRequest {
    private UUID id;
    private String username;
    private String bio;
} 