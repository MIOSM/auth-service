package MIOSM.auth_service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {
    @JsonAlias({"refresh_token"})
    private String refreshToken;
} 