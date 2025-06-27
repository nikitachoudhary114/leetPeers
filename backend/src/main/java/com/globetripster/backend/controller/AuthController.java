package com.globetripster.backend.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.globetripster.backend.model.User;
import com.globetripster.backend.repository.UserRepository;
import com.globetripster.backend.service.EmailService;
import com.globetripster.backend.service.JwtUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private Random random = new Random();

    private String generateOtp() {
        return String.format("%06d", random.nextInt(999999));
    }

    private void setTokenCookie(HttpServletResponse response, String token) {
        response.setHeader("Set-Cookie",
                "token=" + token +
                        "; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=" + (7 * 24 * 60 * 60));
    }

    private void clearTokenCookie(HttpServletResponse response) {
        response.setHeader("Set-Cookie",
                "token=; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=0");
    }

    // --- SIGNUP with OTP Verification ---
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already exists"));
        }

        user.setPassword(encoder.encode(user.getPassword()));
        user.setProvider("local");

        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        user.setIsVerified(false);

        userRepo.save(user);
        emailService.sendOtpEmail(user.getEmail(), otp);

        return ResponseEntity.ok(Map.of("message", "Signup successful. OTP sent to email for verification."));
    }

    // --- LOGIN with OTP ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User req) {
        Optional<User> userOpt = userRepo.findByEmail(req.getEmail());
        if (userOpt.isEmpty() || !encoder.matches(req.getPassword(), userOpt.get().getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        }

        User user = userOpt.get();
        if (!user.isVerified()) {
            return ResponseEntity.status(403)
                    .body(Map.of("message", "Email not verified. Complete signup verification first."));
        }

        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepo.save(user);

        emailService.sendOtpEmail(user.getEmail(), otp);
        return ResponseEntity.ok(Map.of("message", "OTP sent to email"));
    }

    // --- OTP VERIFICATION for Signup/Login ---
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request, HttpServletResponse response) {
        String email = request.get("email");
        String otp = request.get("otp");

        Optional<User> userOpt = userRepo.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        User user = userOpt.get();
        if (user.getOtp() == null || user.getOtpExpiry() == null ||
                LocalDateTime.now().isAfter(user.getOtpExpiry()) || !user.getOtp().equals(otp)) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid or expired OTP"));
        }

        user.setIsVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepo.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        setTokenCookie(response, token);

        return ResponseEntity.ok(Map.of("message", "OTP verified. Login successful"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        Optional<User> userOpt = userRepo.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        String resetToken = jwtUtil.generateResetToken(email);
        String url = "http://localhost:5173/reset-password?token=" +
                URLEncoder.encode(resetToken, StandardCharsets.UTF_8);

        emailService.sendResetEmail(email, url);
        return ResponseEntity.ok(Map.of("message", "Reset link sent to your email"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("password");

        if (token == null || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Token and password are required"));
        }

        try {
            String email = jwtUtil.extractEmail(token);
            if (email == null) {
                return ResponseEntity.status(400).body(Map.of("message", "Invalid or expired token"));
            }

            Optional<User> userOpt = userRepo.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("message", "User not found"));
            }

            User user = userOpt.get();
            user.setPassword(encoder.encode(newPassword));
            userRepo.save(user);

            return ResponseEntity.ok(Map.of("message", "Password reset successful"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("message", "Invalid or expired token"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        clearTokenCookie(response);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/api/auth/check")
public ResponseEntity<?> checkAuth(HttpServletRequest request) {
    String token = Arrays.stream(request.getCookies())
        .filter(c -> "token".equals(c.getName()))
        .findFirst()
        .map(Cookie::getValue)
        .orElse(null);

    if (token != null && jwtUtil.validateToken(token)) {
        return ResponseEntity.ok().build();
    }

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
}


}
