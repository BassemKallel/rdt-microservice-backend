package com.replate.usermanagementservice.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String username;
    private String phoneNumber;
    private String location;
}