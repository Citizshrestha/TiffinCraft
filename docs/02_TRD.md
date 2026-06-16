━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
FRONTEND
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Platform:       Android only
Language:       Java
UI:             XML layouts
Architecture:   MVC
                (Model = Java POJOs,
                 View = XML + Activities,
                 Controller = Activities
                 + helper classes)
Min SDK:        API 26 (Android 8.0)
Target SDK:     API 36
IDE:            Android Studio

Key Libraries:
  Retrofit 2.9.0    — REST API calls
  Gson converter    — JSON to Java objects
  OkHttp 4.12.0     — HTTP client
  Socket.IO 2.1.1   — Real-time events
  Glide 4.16.0      — Image loading
  Google Maps SDK   — Cook discovery map

Navigation:
  Activities for main screens
  No Fragments for MVP (keeps it simple)

Token Storage:
  SharedPreferences (JWT token, role,
  userId, fullName stored locally)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
BACKEND
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Runtime:        Node.js v25+
Framework:      Express.js
Module system:  ES Modules (type: module)
Auth:           JWT (jsonwebtoken)
                Token expiry: 7 days
Password:       bcryptjs (salt rounds: 10)
Real-time:      Socket.IO
File uploads:   Multer (meal images)
CORS:           Enabled for all origins
Port:           5000

Folder structure:
  tiffincraft-backend/
  ├── config/db.js
  ├── controllers/
  │   ├── authController.js
  │   ├── cookController.js
  │   ├── mealController.js
  │   ├── orderController.js
  │   └── reviewController.js
  ├── middleware/
  │   ├── authMiddleware.js
  │   └── roleMiddleware.js
  ├── routes/
  │   ├── authRoutes.js
  │   ├── cookRoutes.js
  │   ├── mealRoutes.js
  │   ├── orderRoutes.js
  │   └── reviewRoutes.js
  ├── socket/orderSocket.js
  ├── utils/jwt.js
  ├── .env
  └── app.js

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
DATABASE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Type:       MySQL 8.0
Port:       3306
DB Name:    tiffincraft
ORM:        None (raw queries with mysql2)
Queries:    db.promise().query() for async/await

Tables:
  users, cooks, meals, orders,
  order_items, reviews, admin_records

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
AUTHENTICATION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Method:     JWT Bearer token
Flow:       Register → Login → Token saved
            in SharedPreferences → sent in
            Authorization header on all
            protected API calls
Roles:      customer, cook, admin
Protection: authMiddleware verifies token
            roleMiddleware checks role

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
THIRD-PARTY SERVICES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Firebase FCM     — Push notifications
                   (order updates, dispatch)
Google Maps SDK  — Cook discovery map,
                   location-based browsing
eSewa / Khalti   — Digital payment (Nepal)
Bank QR          — Bank transfer option
COD              — Cash on delivery
Pathao / InDrive — Delivery partners
                   (external, not integrated)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
ENVIRONMENT VARIABLES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PORT=5000
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=yourpassword
DB_NAME=tiffincraft
DB_PORT=3306
JWT_SECRET=your_secret_key

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
CONSTRAINTS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- Must work on Android API 26+
- Nepal-specific payment gateways only
- No Razorpay (India-focused)
- Admin panel is web-based (not Android)
- Delivery is handled externally
- All imports use .js extension (ES Modules)
- 10.0.2.2 used for emulator localhost
- Real device uses LAN IP (192.168.x.x)