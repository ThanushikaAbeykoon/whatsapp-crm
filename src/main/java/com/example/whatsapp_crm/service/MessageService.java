package com.example.whatsapp_crm.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.whatsapp_crm.dto.WhatsAppWebhookRequest;
import com.example.whatsapp_crm.entity.Contact;
import com.example.whatsapp_crm.entity.Message;
import com.example.whatsapp_crm.repository.ContactRepository;
import com.example.whatsapp_crm.repository.MessageRepository;

@Service
@Transactional
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ContactRepository contactRepository;

    public void processIncomingMessage(WhatsAppWebhookRequest.Entry.Change.Value.Message messageData,
                                      WhatsAppWebhookRequest.Entry.Change.Value.Contact contactData) {
        try {
            String phoneNumber = messageData.getFrom();
            String messageId = messageData.getId();
            
            // Check if message already exists to avoid duplicates
            Optional<Message> existingMessage = messageRepository.findAll()
                .stream()
                .filter(m -> messageId.equals(m.getWhatsappMessageId()))
                .findFirst();

            if (existingMessage.isPresent()) {
                return; // Message already processed
            }

            // Extract message body based on type
            String body = "";
            if (messageData.getText() != null) {
                body = messageData.getText().getBody();
            } else if (messageData.getImage() != null) {
                body = messageData.getImage().getCaption() != null 
                    ? "[Image] " + messageData.getImage().getCaption()
                    : "[Image]";
            } else if (messageData.getDocument() != null) {
                body = messageData.getDocument().getCaption() != null
                    ? "[Document: " + messageData.getDocument().getFilename() + "] " + messageData.getDocument().getCaption()
                    : "[Document: " + messageData.getDocument().getFilename() + "]";
            }

            // Create or update contact
            Contact contact = contactRepository.findByPhone(phoneNumber);
            if (contact == null) {
                contact = new Contact();
                contact.setPhone(phoneNumber);
                if (contactData != null && contactData.getProfile() != null) {
                    contact.setName(contactData.getProfile().getName());
                }
                contact.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                contactRepository.save(contact);
            } else if (contactData != null && contactData.getProfile() != null && 
                       contactData.getProfile().getName() != null &&
                       !contactData.getProfile().getName().equals(contact.getName())) {
                // Update contact name if it has changed
                contact.setName(contactData.getProfile().getName());
                contactRepository.save(contact);
            }

            // Save message
            Message message = new Message();
            message.setWhatsappMessageId(messageId);
            message.setContactPhone(phoneNumber);
            message.setBody(body);
            message.setFromMe(false); // Incoming message
            message.setTimestamp(new Timestamp(Long.parseLong(messageData.getTimestamp()) * 1000));
            message.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            
            messageRepository.save(message);
        } catch (Exception e) {
            // Log error but don't throw to avoid webhook retries
            System.err.println("Error processing incoming message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }

    public Page<Message> getAllMessages(Pageable pageable) {
        return messageRepository.findAll(pageable);
    }

    public List<Message> getMessagesByPhone(String phone) {
        return messageRepository.findByContactPhoneOrderByTimestampDesc(phone);
    }

    public Optional<Message> getMessageById(Long id) {
        return messageRepository.findById(id);
    }

    public List<Message> getMessagesByDateRange(Timestamp startDate, Timestamp endDate) {
        return messageRepository.findAll().stream()
            .filter(m -> m.getTimestamp() != null &&
                        m.getTimestamp().after(startDate) &&
                        m.getTimestamp().before(endDate))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .toList();
    }
}

