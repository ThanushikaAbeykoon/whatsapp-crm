# WhatsApp CRM API Documentation

**Base URL:** `http://your-server:8080/api`

All endpoints are prefixed with `/api`

---

## 📨 Message Endpoints

### 1. Get All Messages

**Endpoint:** `GET /api/messages`

**Description:** Retrieves all messages from the database. Supports pagination and sorting.

**Query Parameters:**
- `page` (optional, integer): Page number (0-indexed). Required if using pagination.
- `size` (optional, integer): Number of items per page. Required if using pagination.
- `sortBy` (optional, string): Field to sort by. Default: `"timestamp"`
- `sortDir` (optional, string): Sort direction. Options: `"ASC"` or `"DESC"`. Default: `"DESC"`

**Response (without pagination):**
```json
[
  {
    "id": 1,
    "whatsappMessageId": "wamid.xxx",
    "contactPhone": "1234567890",
    "body": "Hello!",
    "fromMe": false,
    "timestamp": "2024-01-15T10:30:00",
    "createdAt": "2024-01-15T10:30:00"
  }
]
```

**Response (with pagination):**
```json
{
  "content": [
    {
      "id": 1,
      "whatsappMessageId": "wamid.xxx",
      "contactPhone": "1234567890",
      "body": "Hello!",
      "fromMe": false,
      "timestamp": "2024-01-15T10:30:00",
      "createdAt": "2024-01-15T10:30:00"
    }
  ],
  "totalElements": 100,
  "totalPages": 10,
  "currentPage": 0,
  "pageSize": 10
}
```

**Example Requests:**
```bash
# Get all messages
curl http://localhost:8080/api/messages

# Get paginated messages (first page, 10 items)
curl "http://localhost:8080/api/messages?page=0&size=10"

# Get messages sorted by timestamp ascending
curl "http://localhost:8080/api/messages?page=0&size=10&sortBy=timestamp&sortDir=ASC"
```

---

### 2. Get Message by ID

**Endpoint:** `GET /api/messages/{id}`

**Description:** Retrieves a specific message by its ID.

**Path Parameters:**
- `id` (required, Long): The message ID

**Response (200 OK):**
```json
{
  "id": 1,
  "whatsappMessageId": "wamid.xxx",
  "contactPhone": "1234567890",
  "body": "Hello!",
  "fromMe": false,
  "timestamp": "2024-01-15T10:30:00",
  "createdAt": "2024-01-15T10:30:00"
}
```

**Response (404 Not Found):**
```
(empty body)
```

**Example Request:**
```bash
curl http://localhost:8080/api/messages/1
```

---

### 3. Get Messages by Phone Number

**Endpoint:** `GET /api/messages/phone/{phone}`

**Description:** Retrieves all messages for a specific phone number.

**Path Parameters:**
- `phone` (required, string): Phone number to search for

**Response:**
```json
[
  {
    "id": 1,
    "whatsappMessageId": "wamid.xxx",
    "contactPhone": "1234567890",
    "body": "Hello!",
    "fromMe": false,
    "timestamp": "2024-01-15T10:30:00",
    "createdAt": "2024-01-15T10:30:00"
  },
  {
    "id": 2,
    "whatsappMessageId": "wamid.yyy",
    "contactPhone": "1234567890",
    "body": "How are you?",
    "fromMe": true,
    "timestamp": "2024-01-15T10:35:00",
    "createdAt": "2024-01-15T10:35:00"
  }
]
```

**Example Request:**
```bash
curl http://localhost:8080/api/messages/phone/1234567890
```

---

### 4. Search Messages

**Endpoint:** `GET /api/messages/search`

**Description:** Search messages by phone number or date range.

**Query Parameters:**
- `phone` (optional, string): Filter by phone number
- `startDate` (optional, string): Start date in format `YYYY-MM-DD HH:MM:SS` (required if using date range)
- `endDate` (optional, string): End date in format `YYYY-MM-DD HH:MM:SS` (required if using date range)

