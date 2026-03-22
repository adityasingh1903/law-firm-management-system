package com.lawfirm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {

    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = 6, max = 100)
    private String newPassword;

    @NotBlank
    private String confirmPassword;

    public String getCurrentPassword()  { return currentPassword; }
    public void setCurrentPassword(String v)  { this.currentPassword = v; }
    public String getNewPassword()      { return newPassword; }
    public void setNewPassword(String v)      { this.newPassword = v; }
    public String getConfirmPassword()  { return confirmPassword; }
    public void setConfirmPassword(String v)  { this.confirmPassword = v; }
}