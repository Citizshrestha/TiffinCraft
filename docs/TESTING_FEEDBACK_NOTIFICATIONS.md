# Testing Guide: Customer Feedback Notifications to Cooks

## Quick Test Scenarios

### Scenario 1: Customer Submits New Review
**Steps:**
1. Log in as a customer who has a delivered/completed order
2. Navigate to order history
3. Select an order with status 'delivered' or 'completed'
4. Submit a review with rating and optional comment
5. Check cook's notification section

**Expected Result:**
- Cook receives notification: "New Review Received!"
- Message includes: customer name, star rating (⭐⭐⭐⭐⭐), and feedback comment
- Notification stored in database
- Real-time socket event fired (if cook is online)
- FCM push sent (if cook has FCM token)

**API Test (Postman/cURL):**
```bash
POST http://localhost:5000/api/reviews
Authorization: Bearer {customer_token}
Content-Type: application/json

{
  "order_id": 1,
  "cook_id": 2,
  "rating": 5,
  "comment": "Excellent food! Thanks for your service"
}
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Review submitted successfully."
}
```

---

### Scenario 2: Customer Updates Existing Review
**Steps:**
1. Log in as a customer who has already submitted a review
2. Navigate to their reviews
3. Edit an existing review (change rating or comment)
4. Save the changes
5. Check cook's notification section

**Expected Result:**
- Cook receives notification: "Review Updated"
- Message includes: customer name, updated star rating, and updated comment
- Notification stored in database
- Real-time socket event fired

**API Test (Postman/cURL):**
```bash
PUT http://localhost:5000/api/reviews/123
Authorization: Bearer {customer_token}
Content-Type: application/json

{
  "rating": 4,
  "comment": "Updated: Very good service!"
}
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Review updated successfully."
}
```

---

### Scenario 3: Real-time Notification (Socket.IO)
**Steps:**
1. Log in as a cook
2. Establish Socket.IO connection with authentication token
3. Join cook's user room: `user_{cook_id}`
4. Have a customer submit a review for that cook
5. Listen for `newNotification` event

**Socket.IO Event:**
```javascript
socket.on("newNotification", (data) => {
  console.log("Notification received:", data);
  // data structure:
  // {
  //   type: "review",
  //   title: "New Review Received!",
  //   message: "John Doe gave you 5 stars ⭐⭐⭐⭐⭐: 'Excellent food!'",
  //   reference_id: 123,
  //   reference_type: "review",
  //   created_at: "2026-08-30T12:34:56.789Z"
  // }
});
```

---

### Scenario 4: Verify Database Notification
**Steps:**
1. After customer submits review, check the `notifications` table
2. Query for cook's notifications

**SQL Query:**
```sql
SELECT * FROM notifications 
WHERE user_id = {cook_id} 
  AND type = 'review' 
ORDER BY created_at DESC 
LIMIT 5;
```

**Expected Fields:**
- `user_id`: Cook's ID
- `title`: "New Review Received!" or "Review Updated"
- `message`: Contains customer name, rating, stars, and comment
- `type`: "review"
- `reference_id`: Review ID
- `reference_type`: "review"
- `is_read`: 0 (false initially)
- `created_at`: Recent timestamp

---

### Scenario 5: FCM Push Notification
**Prerequisites:**
- Cook must have FCM token registered in `users.fcm_token`
- Firebase Admin SDK must be configured

**Steps:**
1. Ensure cook's device has FCM token registered
2. Customer submits a review
3. Check cook's mobile device for push notification

**Check Server Logs:**
Look for these log messages:
```
✅ Notification created for user {cook_id}: New Review Received!
📲 Push sent to user {cook_id}: New Review Received!
```

**If Push Fails:**
Look for error log:
```
❌ Push failed for user {cook_id}: {error_message}
```

---

## API Endpoint Reference

### GET /api/notifications
Get all notifications for logged-in user (cook)

**Request:**
```bash
GET http://localhost:5000/api/notifications
Authorization: Bearer {cook_token}
```

