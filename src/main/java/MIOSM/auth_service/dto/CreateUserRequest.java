package MIOSM.auth_service.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    private UUID id;
    private String username;
    private String bio;
} 