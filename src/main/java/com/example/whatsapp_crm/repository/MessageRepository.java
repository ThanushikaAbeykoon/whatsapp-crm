package com.example.whatsapp_crm.repository;

import com.example.whatsapp_crm.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByContactPhoneOrderByTimestampDesc(String phone);
    Optional<Message> findByWhatsappMessageId(String whatsappMessageId);
}
