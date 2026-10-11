package com.tripplanner.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AddMemberRequest {
    @NotBlank(message = "Member email is required")
    @Email(message = "Member email must be valid")
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
