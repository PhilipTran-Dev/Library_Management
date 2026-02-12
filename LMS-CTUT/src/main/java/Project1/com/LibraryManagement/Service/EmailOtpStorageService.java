package Project1.com.LibraryManagement.Service;

import Project1.com.LibraryManagement.Entity.OtpDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class EmailOtpStorageService {
    private Map<String, OtpDetails> otpStore = new HashMap<>();

    public OtpDetails storeOtp(String email, String otp){
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(5);
        OtpDetails otpDetails = new OtpDetails(email, otp, expireAt);
        return otpStore.put(email,otpDetails);
    }

    public String generateOtp(){
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    public OtpDetails getAllOptDetails(String email){
        return otpStore.get(email);
    }

    public void removeOtp(String email){
        otpStore.remove(email);
    }
}
