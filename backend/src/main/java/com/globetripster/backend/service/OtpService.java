package com.globetripster.backend.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.globetripster.backend.model.User;
import com.globetripster.backend.repository.UserRepository;

@Service
public class OtpService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private EmailService emailService;

    private final Random random = new Random();

    private String generateOtp() {
        return String.format("%06d", random.nextInt(999999));
    }

    public void generateAndSendOtp(User user, String purpose) {
        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepo.save(user);

        if (purpose.equalsIgnoreCase("login")) {
            emailService.sendOtpEmail(user.getEmail(), otp); // Update this to a login-specific template if needed
        } else if (purpose.equalsIgnoreCase("signup")) {
            emailService.sendOtpEmail(user.getEmail(), otp); // Or a different signup email if desired
        }
    }

    public boolean verifyOtp(String email, String inputOtp) {
        Optional<User> userOpt = userRepo.findByEmail(email);
        if (userOpt.isEmpty())
            return false;

        User user = userOpt.get();

        boolean valid = user.getOtp() != null
                && user.getOtpExpiry() != null
                && LocalDateTime.now().isBefore(user.getOtpExpiry())
                && user.getOtp().equals(inputOtp);

        if (valid) {
            user.setOtp(null);
            user.setOtpExpiry(null);
            user.setIsVerified(true);
            userRepo.save(user);
        }

        return valid;
    }
}
