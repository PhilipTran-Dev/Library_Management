package Project1.com.LibraryManagement.Service;

import Project1.com.LibraryManagement.DTO.UserInfoDTO;
import Project1.com.LibraryManagement.Entity.Users;
import Project1.com.LibraryManagement.Repository.UserRepos;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailOtpService {
    private final JavaMailSender javaMailSender;
    private final EmailOtpStorageService emailOtpStorageService;
    private final UserRepos userRepos;
    private final PasswordEncoder passwordEncoder;

    public void sendOtp(String email){
        String otp = emailOtpStorageService.generateOtp();
        emailOtpStorageService.storeOtp(email,otp);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Your OTP Code");
        message.setText("Your OTP is: " + otp + "\nIt is valid for 5 minutes.");
        javaMailSender.send(message);
    }

//    public Users saveUserInfor (UserInfoDTO userInfoDTO, Users users) throws Exception{
//        var saveInfor = Users.builder()
//                .fullName(userInfoDTO.getFullName())
//                .email(userInfoDTO.getEmail())
//                .unit(users.getUnit())
//                .phoneNumber(userInfoDTO.getPhoneNumber())
//                .dateOfBirth(userInfoDTO.getDateOfBirth())
//                .address(userInfoDTO.getAddress())
//                .password(passwordEncoder.encode(userInfoDTO.getPassword()))
//                .build();
//        return userRepos.save(saveInfor);
//    }
}
