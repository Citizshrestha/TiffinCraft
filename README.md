# TiffinCraft - Homemade Food Delivery Platform

A complete full-stack application connecting home cooks with customers seeking authentic homemade meals.

## 🎯 Project Overview

TiffinCraft enables home cooks to showcase their culinary skills by offering homemade meals to customers in their area. The platform provides a seamless experience for meal browsing, ordering, and delivery coordination.

## 🏗️ Architecture

### Backend
- **Framework:** Node.js + Express
- **Database:** MySQL
- **Authentication:** JWT (JSON Web Tokens)
- **Security:** bcrypt password hashing, role-based access control

### Frontend
- **Platform:** Android (Native Java)
- **HTTP Client:** Retrofit 2
- **JSON Parser:** Gson
- **Architecture:** MVC pattern

## 📁 Project Structure

```
TiffinCraft/
├── backend/                          # Node.js backend
│   ├── config/                       # Configuration files
│   │   └── db.js                     # Database connection
│   ├── controllers/                  # Request handlers
│   │   ├── authController.js         # Authentication logic
│   │   ├── cookController.js         # Cook profile management
│   │   └── mealController.js         # Meal CRUD operations
│   ├── middleware/                   # Express middleware
│   │   ├── authMiddleware.js         # JWT verification & RBAC
│   │   └── validation.js             # Input validation
│   ├── routes/                       # API routes
│   │   ├── authRoutes.js
│   │   ├── cookRoutes.js
│   │   └── mealRoutes.js
│   ├── database/                     # Database scripts
│   │   ├── complete_schema.sql
│   │   ├── complete_database_setup.sql
│   │   └── migration_update_schema.sql
│   ├── .env                          # Environment variables
│   ├── server.js                     # Main server file
│   ├── package.json
│   ├── API_DOCUMENTATION.md          # Complete API docs
│   ├── COMPLETE_SETUP_GUIDE.md       # Setup instructions
│   ├── DATABASE_UPDATE_REQUIRED.md   # Migration guide
│   └── test-api.sh                   # API testing script
│
├── frontend/                         # Android frontend
│   └── app/src/main/java/com/tiffincraft/app/
│       ├── api/                      # Network layer
│       │   ├── ApiService.java       # Retrofit interface
│       │   └── RetrofitClient.java   # HTTP client
│       ├── models/                   # Data models
│       │   ├── CookProfile.java
│       │   ├── CookProfileRequest.java
│       │   ├── CookProfileResponse.java
│       │   ├── Meal.java
│       │   ├── MealRequest.java
│       │   ├── MealResponse.java
│       │   ├── LoginRequest.java
│       │   ├── LoginResponse.java
│       │   ├── RegisterRequest.java
│       │   └── RegisterResponse.java
│       ├── activities/               # Android activities
│       └── session/                  # Session management
│
├── docs/                             # Additional documentation
├── IMPLEMENTATION_SUMMARY.md         # Implementation details
└── README.md                         # This file
```

## ⚡ Quick Start

### Prerequisites
- Node.js v14+
- MySQL v5.7+
- Android Studio (for frontend)
- Git

### Backend Setup

1. **Navigate to backend directory:**
   ```bash
   cd backend
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Configure environment:**
   - Ensure `.env` file has correct database credentials
   - Update `JWT_SECRET` for production

4. **Setup database:**
   - Open MySQL Workbench
   - Execute `database/migration_update_schema.sql`

5. **Start server:**
   ```bash
   npm run dev        # Development with auto-reload
   # OR
   npm start          # Production
   ```

6. **Verify server:**
   ```bash
   curl http://localhost:5000/api/health
   ```

### Frontend Setup

1. **Open project in Android Studio:**
   ```bash
   cd frontend
   ```

2. **Update API base URL in `RetrofitClient.java`:**
   ```java
   // For emulator
   private static final String BASE_URL = "http://10.0.2.2:5000/api/";
   
   // For physical device (use your computer's IP)
   private static final String BASE_URL = "http://192.168.x.x:5000/api/";
   ```

3. **Build and run** the Android app

## 🔑 Key Features

### For Cooks
✅ Register and setup kitchen profile  
✅ Add, edit, and delete meals  
✅ Manage meal availability  
✅ View own menu and orders  
✅ Track ratings and reviews  
✅ Real-time earnings tracking with visual trends  
✅ Order management with delete functionality  
✅ Transaction history with meal images  
✅ Payment verification and status tracking  

### For Customers
✅ Browse available cooks  
✅ Search and filter meals  
✅ View detailed meal information  
✅ Place orders  
✅ Rate and review cooks  
✅ Real-time order tracking  
✅ Multiple payment methods (COD, Online, eSewa)  

### Platform Features
✅ Secure JWT authentication  
✅ Role-based access control  
✅ Real-time updates via Socket.IO  
✅ Image upload support with Cloudinary  
✅ Location-based search  
✅ Commission management for platform  
✅ Notification system (in-app + FCM)  
✅ Chat functionality between cooks and customers  
✅ Subscription plans  
✅ Combo deals and referral system  

## 🔐 Security

- **Password Security:** bcrypt hashing with 12 salt rounds
- **Authentication:** JWT tokens with 30-day expiration
- **Authorization:** Role-based middleware (cook/customer)
- **SQL Injection Prevention:** Parameterized queries
- **CORS:** Configured for cross-origin requests
- **Input Validation:** express-validator middleware

## 📡 API Endpoints

### Authentication
```
POST   /api/auth/register     - Register new user
POST   /api/auth/login        - Login user
GET    /api/auth/me           - Get current user (protected)
POST   /api/auth/logout       - Logout (protected)
```

### Cook Profile
```
POST   /api/cook/profile      - Setup cook profile (cook only)
GET    /api/cook/profile      - Get my profile (cook only)
PUT    /api/cook/profile      - Update profile (cook only)
GET    /api/cook              - Get all cooks (public)
GET    /api/cook/:id          - Get cook by ID (public)
```

### Meals
```
POST   /api/meals             - Add meal (cook only)
GET    /api/meals/my/list     - Get my meals (cook only)
PUT    /api/meals/:id         - Update meal (cook only)
DELETE /api/meals/:id         - Delete meal (cook only)
GET    /api/meals             - Get all meals with filters (public)
GET    /api/meals/cook/:id    - Get meals by cook (public)
GET    /api/meals/:id         - Get meal by ID (public)
```

See `backend/API_DOCUMENTATION.md` for complete API reference.

## 🗄️ Database Schema

### Core Tables
- **users** - User accounts (cooks & customers)
- **cook_profiles** - Extended cook information
- **meals** - Meal listings
- **orders** - Customer orders
- **order_items** - Order line items
- **reviews** - Cook ratings and reviews

## 🧪 Testing

### Backend Testing
```bash
cd backend

