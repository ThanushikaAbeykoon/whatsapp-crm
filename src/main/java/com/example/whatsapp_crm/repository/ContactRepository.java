package com.example.whatsapp_crm.repository;

import com.example.whatsapp_crm.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    Contact findByPhone(String phone);
}
