# Webhook Setup Guide - Receiving Messages

## ✅ Current Status

Your webhook verification is **OK**, which means:
- ✅ Meta/WhatsApp can reach your server
- ✅ Your webhook endpoint is publicly accessible
- ✅ The verify token is correct

## 📋 Next Steps to Receive Messages

### 1. **Verify Webhook Subscription in Meta Console**

Even though verification passed, you need to ensure the webhook is **subscribed** to receive messages:

1. Go to [Meta for Developers](https://developers.facebook.com/)
2. Navigate to your WhatsApp app
3. Go to **Configuration** → **Webhooks**
4. Check that your webhook is **subscribed** to the **"messages"** field
5. If not subscribed, click **"Manage"** or **"Subscribe"** next to the messages field

**Important:** The webhook must be subscribed to receive message events!

---

### 2. **Test Message Reception**

#### Option A: Send a Test Message from WhatsApp

1. Open WhatsApp on your phone
2. Send a message to your WhatsApp Business number (the one connected to your app)
3. Check your application logs - you should see:
   ```
   === Webhook Received ===
   Processing 1 message(s)
   Processing message from: [phone number]
   Message ID: [message id]
   ✓ Message saved to database - ID: [id], From: [phone]
   ```

#### Option B: Use Meta's Test Tool

1. In Meta Developer Console → **Configuration** → **Webhooks**
2. Click **"Test"** button next to your webhook
3. This will send a test webhook to your endpoint

---

### 3. **Verify Messages Are Being Saved**

Check your database or use the API:

```bash
# Get all messages
curl http://your-server:8080/api/messages

# Get all contacts
curl http://your-server:8080/api/contacts

# Get messages for a specific phone
curl http://your-server:8080/api/messages/phone/[phone-number]
```

---

### 4. **Monitor Application Logs**

Watch your application logs in real-time. When a message is received, you'll see:

```
=== Webhook Received ===
Request received at: [timestamp]
Processing 1 message(s)
Processing message from: 1234567890
Message ID: wamid.xxx
✓ Message saved to database - ID: 1, From: 1234567890
Successfully processed 1 message(s)
=== Webhook Processing Complete ===
```

---

## 🔍 Troubleshooting

### Problem: Messages Not Being Received

**Check 1: Webhook Subscription**
- ✅ Go to Meta Console → Webhooks
- ✅ Verify "messages" field is **subscribed** (not just verified)
- ✅ If not subscribed, click "Subscribe"

**Check 2: Application Logs**
- Check if webhook requests are being received
- Look for error messages in logs
- Verify database connection is working

**Check 3: Database Connection**
```bash
# Test health endpoint (checks database)
curl http://your-server:8080/api/health
```

**Check 4: Webhook URL Accessibility**
- Ensure your server is publicly accessible
- Test webhook URL manually:
  ```bash
  curl -X POST https://your-server:8080/api/webhook \
    -H "Content-Type: application/json" \
    -d '{"object":"whatsapp_business_account","entry":[]}'
  ```

**Check 5: Phone Number Status**
- In Meta Console → **Phone Numbers**
- Verify your phone number is **connected** and **active**
- Check if there are any restrictions

---

### Problem: Messages Received But Not Saved

**Check Application Logs:**
- Look for error messages when processing
- Check database connection errors
- Verify table structure is correct

**Check Database:**
```sql
-- Check if messages table exists
SHOW TABLES;

-- Check message table structure
DESCRIBE message;

-- Check if messages are being saved
SELECT * FROM message ORDER BY created_at DESC LIMIT 10;
```

---

### Problem: Duplicate Messages

The system has duplicate prevention built-in:
- Messages are checked by `whatsappMessageId` before saving
- If a message with the same ID exists, it won't be saved again

---

## 📊 What Happens When a Message is Received?

1. **Webhook Receives Request**
   - Meta sends POST request to `/api/webhook`
   - Request contains message data in JSON format

2. **Message Processing**
   - Extracts message content (text, image, document)
   - Gets sender phone number
   - Gets contact information (name, etc.)

3. **Contact Management**
   - Creates new contact if phone number doesn't exist
   - Updates contact name if it has changed

4. **Message Storage**
   - Saves message to database
   - Marks as `fromMe = false` (incoming message)
   - Stores WhatsApp message ID to prevent duplicates

5. **Response**
   - Returns `200 OK` to Meta (prevents retries)

---

## 🧪 Testing Checklist

- [ ] Webhook verification successful
- [ ] Webhook subscribed to "messages" field
- [ ] Phone number connected in Meta Console
- [ ] Application logs show webhook requests
- [ ] Messages appear in database
- [ ] Contacts are created automatically
- [ ] API endpoints return messages correctly

---

## 📝 Quick Test Commands

```bash
# 1. Check if backend is running
curl http://your-server:8080/

# 2. Check database health
curl http://your-server:8080/api/health

# 3. Get all messages (should show received messages)
curl http://your-server:8080/api/messages

# 4. Get all contacts (should show contacts from received messages)
curl http://your-server:8080/api/contacts

# 5. Send a test message (to trigger a response)
curl -X POST http://your-server:8080/api/send \
  -H "Content-Type: application/json" \
  -d '{"phone":"[recipient-phone]","message":"Test message"}'
```

---

## 🎯 Expected Behavior

When everything is working correctly:

1. **Send a message TO your WhatsApp Business number** from any WhatsApp account
2. **Within seconds**, you should see in logs:
   - Webhook received
   - Message processed
   - Message saved to database

3. **Query the API**:
   ```bash
   curl http://your-server:8080/api/messages
   ```
   You should see the received message in the response

4. **Check contacts**:
   ```bash
   curl http://your-server:8080/api/contacts
   ```
   The sender should appear as a contact

---

## ⚠️ Important Notes

1. **Message Direction:**
   - Messages sent TO your WhatsApp Business number = Incoming (saved automatically)
   - Messages sent FROM your app = Outgoing (use `/api/send` endpoint)

2. **Webhook Events:**
   - The webhook receives ALL message events (sent, delivered, read, etc.)
   - The code filters for actual message content
   - Status updates (delivered, read) won't create message records

3. **Phone Number Format:**
   - WhatsApp sends phone numbers in international format (e.g., "1234567890")
   - No country code prefix needed in most cases

4. **Rate Limits:**
   - Meta has rate limits for webhook calls
   - If you receive many messages quickly, they may be batched

---

## 🆘 Still Not Working?

1. **Check Meta Console Webhook Logs:**
   - Go to Meta Console → Webhooks → View Logs
   - See if requests are being sent
   - Check for error responses

2. **Enable Debug Logging:**
   - Check application.properties for logging levels
   - Set `logging.level.com.example.whatsapp_crm=DEBUG` for more details

3. **Test Webhook Manually:**
   - Use Postman or curl to send a test webhook payload
   - Verify your endpoint processes it correctly

4. **Check Server Logs:**
   - Look for any exceptions or errors
   - Verify database connection is stable

---

## ✅ Success Indicators

You'll know it's working when:
- ✅ Webhook verification shows green checkmark in Meta Console
- ✅ Webhook subscription shows "Subscribed" for messages field
- ✅ Application logs show "Webhook Received" when messages arrive
- ✅ Messages appear in database via API calls
- ✅ Contacts are automatically created

Your system is now ready to receive and store WhatsApp messages! 🎉
