package com.replate.usermanagementservice.model;

import jakarta.persistence.Column;
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
    @Column(nullable = false)
    private String documentUrl;
}