━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TABLE: users
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
id              INT PK AUTO_INCREMENT
full_name       VARCHAR(100) NOT NULL
email           VARCHAR(100) UNIQUE NOT NULL
phone           VARCHAR(20) UNIQUE NOT NULL
password_hash   VARCHAR(255) NOT NULL
role            ENUM('customer','cook','admin')
profile_image   VARCHAR(255) NULL
address         TEXT NULL
latitude        DECIMAL(10,7) NULL
longitude       DECIMAL(10,7) NULL
is_active       BOOLEAN DEFAULT TRUE
created_at      TIMESTAMP DEFAULT NOW()

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TABLE: cooks
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
id                  INT PK AUTO_INCREMENT
user_id             INT FK → users(id)
kitchen_name        VARCHAR(100) NULL
description         TEXT NULL
food_type           VARCHAR(100) NULL
verification_status ENUM('pending',
                    'approved','rejected')
                    DEFAULT 'pending'
capacity_per_day    INT DEFAULT 0
average_rating      DECIMAL(3,2) DEFAULT 0.00
total_reviews       INT DEFAULT 0
is_on_holiday       BOOLEAN DEFAULT FALSE
listing_fee_paid    BOOLEAN DEFAULT FALSE
premium_badge       BOOLEAN DEFAULT FALSE

Relationship: user_id → users(id) CASCADE DELETE

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TABLE: meals
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
id                INT PK AUTO_INCREMENT
cook_id           INT FK → cooks(id)
meal_name         VARCHAR(100) NOT NULL
description       TEXT NULL
price             DECIMAL(10,2) NOT NULL
category          VARCHAR(50) NULL
available_qty     INT DEFAULT 0
is_available      BOOLEAN DEFAULT TRUE
meal_image        VARCHAR(255) NULL
created_at        TIMESTAMP DEFAULT NOW()

Relationship: cook_id → cooks(id) CASCADE DELETE

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TABLE: orders
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
id                INT PK AUTO_INCREMENT
customer_id       INT FK → users(id)
cook_id           INT FK → cooks(id)
total_amount      DECIMAL(10,2) NOT NULL
delivery_charge   DECIMAL(10,2) DEFAULT 0.00
order_status      ENUM('placed','accepted',
                  'preparing',
                  'out_for_delivery',
                  'delivered','cancelled')
                  DEFAULT 'placed'
payment_method    ENUM('esewa','khalti',
                  'bank_qr','cod')
                  DEFAULT 'cod'
payment_status    ENUM('pending','paid','failed')
                  DEFAULT 'pending'
delivery_address  TEXT NOT NULL
subscription_type ENUM('weekly','monthly') NULL
created_at        TIMESTAMP DEFAULT NOW()
updated_at        TIMESTAMP DEFAULT NOW()
                  ON UPDATE NOW()

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TABLE: order_items
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
id          INT PK AUTO_INCREMENT
order_id    INT FK → orders(id)
meal_id     INT FK → meals(id)
quantity    INT NOT NULL
price       DECIMAL(10,2) NOT NULL

Relationship: order_id CASCADE DELETE

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TABLE: reviews
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
id            INT PK AUTO_INCREMENT
order_id      INT FK → orders(id)
customer_id   INT FK → users(id)
cook_id       INT FK → cooks(id)
rating        INT NOT NULL (CHECK 1–5)
comment       TEXT NULL
created_at    TIMESTAMP DEFAULT NOW()

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TABLE: admin_records
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
id            INT PK AUTO_INCREMENT
admin_id      INT FK → users(id)
action_type   VARCHAR(100)
description   TEXT NULL
created_at    TIMESTAMP DEFAULT NOW()

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
RELATIONSHIPS SUMMARY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
users → cooks         one-to-one
cooks → meals         one-to-many
users → orders        one-to-many (customer)
cooks → orders        one-to-many
orders → order_items  one-to-many
orders → reviews      one-to-one

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
AUTH ARCHITECTURE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Provider:     JWT (custom, no third-party auth)
Token:        Signed with JWT_SECRET, 7 day expiry
Storage:      Android SharedPreferences
Header:       Authorization: Bearer <token>
Protection:   authMiddleware on all protected routes
Role check:   roleMiddleware('cook') or ('customer')

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
SENSITIVE FIELDS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
password_hash   — bcryptjs, never return in API
JWT_SECRET      — .env only, never in code
payment data    — handled by eSewa/Khalti
                  externally, not stored

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
API ENDPOINTS LIST
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Auth:
  POST   /api/auth/register
  POST   /api/auth/login
  GET    /api/auth/me          [protected]

Cooks:
  POST   /api/cooks/profile    [cook only]
  GET    /api/cooks            [public]
  GET    /api/cooks/:id        [public]
  PUT    /api/cooks/profile    [cook only]

Meals:
  POST   /api/meals            [cook only]
  GET    /api/meals/cook/:id   [public]
  PUT    /api/meals/:id        [cook only]
  DELETE /api/meals/:id        [cook only]

Orders:
  POST   /api/orders           [customer]
  GET    /api/orders/:id       [protected]
  GET    /api/orders/customer/:id [customer]
  GET    /api/orders/cook/:id  [cook]
  PUT    /api/orders/:id/status [cook]

Reviews:
  POST   /api/reviews          [customer]
  GET    /api/reviews/cook/:id [public]