**Response:**
```json
{
  "success": true,
  "notifications": [
    {
      "id": 123,
      "user_id": 2,
      "title": "New Review Received!",
      "message": "John Doe gave you 5 stars ⭐⭐⭐⭐⭐: 'Excellent food!'",
      "type": "review",
      "reference_id": 456,
      "reference_type": "review",
      "is_read": false,
      "created_at": "2026-08-30T12:34:56.000Z"
    }
  ]
}
```

### PUT /api/notifications/:id/read
Mark notification as read

**Request:**
```bash
PUT http://localhost:5000/api/notifications/123/read
Authorization: Bearer {cook_token}
```

**Response:**
```json
{
  "success": true,
  "message": "Notification marked as read"
}
```

### GET /api/notifications/unread-count
Get count of unread notifications

**Request:**
```bash
GET http://localhost:5000/api/notifications/unread-count
Authorization: Bearer {cook_token}
```

**Response:**
```json
{
  "success": true,
  "unread_count": 3
}
```

---

## Edge Cases to Test

### 1. Review Without Comment
**Test:** Submit review with only rating, no comment
```json
{
  "order_id": 1,
  "cook_id": 2,
  "rating": 5
}
```
**Expected:** Notification message doesn't include comment part

---

### 2. Multiple Reviews in Quick Succession
**Test:** Multiple customers review same cook within seconds
**Expected:** Each review creates separate notification

---

### 3. Cook Offline During Review Submission
**Test:** Submit review when cook is not connected via Socket.IO
**Expected:** 
- Notification stored in database
- FCM push sent
- Cook sees notification when they next open the app

---

### 4. Review on Non-Delivered Order
**Test:** Try to submit review on order with status 'pending' or 'confirmed'
```json
{
  "order_id": 999,
  "cook_id": 2,
  "rating": 5
}
```
**Expected:** Error response:
```json
{
  "success": false,
  "message": "Order not found or not delivered yet."
}
```

---

### 5. Duplicate Review
**Test:** Submit review twice for same order
**Expected:** Second attempt returns error:
```json
{
  "success": false,
  "message": "You have already reviewed this order."
}
```

---

## Debugging Checklist

If notifications are not working:

1. **Check server logs** for notification creation confirmation
   ```
   ✅ Notification created for user {cook_id}: New Review Received!
   ```

2. **Verify database entry**
   ```sql
   SELECT * FROM notifications 
   WHERE user_id = {cook_id} 
   ORDER BY created_at DESC LIMIT 1;
   ```

3. **Check Socket.IO connection**
   - Is cook connected?
   - Did cook join their user room (`user_{cook_id}`)?
   - Check server logs for: `🔌 Socket connected: {socket_id} (User: {cook_id})`

4. **Verify FCM token**
   ```sql
   SELECT fcm_token FROM users WHERE id = {cook_id};
   ```
   - Should not be NULL if push notifications expected

5. **Check Firebase configuration**
   - Is Firebase Admin SDK initialized?
   - Are credentials valid?
   - Check server logs for: `✅ Firebase Admin SDK initialized`

6. **Test notification helper directly**
   ```javascript
   import { notifyNewReview } from './utils/notificationHelper.js';
   await notifyNewReview(cookId, reviewId, "Test Customer", 5);
   ```

---

## Performance Considerations

- Notification creation is async (fire-and-forget for FCM push)
- Database transaction commits before notification sent
- Review submission should not be blocked by notification failures
- Socket.IO events are non-blocking

---

## Security Notes

- Only authenticated customers can submit reviews
- Customers can only review their own delivered/completed orders
- Customers cannot review same order twice
- Socket.IO requires authentication token
- Cook-specific rooms ensure notifications go to correct user

---

## Mobile App Checklist

For Android developers implementing this feature:

- [ ] Implement Socket.IO client connection with JWT token
- [ ] Join user room on connection: `user_{cook_id}`
- [ ] Listen for `newNotification` events
- [ ] Register FCM token with backend on login
- [ ] Handle FCM push notifications
- [ ] Display notifications in NotificationBell component
- [ ] Allow navigation to review details on tap
- [ ] Update notification badge count
- [ ] Mark notifications as read when viewed
- [ ] Handle offline notification queue

---

## Support

For issues or questions:
1. Check server logs in `backend/logs/`
2. Review database notifications table
3. Test Socket.IO connection separately
4. Verify FCM token registration
5. Check Firebase Admin SDK initialization
