━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 1 — PROJECT SETUP ✓ DONE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Goal: Working project structure, DB connected,
      server running, Android project ready.

✓ Created TiffinCraft/ root folder
✓ Created backend/ and frontend/ folders
✓ Initialized Node.js with ES Modules
✓ Installed all backend dependencies
✓ Created MySQL database + all 7 tables
✓ Backend server running on port 5000
✓ MySQL connected successfully
✓ Android project created (Java, API 26)
✓ All dependencies synced in Gradle
✓ Folder structure created in Android

Done when:
  npm run dev shows "MySQL connected"
  Browser shows TiffinCraft running ✓

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 2 — AUTHENTICATION (Backend) ✓ DONE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Goal: Register and login APIs working,
      JWT token returned.

✓ authController.js (register, login,
  getCurrentUser)
✓ authRoutes.js
✓ authMiddleware.js
✓ Tested in Postman — all passing

Done when:
  POST /register → 201 + userId
  POST /login → 200 + token + user object
  GET /me with token → user data returned

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 3 — ONBOARDING UI (Android) ✓ DONE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Goal: First-time user flow working end to end.

✓ SplashActivity + activity_splash.xml
✓ OnboardingActivity1 + layout
✓ OnboardingActivity2 + layout
✓ SelectRoleActivity + layout
✓ Navigation: Splash→Onboard1→Onboard2
  →SelectRole→Login

Done when:
  App opens, shows splash 3s, onboarding
  screens swipe, role selection navigates
  correctly to Login.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 4 — AUTH UI (Android) ← CURRENT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Goal: Login and Register screens connected
      to backend API.

[ ] LoginActivity + activity_login.xml
[ ] RegisterActivity + activity_register.xml
[ ] ApiClient.java + ApiService.java
[ ] All models (LoginRequest, RegisterRequest,
    AuthResponse, User)
[ ] SessionManager.java
[ ] Connect login API → save token → redirect
[ ] Connect register API → navigate on success
[ ] Validate all form fields with inline errors
[ ] Forgot password screen (UI only for now)

Done when:
  Real user can register from Android app
  Login returns token saved to SharedPrefs
  Redirects to correct home screen by role

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 5 — COOK FEATURES (Backend + Android)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Goal: Cook can set up profile and add meals.

[ ] cookController.js (create, get, update)
[ ] cookRoutes.js
[ ] mealController.js (add, list, update,
    delete)
[ ] mealRoutes.js
[ ] CookProfileSetupActivity (Android)
[ ] CookHomeActivity (Android)
[ ] AddMealActivity (Android)
[ ] ManageMealsActivity (Android)
[ ] Upload meal image with Multer

Done when:
  Cook registers → sets up profile
  Cook adds 3 meals with photos
  GET /cooks returns cook in list

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 6 — CUSTOMER FEATURES (Backend + Android)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Goal: Customer can browse cooks and subscribe.

[ ] GET /cooks endpoint (nearby filter)
[ ] CustomerHomeActivity (Android)
[ ] CookListActivity with RecyclerView
[ ] CookProfileActivity (view cook)
[ ] MealListActivity
[ ] SubscribeActivity (weekly/monthly)
[ ] orderController.js (place order)
[ ] orderRoutes.js
[ ] Order placed → cook notified via FCM

Done when:
  Customer sees cook list
  Subscribes to a cook
  Order appears in cook's incoming orders

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 7 — REAL-TIME TRACKING (Socket.IO)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Goal: Live order status updates.

[ ] orderSocket.js on backend
[ ] Cook updates status → emits socket event
[ ] Customer receives live update
[ ] OrderTrackingActivity (Android)
[ ] SocketManager.java
[ ] Status timeline UI

Done when:
  Cook taps "Mark Preparing" →
  Customer sees "Preparing" in real time
  without refreshing the screen

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 8 — RATINGS + REVIEWS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[ ] reviewController.js
[ ] reviewRoutes.js
[ ] RateMealActivity (Android)
[ ] Star rating UI component
[ ] Cook average rating auto-updated
[ ] Reviews shown on CookProfileActivity

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 9 — UI POLISH + EDGE CASES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[ ] Empty state screens for all lists
[ ] Loading indicators on all API calls
[ ] Error handling + user-friendly messages
[ ] No internet connection handling
[ ] Token expiry → auto redirect to login
[ ] Push notification integration (FCM)
[ ] In-app chat (Socket.IO)
[ ] Holiday mode for cook

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
PHASE 10 — TESTING + DEFENSE PREP
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[ ] Test all flows end-to-end
[ ] Fix all critical bugs
[ ] Demo on real Android device
[ ] Postman collection documented
[ ] GitHub repo organized with README
[ ] Presentation slides updated
[ ] Defense walkthrough rehearsed

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
DONE CRITERIA (Final Submission)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ New user can register as customer or cook
✓ Cook sets up profile and adds meals
✓ Customer finds cook on map and subscribes
✓ Order flows through all status stages
✓ Customer tracks order in real time
✓ Customer rates meal after delivery
✓ Admin can verify cook via web panel
✓ All core APIs tested in Postman
✓ App runs on real Android device
✓ No crashes on happy path flows