**Note:** Either `phone` OR both `startDate` and `endDate` must be provided.

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "whatsappMessageId": "wamid.xxx",
    "contactPhone": "1234567890",
    "body": "Hello!",
    "fromMe": false,
    "timestamp": "2024-01-15T10:30:00",
    "createdAt": "2024-01-15T10:30:00"
  }
]
```

**Response (400 Bad Request):**
```
(empty body)
```

**Example Requests:**
```bash
# Search by phone
curl "http://localhost:8080/api/messages/search?phone=1234567890"

# Search by date range
curl "http://localhost:8080/api/messages/search?startDate=2024-01-15 00:00:00&endDate=2024-01-15 23:59:59"
```

---

## 👥 Contact Endpoints

### 5. Get All Contacts

**Endpoint:** `GET /api/contacts`

**Description:** Retrieves all contacts from the database.

**Response:**
```json
[
  {
    "id": 1,
    "phone": "1234567890",
    "name": "John Doe",
    "createdAt": "2024-01-15T10:30:00"
  },
  {
    "id": 2,
    "phone": "0987654321",
    "name": "Jane Smith",
    "createdAt": "2024-01-15T11:00:00"
  }
]
```

**Example Request:**
```bash
curl http://localhost:8080/api/contacts
```

---

### 6. Get Contact by Phone Number

**Endpoint:** `GET /api/contacts/phone/{phone}`

**Description:** Retrieves a contact by their phone number.

**Path Parameters:**
- `phone` (required, string): Phone number to search for

**Response (200 OK):**
```json
{
  "id": 1,
  "phone": "1234567890",
  "name": "John Doe",
  "createdAt": "2024-01-15T10:30:00"
}
```

**Response (404 Not Found):**
```
(empty body)
```

**Example Request:**
```bash
curl http://localhost:8080/api/contacts/phone/1234567890
```

---

## 📤 Send Message Endpoint

### 7. Send WhatsApp Message

**Endpoint:** `POST /api/send`

**Description:** Sends a WhatsApp message to a phone number. The message is saved to the database and sent via WhatsApp Cloud API.

**Request Body:**
```json
{
  "phone": "1234567890",
  "message": "Hello! This is a test message."
}
```

**Request Body Fields:**
- `phone` (required, string): Recipient phone number (can include formatting, will be cleaned)
- `message` (required, string): Message text to send

**Response (200 OK):**
```
Message sent to WhatsApp!
```

**Response (500 Internal Server Error):**
```
Error: [error message]
```
or
```
WhatsApp API error: [error details]
```

**Example Request:**
```bash
curl -X POST http://localhost:8080/api/send \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "1234567890",
    "message": "Hello from WhatsApp CRM!"
  }'
