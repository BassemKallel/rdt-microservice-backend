package com.replate.usermanagementservice.dto;

import com.replate.usermanagementservice.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.management.relation.Role;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {


    private Long userId;
    private String username;
    private UserRole role;
    private String jwtToken;

}