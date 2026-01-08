package com.example.whatsapp_crm.controller;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.example.whatsapp_crm.dto.WhatsAppWebhookRequest;
import com.example.whatsapp_crm.entity.Contact;
import com.example.whatsapp_crm.entity.Message;
import com.example.whatsapp_crm.repository.ContactRepository;
import com.example.whatsapp_crm.repository.MessageRepository;
import com.example.whatsapp_crm.service.MessageService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class MessageController {

    @Autowired private MessageRepository messageRepository;
    @Autowired private ContactRepository contactRepository;
    @Autowired private RestTemplate restTemplate;
    @Autowired private MessageService messageService;

    @Value("${whatsapp.phone-number-id}") private String PHONE_NUMBER_ID;
    @Value("${whatsapp.access-token}") private String ACCESS_TOKEN;
    @Value("${whatsapp.version}") private String API_VERSION;
    @Value("${whatsapp.webhook-verify-token:your_verify_token}") private String WEBHOOK_VERIFY_TOKEN;

    // Enhanced GET endpoints with pagination and filtering
    @GetMapping("/messages")
    public ResponseEntity<?> getAllMessages(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "timestamp") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDir) {
        
        if (page != null && size != null) {
            Sort sort = sortDir.equalsIgnoreCase("ASC") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Message> messages = messageService.getAllMessages(pageable);
            return ResponseEntity.ok(Map.of(
                "content", messages.getContent(),
                "totalElements", messages.getTotalElements(),
                "totalPages", messages.getTotalPages(),
                "currentPage", messages.getNumber(),
                "pageSize", messages.getSize()
            ));
        } else {
            List<Message> messages = messageService.getAllMessages();
            return ResponseEntity.ok(messages);
        }
    }

    @GetMapping("/messages/{id}")
    public ResponseEntity<Message> getMessageById(@PathVariable Long id) {
        Optional<Message> message = messageService.getMessageById(id);
        return message.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/messages/phone/{phone}")
    public List<Message> getMessagesByPhone(@PathVariable String phone) {
        return messageService.getMessagesByPhone(phone);
    }

    @GetMapping("/messages/search")
    public ResponseEntity<List<Message>> searchMessages(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        if (phone != null && !phone.isEmpty()) {
            return ResponseEntity.ok(messageService.getMessagesByPhone(phone));
        }
        
        if (startDate != null && endDate != null) {
            try {
                Timestamp start = Timestamp.valueOf(startDate);
                Timestamp end = Timestamp.valueOf(endDate);
                return ResponseEntity.ok(messageService.getMessagesByDateRange(start, end));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/contacts")
    public List<Contact> getAllContacts() { 
        return contactRepository.findAll(); 
    }

    @GetMapping("/contacts/phone/{phone}")
    public ResponseEntity<Contact> getContactByPhone(@PathVariable String phone) {
        Contact contact = contactRepository.findByPhone(phone);
        return contact != null 
            ? ResponseEntity.ok(contact)
            : ResponseEntity.notFound().build();
    }

    // Webhook endpoint to verify webhook (WhatsApp sends GET request during setup)
    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        
        if ("subscribe".equals(mode) && WEBHOOK_VERIFY_TOKEN.equals(token)) {
            return ResponseEntity.ok(challenge);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
        }
    }

    // Webhook endpoint to receive incoming WhatsApp messages
    @PostMapping("/webhook")
    public ResponseEntity<String> receiveWebhook(@RequestBody WhatsAppWebhookRequest request) {
        try {
            if (request.getEntry() == null || request.getEntry().isEmpty()) {
                return ResponseEntity.ok("OK");
            }

            for (WhatsAppWebhookRequest.Entry entry : request.getEntry()) {
                if (entry.getChanges() == null) continue;

                for (WhatsAppWebhookRequest.Entry.Change change : entry.getChanges()) {
                    if (change.getValue() == null) continue;

                    WhatsAppWebhookRequest.Entry.Change.Value value = change.getValue();
                    
                    // Process incoming messages
                    if (value.getMessages() != null) {
                        for (WhatsAppWebhookRequest.Entry.Change.Value.Message message : value.getMessages()) {
                            // Find corresponding contact info
                            WhatsAppWebhookRequest.Entry.Change.Value.Contact contact = null;
                            if (value.getContacts() != null && !value.getContacts().isEmpty()) {
                                contact = value.getContacts().get(0);
                            }
                            
                            // Process and save the message
                            messageService.processIncomingMessage(message, contact);
                        }
                    }
                }
            }

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            System.err.println("Error processing webhook: " + e.getMessage());
            e.printStackTrace();
            // Still return 200 to prevent WhatsApp from retrying
            return ResponseEntity.ok("Error processed");
        }
    }

    // FINAL SEND ENDPOINT — THIS SENDS REAL WHATSAPP MESSAGES!
    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody SendMessageRequest request) {
        try {
            // 1. Save to DB first
            Message msg = new Message();
            msg.setContactPhone(request.phone());
            msg.setBody(request.message());
            msg.setFromMe(true);
            msg.setTimestamp(new Timestamp(System.currentTimeMillis()));
            msg.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            messageRepository.save(msg);

            // 2. Send via WhatsApp Cloud API
            String url = String.format("https://graph.facebook.com/%s/%s/messages", API_VERSION, PHONE_NUMBER_ID);

            String jsonBody = """
                {
                    "messaging_product": "whatsapp",
                    "to": "%s",
                    "type": "text",
                    "text": {
                        "body": "%s"
                    }
                }
                """.formatted(request.phone().replaceAll("[^0-9]", ""), request.message());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(ACCESS_TOKEN);

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return ResponseEntity.ok("Message sent to WhatsApp!");
            } else {
                return ResponseEntity.status(500).body("WhatsApp API error: " + response.getBody());
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    record SendMessageRequest(String phone, String message) {}
}