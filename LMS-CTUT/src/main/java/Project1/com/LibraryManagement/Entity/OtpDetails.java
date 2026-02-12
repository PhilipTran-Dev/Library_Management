package Project1.com.LibraryManagement.Entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpDetails {
    private String email;
    private String otp;
    private LocalDateTime expirationTime;

}
