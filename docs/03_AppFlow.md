━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
ALL SCREENS / PAGES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Onboarding:
  01. SplashActivity
  02. OnboardingActivity1
  03. OnboardingActivity2
  04. SelectRoleActivity

Auth:
  05. LoginActivity
  06. RegisterActivity
  07. ForgotPasswordActivity

Customer Screens:
  08. CustomerHomeActivity
  09. CookListActivity
  10. CookProfileActivity
  11. MealListActivity
  12. SubscribeActivity
  13. OrderTrackingActivity
  14. CustomerOrdersActivity
  15. RateMealActivity
  16. CustomerProfileActivity

Cook Screens:
  17. CookHomeActivity
  18. CookProfileSetupActivity
  19. AddMealActivity
  20. ManageMealsActivity
  21. IncomingOrdersActivity
  22. OrderDetailsActivity
  23. EarningsActivity
  24. CookProfileActivity

Shared:
  25. ChatActivity
  26. NotificationsActivity

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
NAVIGATION TYPE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- Activities (no Fragments for MVP)
- Bottom navigation bar for home screens
- Back button for secondary screens
- Intent extras to pass data between screens

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
FIRST SCREEN (brand new user)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Splash (3 sec) → Onboarding1 → Onboarding2
→ SelectRole → Login / Register

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
AUTH FLOW
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
New User:
  SelectRole
    → [Customer] → LoginActivity (role=customer)
         → "Register here" → RegisterActivity
              → Success → LoginActivity
              → Login → CustomerHomeActivity

    → [Cook] → LoginActivity (role=cook)
         → "Register here" → RegisterActivity
              → Success → CookProfileSetupActivity
              → Setup done → CookHomeActivity

Returning User:
  Splash → token found in SharedPreferences
    → role = customer → CustomerHomeActivity
    → role = cook → CookHomeActivity

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
CORE USER JOURNEY 1 — CUSTOMER ORDERS TIFFIN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. Customer opens app → CustomerHomeActivity
2. Sees nearby cooks on map or list
3. Taps a cook → CookProfileActivity
   (sees photos, bio, menu, ratings)
4. Taps "Subscribe" → SubscribeActivity
   (selects weekly or monthly plan)
5. Selects payment: eSewa / Khalti / QR / COD
6. Confirms → order placed
   Cook gets push notification instantly
7. Cook accepts → status: "Accepted"
   Customer notified via FCM + Socket.IO
8. Cook marks dispatched via Pathao/InDrive
   → status: "Out for Delivery"
9. Customer tracks in real time
   → OrderTrackingActivity
10. Delivered → customer rates meal
    → RateMealActivity (1–5 stars + comment)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
CORE USER JOURNEY 2 — COOK MANAGES ORDERS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. Cook opens app → CookHomeActivity
2. Sees today's orders and earnings summary
3. Taps incoming order → OrderDetailsActivity
4. Accepts or rejects
5. Updates status: Preparing → Dispatched
6. Calls Pathao/InDrive for delivery
7. Marks delivered → subscription auto-renews
8. Views earnings in EarningsActivity

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
EMPTY STATES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- No cooks nearby → "No cooks in your area yet"
- No orders yet → "You have no active orders"
- Cook has no meals → "Add your first meal"
- No reviews yet → "No reviews yet"

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
ERROR STATES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- No internet → show offline banner
- Login failed → inline error on field
- Server error → Toast "Something went wrong"
- Token expired → redirect to Login

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
REDIRECTS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
After login:
  role=customer → CustomerHomeActivity
  role=cook → CookHomeActivity

After register:
  role=customer → LoginActivity
  role=cook → CookProfileSetupActivity

After logout:
  Clear SharedPreferences → SelectRoleActivity

Token expired:
  Any screen → LoginActivity