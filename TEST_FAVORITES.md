# Quick Test Guide for Favorites Feature

## 🚀 Quick Start (5 Minutes)

### Step 1: Start Backend Server
```bash
cd backend
node server.js
```

**Expected Output:**
```
✅ Database connected
✅ SMTP ready
✅ Server running on port 5000
```

### Step 2: Add Sample Data to Database

Open MySQL and run:
```sql
USE tiffincraft;

-- Verify you have at least one customer and some cooks
SELECT id, name, role FROM users WHERE role IN ('customer', 'cook');

-- Add some test favorites (adjust IDs based on your data)
INSERT INTO favorites (customer_id, cook_id) VALUES (1, 2), (1, 3)
ON DUPLICATE KEY UPDATE created_at = CURRENT_TIMESTAMP;

-- Verify favorites were added
SELECT * FROM favorites;
```

### Step 3: Build and Install Android App
```bash
cd frontend
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
.\gradlew installDebug --no-daemon
```

### Step 4: Test on Device/Emulator

1. **Login as Customer**
   - Use a customer account that has favorites

2. **Navigate to Favorites**
   - Tap on "Favorites" icon in bottom navigation (heart icon)

3. **Verify Display**
   - ✅ See list of favorite cooks
   - ✅ Each card shows: cook image, name, rating, reviews, experience, specialties, stats
   - ✅ Red heart icon visible on each card

4. **Test Remove Favorite**
   - Tap the heart icon on any cook card
   - ✅ Cook should be removed from the list
   - ✅ Toast message: "Removed from favorites"
   - ✅ If last item removed, empty state appears

5. **Test View Menu**
   - Tap "View Menu" button on any cook card
   - ✅ Should navigate to CookDetailsActivity

6. **Test Empty State**
   - Remove all favorites
   - ✅ See empty state: heart outline icon, "No Favorites Yet" message

---

## 🧪 API Testing with Postman/cURL

### 1. Get Favorites
```bash
curl -X GET http://192.168.1.4:5000/api/favorites \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 2. Add to Favorites
```bash
curl -X POST http://192.168.1.4:5000/api/favorites \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{"cookId": 2}'
```

### 3. Remove from Favorites
```bash
curl -X DELETE http://192.168.1.4:5000/api/favorites/2 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 4. Check Favorite Status
```bash
curl -X GET http://192.168.1.4:5000/api/favorites/check/2 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## 🐛 Debugging

### Check Backend Logs
```bash
# Backend console will show:
[2024-06-28T10:30:00.000Z] GET /api/favorites
[2024-06-28T10:30:01.000Z] DELETE /api/favorites/2
```

### Check Android Logs
```bash
adb logcat | grep "FavoritesActivity"
```

### Check Database
```sql
-- See all favorites
SELECT 
    f.id,
    c.name as customer,
    ck.name as cook,
    f.created_at
FROM favorites f
JOIN users c ON f.customer_id = c.id
JOIN users ck ON f.cook_id = ck.id;
```

---

## ✅ Success Criteria

- [ ] Backend server starts without errors
- [ ] API endpoints respond correctly
- [ ] Android app builds successfully
- [ ] Favorites screen loads
- [ ] Cook cards display properly
- [ ] Remove favorite works
- [ ] View menu navigation works
- [ ] Empty state displays when no favorites
- [ ] Images load correctly

---

## 🎯 Quick Visual Check

**Favorites Screen Should Look Like:**
```
┌─────────────────────────┐
│ ← My Favorites          │
├─────────────────────────┤
│ ┌─────────────────────┐ │
│ │ 👨‍🍳              ❤️ │ │
│ │ Anita's Kitchen     │ │
│ │ ⭐ 4.8 (320)        │ │
│ │ 5 years exp         │ │
│ │ ───────────────     │ │
│ │ Specialties:        │ │
│ │ North Indian, Chi...│ │
│ │ 25  │ 150+ │ 30 min │ │
│ │ [View Menu]         │ │
│ └─────────────────────┘ │
└─────────────────────────┘
```

---

## 🎨 Color Reference

- Background: `#F8F8F8`
- Card: `#FFFFFF`
- Primary Button: `#4CAF50`
- Heart Icon: `#F44336`
- Text Dark: `#1E1E1E`
- Text Light: `#9E9E9E`

---

## 📱 Screenshot Checklist

Take screenshots of:
1. Favorites screen with items
2. Empty state
3. Remove favorite action
4. Bottom navigation highlighting

---

**Ready to test!** 🚀