```

**JavaScript/Fetch Example:**
```javascript
fetch('http://localhost:8080/api/send', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    phone: '1234567890',
    message: 'Hello from WhatsApp CRM!'
  })
})
.then(response => response.text())
.then(data => console.log(data))
.catch(error => console.error('Error:', error));
```

---

## 🔗 Webhook Endpoints

### 8. Webhook Verification (GET)

**Endpoint:** `GET /api/webhook`

**Description:** This endpoint is used by Meta/WhatsApp to verify your webhook during setup. You don't call this directly - Meta calls it automatically.

**Query Parameters (sent by Meta):**
- `hub.mode` (required): Should be `"subscribe"`
- `hub.verify_token` (required): Must match your configured token (`Testtoken12345`)
- `hub.challenge` (required): Random string sent by Meta

**Response (200 OK):**
```
[hub.challenge value]
```

**Response (403 Forbidden):**
```
Verification failed
```

**Note:** This endpoint is automatically called by Meta when you configure the webhook in Meta Developer Console.

---

### 9. Receive Webhook (POST)

**Endpoint:** `POST /api/webhook`

**Description:** Receives incoming WhatsApp messages from Meta. This endpoint is called automatically by Meta when messages are received.

**Request Body (from Meta):**
```json
{
  "object": "whatsapp_business_account",
  "entry": [
    {
      "id": "WHATSAPP_BUSINESS_ACCOUNT_ID",
      "changes": [
        {
          "value": {
            "messaging_product": "whatsapp",
            "metadata": {
              "display_phone_number": "1234567890",
              "phone_number_id": "PHONE_NUMBER_ID"
            },
            "contacts": [
              {
                "profile": {
                  "name": "John Doe"
                },
                "wa_id": "1234567890"
              }
            ],
            "messages": [
              {
                "from": "1234567890",
                "id": "wamid.xxx",
                "timestamp": "1234567890",
                "text": {
                  "body": "Hello!"
                },
                "type": "text"
              }
            ]
          },
          "field": "messages"
        }
      ]
    }
  ]
}
```

**Response:**
```
OK
```

**Note:** 
- This endpoint is called automatically by Meta
- Messages are automatically saved to the database
- Contacts are automatically created/updated
- Always returns `200 OK` to prevent Meta from retrying

---

## 📊 Data Models

### Message Object
```json
{
  "id": 1,                          // Long - Auto-generated ID
  "whatsappMessageId": "wamid.xxx", // String - WhatsApp message ID
  "contactPhone": "1234567890",     // String - Phone number
  "body": "Message text",           // String - Message content
  "fromMe": true,                   // Boolean - true if sent by you, false if received
  "timestamp": "2024-01-15T10:30:00", // Timestamp - Message timestamp
  "createdAt": "2024-01-15T10:30:00"  // Timestamp - Record creation time
}
```

### Contact Object
```json
{
  "id": 1,                          // Long - Auto-generated ID
  "phone": "1234567890",            // String - Phone number (unique)
  "name": "John Doe",               // String - Contact name
  "createdAt": "2024-01-15T10:30:00" // Timestamp - Record creation time
}
```

### Send Message Request
```json
{
  "phone": "1234567890",            // String - Recipient phone number
  "message": "Your message text"    // String - Message to send
}
```

---

## 🔒 CORS Configuration

The API currently allows requests from:
- `http://localhost:3000`

To update CORS settings, modify the `@CrossOrigin` annotation in `MessageController.java`.

---

## ⚠️ Error Responses

### 400 Bad Request
- Invalid request parameters
- Missing required fields
- Invalid date format

### 404 Not Found
- Resource not found (message/contact ID doesn't exist)

### 403 Forbidden
- Webhook verification failed (invalid token)

### 500 Internal Server Error
- Server error
- Database connection issues
- WhatsApp API errors

---

## 🧪 Testing Examples

### Complete Testing Workflow

```bash
# 1. Get all messages
curl http://localhost:8080/api/messages

# 2. Get all contacts
curl http://localhost:8080/api/contacts

# 3. Get messages for a specific phone
curl http://localhost:8080/api/messages/phone/1234567890

# 4. Get contact by phone
curl http://localhost:8080/api/contacts/phone/1234567890

# 5. Send a message
curl -X POST http://localhost:8080/api/send \
  -H "Content-Type: application/json" \
  -d '{"phone":"1234567890","message":"Test message"}'

# 6. Search messages by date range
curl "http://localhost:8080/api/messages/search?startDate=2024-01-15 00:00:00&endDate=2024-01-15 23:59:59"

# 7. Get paginated messages
curl "http://localhost:8080/api/messages?page=0&size=10&sortBy=timestamp&sortDir=DESC"
```

---

## 📝 Notes

1. **Phone Number Format:** Phone numbers are stored as strings. The send endpoint automatically removes non-numeric characters before sending to WhatsApp.

2. **Date Format:** When using date range search, use format: `YYYY-MM-DD HH:MM:SS` (e.g., `2024-01-15 10:30:00`)

3. **Webhook Setup:** The webhook endpoints (`/api/webhook`) are automatically called by Meta. You only need to configure them in Meta Developer Console.

4. **Message Storage:** All sent and received messages are automatically stored in the database.

5. **Contact Creation:** Contacts are automatically created when messages are received from new phone numbers.
