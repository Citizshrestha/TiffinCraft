# Customer Feedback Notification Feature

## Overview
This document describes the implementation of the feature where cooks receive notifications when customers send feedback (reviews) on received/delivered meals.

## Implementation Date
Implemented: August 30, 2026

## Feature Description
When a customer submits a review/feedback for a delivered or completed order, the cook associated with that order will automatically receive:
1. **In-app notification** - Stored in the database and visible in the notifications section
2. **Real-time notification** - Delivered via Socket.IO for immediate visibility
3. **Push notification** - Sent via Firebase Cloud Messaging (FCM) to the cook's mobile device

## Changes Made

### 1. Updated Review Controller
**File**: `backend/controllers/reviewController.js`

#### Added Import
```javascript
import { notifyNewReview } from "../utils/notificationHelper.js";
```

#### Modified `addReview` Function
- Captures the `reviewId` after inserting the review
- Fetches customer name for personalized notification
- Calls `notifyNewReview()` to create a database notification and send FCM push
- Emits real-time Socket.IO event to cook with review details including feedback message
- Updated order status check to include both 'delivered' and 'completed' orders

**Key additions:**
```javascript
// Get customer name for notification
const [customer] = await connection.query(
    "SELECT full_name FROM users WHERE id = ?",
    [customerId]
);

// Send notification to cook about the new review with feedback
const customerName = customer.length > 0 ? customer[0].full_name : "A customer";
await notifyNewReview(cook_id, reviewId, customerName, rating);

// Emit real-time socket event for the cook
const io = req.app.get("io");
if (io) {
    io.to(`user_${cook_id}`).emit("newNotification", {
        type: "review",
        title: "New Review Received!",
        message: `${customerName} gave you ${rating} stars${'⭐'.repeat(rating)}${comment ? `: "${comment}"` : ''}`,
        reference_id: reviewId,
        reference_type: "review",
        created_at: new Date().toISOString()
    });
}
```

#### Modified `updateReview` Function
- Similar implementation for when customers update their existing reviews
- Notifies cook about review updates with the new rating and feedback message
- Provides context that the review was "updated" rather than newly created

## How It Works

### Flow Diagram
```
Customer submits review
        ↓
Review stored in database
        ↓
Cook's average rating updated
        ↓
┌───────────────────────────────┐
│  Three notification channels  │
├───────────────────────────────┤
│ 1. Database notification      │ ← Stored in `notifications` table
│ 2. Socket.IO real-time event  │ ← Instant delivery if cook is online
│ 3. FCM push notification      │ ← Reaches cook's mobile device
└───────────────────────────────┘
        ↓
Cook receives notification with:
- Customer name
- Star rating (1-5)
- Star emojis (⭐⭐⭐⭐⭐)
- Feedback comment (if provided)
```

### Notification Content

#### For New Reviews
- **Title**: "New Review Received!"
- **Message**: "{Customer Name} gave you {rating} stars ⭐⭐⭐⭐⭐"
  - If comment provided: "{Customer Name} gave you {rating} stars ⭐⭐⭐⭐⭐: "{comment}""

#### For Updated Reviews
- **Title**: "Review Updated"
- **Message**: "{Customer Name} updated their review to {rating} stars ⭐⭐⭐⭐⭐"
  - If comment provided: "{Customer Name} updated their review to {rating} stars ⭐⭐⭐⭐⭐: "{comment}""

### Database Structure
Notifications are stored in the `notifications` table with:
- `user_id`: Cook's ID
- `title`: Notification title
- `message`: Notification message with feedback
- `type`: 'review'
- `reference_id`: Review ID
- `reference_type`: 'review'
- `is_read`: false (initially)
- `created_at`: Timestamp

## API Endpoints Affected

### POST /api/reviews
Creates a new review and sends notification to cook
- **Requirement**: Order must be in 'delivered' or 'completed' status
- **Payload**: 
  ```json
  {
    "order_id": 123,
    "cook_id": 456,
    "rating": 5,
    "comment": "Excellent food! Thanks for your service"
  }
  ```

### PUT /api/reviews/:reviewId
Updates an existing review and notifies cook
- **Requirement**: Review must belong to the requesting customer
- **Payload**: 
  ```json
  {
    "rating": 4,
    "comment": "Updated feedback: Very good!"
  }
  ```

## Existing Infrastructure Used

### 1. Notification Helper (`utils/notificationHelper.js`)
- Already had `notifyNewReview()` function
- Creates database notification
- Sends FCM push notification if cook has FCM token
- Provides consistent notification formatting

### 2. Socket.IO Setup (`server.js`)
- User-specific rooms: `user_${userId}`
- Real-time event: `newNotification`
- Cook can join their user room to receive notifications

### 3. Database Schema
- `notifications` table already structured for review notifications
- `reviews` table with all necessary foreign keys

## Benefits

1. **Immediate Feedback Loop**: Cooks get instant feedback when customers review their meals
2. **Multi-Channel Delivery**: Notifications delivered via app, real-time socket, and push
3. **Contextual Information**: Includes customer name, rating, and actual feedback message
4. **Non-Intrusive**: Stored in database, can be read later if cook is offline
5. **Actionable**: Cook can respond to reviews through existing `replyToReview` endpoint

## Mobile App Integration

The Android app should:
1. Listen to Socket.IO `newNotification` events when cook is logged in
2. Register for FCM push notifications
3. Display review notifications prominently in the notifications section
4. Allow cooks to tap notification to view full review and respond

### Socket.IO Client Code (Example)
```java
// In CookHomeActivity or NotificationService
socket.on("newNotification", new Emitter.Listener() {
    @Override
    public void call(Object... args) {
        JSONObject data = (JSONObject) args[0];
        String type = data.getString("type");
        if ("review".equals(type)) {
            // Show notification in app
            String message = data.getString("message");
            showReviewNotification(message);
        }
    }
});
```

## Testing Checklist

- [ ] Customer submits review on delivered order → Cook receives notification
- [ ] Customer submits review on completed order → Cook receives notification
- [ ] Customer updates existing review → Cook receives update notification
- [ ] Notification includes customer name correctly
- [ ] Notification includes correct star rating
- [ ] Notification includes feedback comment when provided
- [ ] Notification stored in database with correct fields
- [ ] Socket.IO event emitted to cook's user room
- [ ] FCM push notification sent if cook has FCM token
- [ ] Cook can view notification in notifications section
- [ ] Cook can tap notification to see full review details

## Future Enhancements

1. **Rich Notifications**: Include meal image in notification
2. **Quick Reply**: Allow cook to reply directly from notification
3. **Sentiment Analysis**: Highlight positive/negative feedback differently
4. **Review Trends**: Notify cook of rating trends (improving/declining)
5. **Aggregate Reports**: Weekly summary of all feedback received

## Related Files

- `backend/controllers/reviewController.js` - Review submission logic
- `backend/utils/notificationHelper.js` - Notification creation utilities
- `backend/database/complete_schema.sql` - Database schema
- `backend/server.js` - Socket.IO configuration
- `backend/routes/reviewRoutes.js` - Review API routes

## Support & Maintenance

For issues or questions, check:
1. Server logs for notification creation errors
2. Socket.IO connection status in cook's app
3. FCM token registration in users table
4. Database notifications table for stored records
