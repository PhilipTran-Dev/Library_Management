package Project1.com.LibraryManagement.Controller;

import Project1.com.LibraryManagement.Entity.OtpDetails;
import Project1.com.LibraryManagement.Entity.Users;
import Project1.com.LibraryManagement.Repository.UserRepos;
import Project1.com.LibraryManagement.Service.EmailOtpService;
import Project1.com.LibraryManagement.Service.EmailOtpStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
public class EmailOtpController {
    private final EmailOtpStorageService emailOtpStorageService;
    private final EmailOtpService emailOtpService;
    private final UserRepos userRepos;

    @PostMapping("/send")
    public String sendOtp(@RequestParam String email){
        emailOtpService.sendOtp(email);
        return "";   //create page for inform notification verified and form type otp
    }

    @PostMapping("/verify")
    public String verifyOtp(@RequestParam String email, @RequestParam String otp){
        OtpDetails otpDetails = emailOtpStorageService.getAllOptDetails(email);

        if (otpDetails.getExpirationTime().isBefore(LocalDateTime.now())) {
            emailOtpStorageService.removeOtp(email);
            return "OTP has expired.";
        }

        if (!otpDetails.getOtp().equals(otp)) {
            return "Invalid OTP."; // return type otp page
        }

        emailOtpStorageService.removeOtp(email);
        return "OTP verified successfully!";//save info to db and go to home page
    }

}
