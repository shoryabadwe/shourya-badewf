package com.techpulse.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Objects for authentication and session state.
 */
public class AuthDtos {

    public static class LoginRequest {
        @NotBlank(message = "Email address is required")
        @Email(message = "Please enter a valid email address")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class RegisterRequest {
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 120, message = "Full name must be between 2 and 120 characters")
        private String fullName;

        @NotBlank(message = "Email address is required")
        @Email(message = "Please enter a valid email address")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be at least 8 characters long")
        private String password;

        @Size(max = 160, message = "Institution name cannot exceed 160 characters")
        private String collegeOrInstitution;

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getCollegeOrInstitution() {
            return collegeOrInstitution;
        }

        public void setCollegeOrInstitution(String collegeOrInstitution) {
            this.collegeOrInstitution = collegeOrInstitution;
        }
    }

    public static class UserProfileResponse {
        private boolean authenticated;
        private Long id;
        private String fullName;
        private String email;
        private String collegeOrInstitution;
        private String role;
        private String csrfToken;

        public UserProfileResponse() {
        }

        public UserProfileResponse(boolean authenticated, Long id, String fullName, String email,
                                   String collegeOrInstitution, String role, String csrfToken) {
            this.authenticated = authenticated;
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.collegeOrInstitution = collegeOrInstitution;
            this.role = role;
            this.csrfToken = csrfToken;
        }

        public boolean isAuthenticated() {
            return authenticated;
        }

        public void setAuthenticated(boolean authenticated) {
            this.authenticated = authenticated;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getCollegeOrInstitution() {
            return collegeOrInstitution;
        }

        public void setCollegeOrInstitution(String collegeOrInstitution) {
            this.collegeOrInstitution = collegeOrInstitution;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getCsrfToken() {
            return csrfToken;
        }

        public void setCsrfToken(String csrfToken) {
            this.csrfToken = csrfToken;
        }
    }
}
