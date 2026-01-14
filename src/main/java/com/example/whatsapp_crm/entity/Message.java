package com.example.whatsapp_crm.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.sql.Timestamp;

@Entity
@Table(name = "message")
@Data
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "whatsapp_message_id", length = 255, nullable = true)
    private String whatsappMessageId;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "from_me")
    private Boolean fromMe;

    @Column(name = "timestamp")
    private Timestamp timestamp;

    @Column(name = "created_at")
    private Timestamp createdAt;

    // Relationship (optional but nice)
    @ManyToOne
    @JoinColumn(name = "contact_phone", referencedColumnName = "phone", insertable = false, updatable = false)
    private Contact contact;
}
