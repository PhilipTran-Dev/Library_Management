package Project1.com.LibraryManagement.DTO;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {
    private String email;
    private String password;
    private String fullName;
    private String phoneNumber;
    private String address;
    private LocalDate dateOfBirth;
    private Long unitId;
}
