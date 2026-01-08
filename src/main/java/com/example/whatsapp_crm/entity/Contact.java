package com.example.whatsapp_crm.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "contact")
@Data
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone", length = 20, unique = true, nullable = false)
    private String phone;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "created_at")
    private java.sql.Timestamp createdAt;
}