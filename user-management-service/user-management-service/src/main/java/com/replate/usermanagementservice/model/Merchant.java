package com.replate.usermanagementservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "merchants")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter @Setter
public class Merchant extends User {
    private String documentUrl;
}