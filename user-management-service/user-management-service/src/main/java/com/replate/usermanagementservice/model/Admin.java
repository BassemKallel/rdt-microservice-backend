package com.replate.usermanagementservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "admins") // Une table séparée mais vide
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
public class Admin extends  User {
}
