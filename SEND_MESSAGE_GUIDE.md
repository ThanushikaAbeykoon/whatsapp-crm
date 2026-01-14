# Send Message Guide - Using WhatsApp API

## Overview

This guide explains how to send messages using your WhatsApp Business number and how contacts/messages are saved to the database.

---

## How to Send Messages

### API Endpoint

**POST** `/api/send`

### Request Body

```json
{
  "phone": "1234567890",
  "message": "Hello! This is a test message."
}
```

### Example Request

**Using cURL:**
```bash
curl -X POST http://your-server:8080/api/send \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "1234567890",
    "message": "Hello from WhatsApp CRM!"
  }'
```

**Using JavaScript/Fetch:**
```javascript
fetch('http://your-server:8080/api/send', {
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

**Using Postman:**
1. Method: POST
2. URL: `http://your-server:8080/api/send`
3. Headers: `Content-Type: application/json`
4. Body (raw JSON):
```json
{
  "phone": "1234567890",
  "message": "Your message here"
}
```

---

## What Happens When You Send a Message

### Step-by-Step Process

1. **Contact Creation/Update**
   - System checks if contact exists in database
   - If contact doesn't exist → Creates new contact with phone number
   - If contact exists → Uses existing contact
   - Contact is linked to the message

2. **Message Saved to Database**
   - Message is saved BEFORE sending to WhatsApp
   - `fromMe = true` (marks as outgoing message)
   - Contact phone number is stored
   - Message body is stored
   - Timestamp is recorded

3. **Message Sent via WhatsApp API**
   - Message is sent to WhatsApp Cloud API
   - Uses your configured WhatsApp Business number
   - Phone number is cleaned (removes non-numeric characters)

4. **WhatsApp Message ID Updated**
   - If successful, WhatsApp returns a message ID
   - Message ID is saved to database (if available)

---

## What Gets Saved to Database

### Contact Table

When you send a message to a phone number:

**If contact doesn't exist:**
```sql
INSERT INTO contact (phone, name, created_at)
VALUES ('1234567890', '1234567890', NOW());
```

**If contact exists:**
- Uses existing contact record
- No changes made (unless you update contact name separately)

### Message Table

Every sent message is saved:

```sql
INSERT INTO message (
    contact_phone,
    body,
    from_me,
    timestamp,
    created_at,
    whatsapp_message_id
)
VALUES (
    '1234567890',           -- Recipient phone number
    'Hello!',                -- Message content
    true,                    -- from_me = true (outgoing)
    NOW(),                  -- When message was sent
    NOW(),                  -- When saved to database
    'wamid.xxx'             -- WhatsApp message ID (if available)
);
```

---

## Phone Number Format

### Accepted Formats

The system automatically cleans phone numbers:

- `1234567890` ✅
- `+1 234-567-890` ✅ (cleaned to `1234567890`)
- `(123) 456-7890` ✅ (cleaned to `1234567890`)
- `+91 12345 67890` ✅ (cleaned to `911234567890`)

**Important:** 
- Phone numbers should include country code
- Example: For India, use `911234567890` (not `1234567890`)
- For US, use `1234567890` or `11234567890`

---

## Response Examples

### Success Response

```json
"Message sent successfully to WhatsApp"
```

**Logs will show:**
```
=== Sending Message ===
To: 1234567890
Message: Hello!
✓ New contact created: 1234567890
✓ Message saved to database
  - Database ID: 1
  - To: 1234567890
  - From Me: true (outgoing)
✓ WhatsApp Message ID updated: wamid.xxx
=== Message Sent Successfully ===
```

### Error Response

```json
{
  "error": "WhatsApp API error: [error details]"
}
```

---

## Checking Sent Messages

### Get All Messages (Including Sent)

```bash
GET http://your-server:8080/api/messages
```

**Response includes:**
- Incoming messages (`fromMe: false`)
- Outgoing messages (`fromMe: true`)

### Get Messages for a Specific Contact

```bash
GET http://your-server:8080/api/messages/phone/1234567890
```

This returns ALL messages (both sent and received) for that contact.

### Get All Contacts

```bash
GET http://your-server:8080/api/contacts
```

---

## Important Notes

### 1. Contact Creation
- Contacts are **automatically created** when you send a message
- Contact name defaults to phone number (can be updated later)
- If contact already exists, it's reused

### 2. Message History
- **All sent messages** are saved to database
- Messages are marked with `fromMe = true`
- Complete conversation history is preserved

### 3. WhatsApp Business Number
- Uses the phone number configured in `application.properties`
- Phone Number ID: `whatsapp.phone-number-id`
- Access Token: `whatsapp.access-token`

### 4. Message Status
- Messages are saved **before** sending to WhatsApp
- If WhatsApp API fails, message is still saved in database
- WhatsApp message ID is updated after successful send

---

## Example Workflow

### Scenario: Send message to a new contact

1. **Send Message:**
   ```bash
   POST /api/send
   {
     "phone": "1234567890",
     "message": "Hello!"
   }
   ```

2. **What Happens:**
   - Contact `1234567890` is created in database
   - Message is saved with `fromMe = true`
   - Message is sent via WhatsApp API
   - WhatsApp message ID is saved (if available)

3. **Check Results:**
   ```bash
   # Get contact
   GET /api/contacts/phone/1234567890
   
   # Get messages
   GET /api/messages/phone/1234567890
   ```

---

## Troubleshooting

### Message Not Sending

1. **Check WhatsApp Credentials:**
   - Verify `whatsapp.phone-number-id` is correct
   - Verify `whatsapp.access-token` is valid
   - Check token expiration

2. **Check Phone Number:**
   - Ensure phone number includes country code
   - Verify phone number format is correct
   - Check if recipient has opted in

3. **Check Logs:**
   - Look for error messages in application logs
   - Check WhatsApp API error responses

### Contact Not Created

- Contacts are created automatically when sending
- Check database connection
- Verify contact table exists
- Check application logs for errors

### Message Not Saved

- Messages are saved before sending
- Check database connection
- Verify message table exists
- Check for constraint violations

---

## Summary

✅ **Contacts are automatically created/updated** when sending messages  
✅ **All sent messages are saved** to database with `fromMe = true`  
✅ **WhatsApp message IDs are saved** (if available)  
✅ **Complete conversation history** is preserved  
✅ **Phone numbers are automatically cleaned** (non-numeric removed)

Your WhatsApp CRM now tracks both incoming and outgoing messages! 🎉
