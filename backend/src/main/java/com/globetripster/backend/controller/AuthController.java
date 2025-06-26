package com.globetripster.backend.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.globetripster.backend.model.User;
import com.globetripster.backend.repository.UserRepository;
import com.globetripster.backend.service.EmailService;
import com.globetripster.backend.service.JwtUtil;

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

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already exists"));
        }

        user.setPassword(encoder.encode(user.getPassword()));
        user.setProvider("local");
        User savedUser = userRepo.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Signup successful");

        // Don't return password
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", savedUser.getId());
        userData.put("email", savedUser.getEmail());
        userData.put("provider", savedUser.getProvider());
        userData.put("name", savedUser.getName()); // if you have name

        response.put("user", userData);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User req) {
        Optional<User> userOpt = userRepo.findByEmail(req.getEmail());
        if (userOpt.isEmpty() || !encoder.matches(req.getPassword(), userOpt.get().getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        }

        String token = jwtUtil.generateToken(userOpt.get().getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("token", token);

        return ResponseEntity.ok(response);
    }

    

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        Optional<User> userOpt = userRepo.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        String resetToken = jwtUtil.generateResetToken(email);
        String url = "http://localhost:5173/reset-password?token="
                + URLEncoder.encode(resetToken, StandardCharsets.UTF_8);

        emailService.sendResetEmail(email, url);

        return ResponseEntity.ok(Map.of("message", "Reset link sent to your email"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("password");

        // System.out.println("Token: " + token);
        // System.out.println("New Password: " + newPassword);

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
            System.out.println("Reset error: " + e.getMessage());
            return ResponseEntity.status(400).body(Map.of("message", "Invalid or expired token"));
        }
    }

}
