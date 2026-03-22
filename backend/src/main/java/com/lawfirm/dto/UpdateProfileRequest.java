package com.lawfirm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String phoneNumber;

    @Size(max = 255)
    private String address;

    public String getFirstName()  { return firstName; }
    public void setFirstName(String v)  { this.firstName = v; }
    public String getLastName()   { return lastName; }
    public void setLastName(String v)   { this.lastName = v; }
    public String getEmail()      { return email; }
    public void setEmail(String v)      { this.email = v; }
    public String getPhoneNumber(){ return phoneNumber; }
    public void setPhoneNumber(String v){ this.phoneNumber = v; }
    public String getAddress()    { return address; }
    public void setAddress(String v)    { this.address = v; }
}