# Using test script
./test-api.sh

# Or manual testing
curl -X POST http://localhost:5000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"full_name":"Test User","email":"test@example.com","phone":"1234567890","password":"test123","role":"cook"}'
```

### Frontend Testing
1. Build and run in Android Studio
2. Test user registration flow
3. Test login and authentication
4. Test cook profile setup
5. Test meal management features

## 📚 Documentation

- **[API_DOCUMENTATION.md](backend/API_DOCUMENTATION.md)** - Complete API reference
- **[COMPLETE_SETUP_GUIDE.md](backend/COMPLETE_SETUP_GUIDE.md)** - Detailed setup guide
- **[IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)** - Implementation details
- **[DATABASE_UPDATE_REQUIRED.md](backend/DATABASE_UPDATE_REQUIRED.md)** - Migration instructions

## 🚀 Deployment

### Backend Deployment

1. **Environment Variables:**
   - Set `NODE_ENV=production`
   - Use strong `JWT_SECRET`
   - Configure production database

2. **Database:**
   - Run migrations
   - Set up backups
   - Configure connection pooling

3. **Server:**
   - Use PM2 or similar process manager
   - Enable HTTPS
   - Configure reverse proxy (Nginx)
   - Set up monitoring

### Frontend Deployment

1. Update API base URL to production server
2. Enable ProGuard for code obfuscation
3. Generate signed APK
4. Test thoroughly before release
5. Publish to Google Play Store

## ⚠️ Important Notes

### Before First Run:
1. ✅ Install all dependencies
2. ✅ Configure `.env` file
3. ⚠️ **MUST RUN:** `database/migration_update_schema.sql`
4. ✅ Update frontend base URL
5. ✅ Test all endpoints

### Production Checklist:
- [ ] Change JWT_SECRET
- [ ] Configure CORS for specific origins
- [ ] Enable HTTPS
- [ ] Set up database backups
- [ ] Configure logging
- [ ] Set up monitoring
- [ ] Implement rate limiting
- [ ] Review security settings

## 🛠️ Technology Stack

### Backend
- Node.js
- Express 5.2.1
- MySQL2 3.22.5
- JWT 9.0.3
- bcryptjs 3.0.3
- CORS 2.8.6
- dotenv 17.4.2
- express-validator 7.3.2

### Frontend
- Android (Java)
- Retrofit 2
- Gson
- OkHttp3
- Material Design Components

## 📈 Project Status

- ✅ Backend API: Complete
- ✅ Frontend Android App: Complete  
- ✅ Admin Dashboard: Complete  
- ✅ Authentication & Authorization: Working
- ✅ Real-time Features: Working (Socket.IO)
- ✅ Payment Integration: Working (eSewa)
- ✅ Chat System: Working
- ✅ Earnings & Analytics: Complete
- ✅ Order Management: Complete with Delete
- ✅ Commission System: Working
- 🚀 Production Ready

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📝 License

MIT License - See LICENSE file for details

## 👥 Team

- Backend Development: Complete
- Frontend Development: Complete
- Database Design: Complete
- Documentation: Complete

## 🐛 Troubleshooting

### Server won't start
- Check if port 5000 is available
- Verify database connection
- Check `.env` configuration

### Database errors
- Ensure MySQL is running
- Run migration script
- Check database credentials

### Frontend can't connect
- Verify base URL in RetrofitClient
- Check server is running
- Test with cURL first

See `backend/COMPLETE_SETUP_GUIDE.md` for detailed troubleshooting.

## 📞 Support

For issues or questions:
1. Check documentation in `backend/` directory
2. Review API_DOCUMENTATION.md
3. See IMPLEMENTATION_SUMMARY.md

📧 Email: citizshresthaa@gmail.com

---

**Built with ❤️ for connecting home cooks with food lovers**
