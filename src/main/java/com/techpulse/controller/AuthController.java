package com.techpulse.controller;

import com.techpulse.dto.AuthDtos;
import com.techpulse.model.Role;
import com.techpulse.model.User;
import com.techpulse.repository.UserRepository;
import com.techpulse.service.DateWindowService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Authentication, Registration, Session State, and CSRF Token inspection.
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final DateWindowService dateWindowService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            DateWindowService dateWindowService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.dateWindowService = dateWindowService;
    }

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        String token = csrf != null ? csrf.getToken() : "";
        return ResponseEntity.ok(Map.of("token", token, "headerName", "X-XSRF-TOKEN"));
    }

    @GetMapping("/auth/me")
    public ResponseEntity<AuthDtos.UserProfileResponse> getCurrentUser(
            Authentication authentication, HttpServletRequest request) {
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        String token = csrf != null ? csrf.getToken() : "";

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.ok(new AuthDtos.UserProfileResponse(
                    false, null, null, null, null, null, token
            ));
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(authentication.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(new AuthDtos.UserProfileResponse(
                    false, null, null, null, null, null, token
            ));
        }

        User user = userOpt.get();
        return ResponseEntity.ok(new AuthDtos.UserProfileResponse(
                true,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getCollegeOrInstitution(),
                user.getRole().name(),
                token
        ));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<AuthDtos.UserProfileResponse> register(
            @Valid @RequestBody AuthDtos.RegisterRequest request,
            HttpServletRequest httpRequest) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email address is already registered.");
        }

        User newUser = new User(
                request.getFullName().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.getPassword()),
                request.getCollegeOrInstitution() != null && !request.getCollegeOrInstitution().isBlank()
                        ? request.getCollegeOrInstitution().trim()
                        : "Not provided",
                Role.USER,
                dateWindowService.nowInKolkata()
        );
        User saved = userRepository.save(newUser);

        // Automatically establish server-side session for the registered student
        authenticateAndCreateSession(normalizedEmail, request.getPassword(), httpRequest);

        CsrfToken csrf = (CsrfToken) httpRequest.getAttribute(CsrfToken.class.getName());
        String token = csrf != null ? csrf.getToken() : "";

        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthDtos.UserProfileResponse(
                true,
                saved.getId(),
                saved.getFullName(),
                saved.getEmail(),
                saved.getCollegeOrInstitution(),
                saved.getRole().name(),
                token
        ));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthDtos.UserProfileResponse> login(
            @Valid @RequestBody AuthDtos.LoginRequest request,
            HttpServletRequest httpRequest) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        authenticateAndCreateSession(normalizedEmail, request.getPassword(), httpRequest);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        CsrfToken csrf = (CsrfToken) httpRequest.getAttribute(CsrfToken.class.getName());
        String token = csrf != null ? csrf.getToken() : "";

        return ResponseEntity.ok(new AuthDtos.UserProfileResponse(
                true,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getCollegeOrInstitution(),
                user.getRole().name(),
                token
        ));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    private void authenticateAndCreateSession(String email, String rawPassword, HttpServletRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, rawPassword)
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }
}
