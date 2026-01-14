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

import com.example.whatsapp_crm.entity.Contact;
import com.example.whatsapp_crm.entity.Message;
import com.example.whatsapp_crm.repository.ContactRepository;
import com.example.whatsapp_crm.repository.MessageRepository;
import com.example.whatsapp_crm.service.MessageService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"https://whatsapp-crm-frontend-production.up.railway.app", "http://localhost:3000"})
public class MessageController {

    @Autowired private MessageRepository messageRepository;
    @Autowired private ContactRepository contactRepository;
    @Autowired private RestTemplate restTemplate;
    @Autowired private MessageService messageService;

    @Value("${whatsapp.phone-number-id}") private String PHONE_NUMBER_ID;
    @Value("${whatsapp.access-token}") private String ACCESS_TOKEN;
    @Value("${whatsapp.version}") private String API_VERSION;
    @Value("${whatsapp.webhook-verify-token:Testtoken12345}") private String WEBHOOK_VERIFY_TOKEN;

    // Root endpoint - Health check and API info
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "status", "running",
                "message", "WhatsApp CRM API is running",
                "version", "1.0.0",
                "endpoints", Map.of(
                        "messages", "/api/messages",
                        "contacts", "/api/contacts",
                        "send", "/api/send",
                        "webhook", "/api/webhook",
                        "health", "/api/health"
                )
        ));
    }

    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        try {
            long messageCount = messageRepository.count();
            long contactCount = contactRepository.count();

            return ResponseEntity.ok(Map.of(
                    "status", "healthy",
                    "database", "connected",
                    "messageCount", messageCount,
                    "contactCount", contactCount,
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "status", "unhealthy",
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    // Messages endpoints
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

    // Webhook verification (GET)
    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && WEBHOOK_VERIFY_TOKEN.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
    }

    // Main webhook for incoming messages (POST)
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> payload) {
        try {
            System.out.println("=== Webhook Received ===");
            System.out.println("Timestamp: " + new java.util.Date());

            List<Map<String, Object>> entries = (List<Map<String, Object>>) payload.get("entry");
            if (entries == null || entries.isEmpty()) {
                System.out.println("No entries found in webhook payload");
                return ResponseEntity.ok("OK");
            }

            int messagesProcessed = 0;
            for (Map<String, Object> entry : entries) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");
                if (changes == null || changes.isEmpty()) {
                    continue;
                }

                for (Map<String, Object> change : changes) {
                    String field = (String) change.get("field");
                    if (!"messages".equals(field)) {
                        System.out.println("Ignoring non-messages field: " + field);
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> value = (Map<String, Object>) change.get("value");
                    if (value == null) {
                        continue;
                    }

                    // Extract contacts
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> contacts = (List<Map<String, Object>>) value.get("contacts");
                    if (contacts == null || contacts.isEmpty()) {
                        System.out.println("No contacts in payload - skipping");
                        continue;
                    }

                    Map<String, Object> contact = contacts.get(0);
                    final String senderPhone = (String) contact.get("wa_id");
                    if (senderPhone == null) {
                        System.out.println("Missing wa_id in contact - skipping");
                        continue;
                    }

                    final String senderName;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> profile = (Map<String, Object>) contact.get("profile");
                    String profileName = (profile != null ? (String) profile.get("name") : null);
                    senderName = profileName != null && !profileName.isEmpty() ? profileName : "Unknown";

                    // Extract messages
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");

                    if (messages == null || messages.isEmpty()) {
                        // This could be a status update (sent/delivered/read)
                        System.out.println("Status update received (no message content) from: " + senderPhone);
                        continue;
                    }

                    // Process each message
                    // NOTE: All messages are saved, even if they are duplicates from the same contact
                    // This ensures complete message history is preserved
                    for (Map<String, Object> message : messages) {
                        String messageId = (String) message.get("id");
                        if (messageId == null) {
                            System.out.println("Message ID is null - skipping");
                            continue;
                        }

                        String type = (String) message.get("type");
                        String body = extractMessageBody(message, type);

                        String timestampStr = (String) message.get("timestamp");
                        long timestamp = timestampStr != null ? Long.parseLong(timestampStr) * 1000 : System.currentTimeMillis();

                        // Find or create contact
                        Contact dbContact = contactRepository.findByPhone(senderPhone);
                        if (dbContact == null) {
                            dbContact = new Contact();
                            dbContact.setPhone(senderPhone);
                            dbContact.setName(senderName);
                            dbContact.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                            dbContact = contactRepository.save(dbContact);
                            System.out.println("✓ New contact created: " + senderName + " (" + senderPhone + ")");
                        } else if (!senderName.equals("Unknown") && !senderName.equals(dbContact.getName())) {
                            // Update contact name if it has changed
                            dbContact.setName(senderName);
                            contactRepository.save(dbContact);
                            System.out.println("✓ Contact name updated: " + senderName);
                        }

                        // Save message to database
                        // IMPORTANT: All messages are saved, including duplicates from same contact
                        // Each message gets a unique database ID, even if WhatsApp message ID is the same
                        Message msg = new Message();
                        msg.setContactPhone(senderPhone);
                        msg.setBody(body);
                        msg.setFromMe(false); // Incoming message
                        msg.setTimestamp(new Timestamp(timestamp));
                        msg.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                        msg.setWhatsappMessageId(messageId);

                        messageRepository.save(msg);
                        messagesProcessed++;

                        System.out.println("✓ Message saved to database (all messages saved, including duplicates)");
                        System.out.println("  - Database ID: " + msg.getId() + " (unique for each save)");
                        System.out.println("  - From: " + senderPhone + " (" + senderName + ")");
                        System.out.println("  - Type: " + type);
                        System.out.println("  - Body: " + (body.length() > 100 ? body.substring(0, 100) + "..." : body));
                        System.out.println("  - WhatsApp Msg ID: " + messageId);
                        System.out.println("  - Timestamp: " + new Timestamp(timestamp));
                    }
                }
            }

            System.out.println("=== Webhook Processing Complete ===");
            System.out.println("Total messages processed: " + messagesProcessed + "\n");
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            System.err.println("ERROR processing webhook: " + e.getMessage());
            e.printStackTrace();
            // Still return 200 to prevent WhatsApp from retrying
            return ResponseEntity.ok("Error processed");
        }
    }

    // Helper method to extract message body based on type
    private String extractMessageBody(Map<String, Object> message, String type) {
        if (type == null) {
            return "[Unknown message type]";
        }

        switch (type) {
            case "text":
                @SuppressWarnings("unchecked")
                Map<String, Object> textObj = (Map<String, Object>) message.get("text");
                return textObj != null ? (String) textObj.get("body") : "[Empty text]";

            case "image":
                @SuppressWarnings("unchecked")
                Map<String, Object> imageObj = (Map<String, Object>) message.get("image");
                if (imageObj != null) {
                    String caption = (String) imageObj.get("caption");
                    return caption != null ? "[Image] " + caption : "[Image]";
                }
                return "[Image]";

            case "document":
                @SuppressWarnings("unchecked")
                Map<String, Object> docObj = (Map<String, Object>) message.get("document");
                if (docObj != null) {
                    String filename = (String) docObj.get("filename");
                    String caption = (String) docObj.get("caption");
                    String display = filename != null ? filename : "Document";
                    return caption != null ? "[Document: " + display + "] " + caption : "[Document: " + display + "]";
                }
                return "[Document]";

            case "audio":
                return "[Audio message]";

            case "video":
                @SuppressWarnings("unchecked")
                Map<String, Object> videoObj = (Map<String, Object>) message.get("video");
                if (videoObj != null) {
                    String caption = (String) videoObj.get("caption");
                    return caption != null ? "[Video] " + caption : "[Video]";
                }
                return "[Video]";

            case "location":
                @SuppressWarnings("unchecked")
                Map<String, Object> locationObj = (Map<String, Object>) message.get("location");
                if (locationObj != null) {
                    Double latitude = (Double) locationObj.get("latitude");
                    Double longitude = (Double) locationObj.get("longitude");
                    return String.format("[Location] Lat: %s, Long: %s", latitude, longitude);
                }
                return "[Location]";

            case "button":
                @SuppressWarnings("unchecked")
                Map<String, Object> buttonObj = (Map<String, Object>) message.get("button");
                if (buttonObj != null) {
                    String text = (String) buttonObj.get("text");
                    return text != null ? "[Button Response] " + text : "[Button Response]";
                }
                return "[Button Response]";

            case "interactive":
                return "[Interactive message]";

            default:
                return "[Message type: " + type + "]";
        }
    }

    // Send message endpoint
    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody SendMessageRequest request) {
        try {
            // Save to DB first (outgoing)
            Message msg = new Message();
            msg.setContactPhone(request.phone());
            msg.setBody(request.message());
            msg.setFromMe(true);
            long now = System.currentTimeMillis();
            msg.setTimestamp(new Timestamp(now));
            msg.setCreatedAt(new Timestamp(now));
            messageRepository.save(msg);

            // Send via WhatsApp API
            String url = String.format("https://graph.facebook.com/%s/%s/messages", API_VERSION, PHONE_NUMBER_ID);

            String cleanPhone = request.phone().replaceAll("[^0-9]", "");

            String jsonBody = """
                    {
                        "messaging_product": "whatsapp",
                        "to": "%s",
                        "type": "text",
                        "text": {
                            "body": "%s"
                        }
                    }
                    """.formatted(cleanPhone, request.message().replace("\"", "\\\""));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(ACCESS_TOKEN);

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok("Message sent successfully to WhatsApp");
            } else {
                return ResponseEntity.status(response.getStatusCode())
                        .body("WhatsApp API error: " + response.getBody());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send message: " + e.getMessage());
        }
    }

    record SendMessageRequest(String phone, String message) {}
}