# Next Steps After Backend Deployment

## ✅ Checklist

### 1. **Verify Backend is Running**
- [ ] Check that your backend is accessible at `http://your-server:8080`
- [ ] Test health endpoint (if available) or try accessing `http://your-server:8080/api/messages`
- [ ] Verify MySQL database is accessible and tables are created

### 2. **Configure WhatsApp Webhook in Meta Developer Console**

Your webhook endpoint is: `https://your-deployed-url/api/webhook`

**Steps:**
1. Go to [Meta for Developers](https://developers.facebook.com/)
2. Navigate to your WhatsApp app
3. Go to **Configuration** → **Webhooks**
4. Click **Edit** or **Add Callback URL**
5. Enter your webhook URL: `https://your-deployed-url/api/webhook`
6. Enter Verify Token: `Testtoken12345` (from your application.properties)
7. Subscribe to **messages** field
8. Click **Verify and Save**

**Important:** 
- Your webhook URL must be publicly accessible (HTTPS required)
- If testing locally, use a tunneling service like ngrok: `ngrok http 8080`
- The verify token must match exactly: `Testtoken12345`

### 3. **Update CORS Configuration (if needed)**

If you're deploying a frontend, update the CORS origin in `MessageController.java`:
- Current: `@CrossOrigin(origins = "http://localhost:3000")`
- Update to your frontend URL: `@CrossOrigin(origins = "https://your-frontend-url.com")`

### 4. **Test API Endpoints**

Test your deployed backend with these endpoints:

**Get all messages:**
```bash
GET http://your-server:8080/api/messages
```

**Get messages with pagination:**
```bash
GET http://your-server:8080/api/messages?page=0&size=10
```

**Get all contacts:**
```bash
GET http://your-server:8080/api/contacts
```

**Send a WhatsApp message:**
```bash
POST http://your-server:8080/api/send
Content-Type: application/json

{
  "phone": "1234567890",
  "message": "Hello from CRM!"
}
```

### 5. **Set Up Frontend (if applicable)**

Your backend expects a frontend at `http://localhost:3000`. You can:

**Option A: Create a React/Next.js frontend**
- Create a new frontend project
- Connect it to your backend API
- Update CORS in backend to match frontend URL

**Option B: Use existing frontend**
- Update frontend API base URL to point to your deployed backend
- Ensure CORS is configured correctly

### 6. **Environment-Specific Configuration**

For production deployment, consider:

**Update `application.properties` for production:**
- Change database connection to production database
- Set `spring.jpa.hibernate.ddl-auto=validate` (instead of `update`)
- Set `spring.jpa.show-sql=false`
- Use environment variables for sensitive data (access tokens, passwords)

**Security Recommendations:**
- Never commit access tokens to version control
- Use environment variables or secrets management
- Enable HTTPS
- Configure proper database credentials

### 7. **Monitor and Test**

- [ ] Test webhook verification (Meta will send a GET request)
- [ ] Send a test message via `/api/send` endpoint
- [ ] Verify incoming messages are being received and saved
- [ ] Check database to ensure messages are being stored
- [ ] Monitor application logs for errors

### 8. **Troubleshooting**

**Webhook verification fails:**
- Check that your server is publicly accessible
- Verify the verify token matches exactly
- Ensure HTTPS is enabled (required by Meta)

**Messages not being received:**
- Check webhook subscription in Meta Console
- Verify webhook URL is correct
- Check application logs for errors
- Ensure database connection is working

**CORS errors:**
- Update `@CrossOrigin` annotation with correct frontend URL
- Or configure CORS globally in Spring Security

## Quick Test Commands

```bash
# Test if backend is running
curl http://your-server:8080/api/messages

# Test webhook verification (simulate Meta's request)
curl "http://your-server:8080/api/webhook?hub.mode=subscribe&hub.verify_token=Testtoken12345&hub.challenge=test123"

# Test sending a message
curl -X POST http://your-server:8080/api/send \
  -H "Content-Type: application/json" \
  -d '{"phone":"1234567890","message":"Test message"}'
```

## Need Help?

- Check application logs for detailed error messages
- Verify all configuration values in `application.properties`
- Ensure MySQL database is running and accessible
- Confirm WhatsApp Cloud API credentials are valid
