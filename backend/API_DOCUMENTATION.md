# TiffinCraft API Documentation

## Base URL
```
http://localhost:5000/api
```

## Authentication
Most endpoints require authentication via JWT token in the Authorization header:
```
Authorization: Bearer <your_jwt_token>
```

---

## Authentication Endpoints

### Register User
**POST** `/auth/register`

**Request Body:**
```json
{
  "full_name": "John Doe",
  "email": "john@example.com",
  "phone": "1234567890",
  "password": "securepassword",
  "role": "cook" // or "customer"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Account created successfully!",
  "userId": 1
}
```

---

### Login User
**POST** `/auth/login`

**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "securepassword"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "fullName": "John Doe",
    "email": "john@example.com",
    "phone": "1234567890",
    "role": "cook",
    "profileImage": null
  }
}
```

---

### Get Current User
**GET** `/auth/me`

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "user": {
    "id": 1,
    "full_name": "John Doe",
    "email": "john@example.com",
    "phone": "1234567890",
    "role": "cook",
    "created_at": "2024-06-15T10:00:00.000Z"
  }
}
```

---

### Logout
**POST** `/auth/logout`

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

---

## Cook Profile Endpoints

### Setup Cook Profile (Cook Only)
**POST** `/cook/profile`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "kitchen_name": "Mama's Kitchen",
  "food_type": "North Indian, South Indian",
  "description": "Authentic homemade Indian food with love",
  "capacity_per_day": 50
}
```

**Response:**
```json
{
  "success": true,
  "message": "Cook profile set up successfully!"
}
```

---

### Get My Cook Profile (Cook Only)
**GET** `/cook/profile`

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "success": true,
  "profile": {
    "id": 1,
    "user_id": 1,
    "kitchen_name": "Mama's Kitchen",
    "food_type": "North Indian, South Indian",
    "description": "Authentic homemade Indian food with love",
    "capacity_per_day": 50,
    "bio": null,
    "specialties": null,
    "rating": 0.00,
    "total_orders": 0,
    "is_verified": false,
    "is_approved": false,
    "full_name": "John Doe",
    "email": "john@example.com",
    "phone": "1234567890",
    "profile_image": null,
    "address": null
  }
}
```

---

### Update Cook Profile (Cook Only)
**PUT** `/cook/profile`

**Headers:** `Authorization: Bearer <token>`

**Request Body:** (all fields optional)
```json
{
  "kitchen_name": "Mama's Kitchen Updated",
  "food_type": "North Indian, South Indian, Chinese",
  "description": "Updated description",
  "capacity_per_day": 100,
  "bio": "Professional cook with 10 years experience",
  "specialties": "Biryani, Dal Makhani, Paneer Butter Masala"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Profile updated successfully."
}
```

---

### Get All Cooks (Public)
**GET** `/cook`

**Response:**
```json
{
  "success": true,
  "cooks": [
    {
      "id": 1,
      "user_id": 1,
      "kitchen_name": "Mama's Kitchen",
      "food_type": "North Indian, South Indian",
      "description": "Authentic homemade Indian food with love",
      "rating": 4.50,
      "total_orders": 120,
      "full_name": "John Doe",
      "profile_image": null,
      "address": "123 Main St"
    }
  ]
}
```

---

### Get Cook By ID (Public)
**GET** `/cook/:cookId`

**Response:**
```json
{
  "success": true,
  "cook": {
    "id": 1,
    "user_id": 1,
    "kitchen_name": "Mama's Kitchen",
    "food_type": "North Indian, South Indian",
    "description": "Authentic homemade Indian food with love",
    "capacity_per_day": 50,
    "bio": "Professional cook with 10 years experience",
    "specialties": "Biryani, Dal Makhani",
    "rating": 4.50,
    "total_orders": 120,
    "is_verified": true,
    "is_approved": true,
    "full_name": "John Doe",
    "email": "john@example.com",
    "phone": "1234567890",
    "profile_image": null,
    "address": "123 Main St"
  }
}
```

---

## Meal Endpoints

### Add Meal (Cook Only)
**POST** `/meals`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "name": "Butter Chicken",
  "description": "Creamy tomato-based curry with tender chicken",
  "price": 250.00,
  "category": "Main Course",
  "cuisine_type": "North Indian",
  "is_available": true,
  "preparation_time": 30,
  "spice_level": "medium",
  "is_vegetarian": false,
  "is_vegan": false,
  "allergens": "dairy, nuts",
  "image_url": "https://example.com/butter-chicken.jpg"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Meal added successfully!",
  "mealId": 1
}
```

---

### Get My Meals (Cook Only)
**GET** `/meals/my/list`

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "success": true,
  "meals": [
    {
      "id": 1,
      "cook_id": 1,
      "name": "Butter Chicken",
      "description": "Creamy tomato-based curry",
      "price": 250.00,
      "category": "Main Course",
      "cuisine_type": "North Indian",
      "is_available": true,
      "preparation_time": 30,
      "spice_level": "medium",
      "is_vegetarian": false,
      "is_vegan": false,
      "allergens": "dairy, nuts",
      "image_url": "https://example.com/butter-chicken.jpg",
      "created_at": "2024-06-15T10:00:00.000Z",
      "updated_at": "2024-06-15T10:00:00.000Z"
    }
  ]
}
```

---

### Get All Meals (Public)
**GET** `/meals`

**Query Parameters:** (all optional)
- `category` - Filter by category
- `cuisine_type` - Filter by cuisine type
- `is_vegetarian` - Filter vegetarian meals (true/false)
- `is_vegan` - Filter vegan meals (true/false)
- `max_price` - Filter by maximum price

**Example:** `/meals?category=Main%20Course&is_vegetarian=true&max_price=300`

**Response:**
```json
{
  "success": true,
  "meals": [
    {
      "id": 1,
      "cook_id": 1,
      "name": "Paneer Butter Masala",
      "description": "Rich and creamy paneer curry",
      "price": 200.00,
      "category": "Main Course",
      "cuisine_type": "North Indian",
      "is_available": true,
      "is_vegetarian": true,
      "cook_name": "John Doe",
      "cook_rating": 4.50
    }
  ]
}
```

---

### Get Meals By Cook (Public)
**GET** `/meals/cook/:cookId`

**Response:**
```json
{
  "success": true,
  "meals": [
    {
      "id": 1,
      "cook_id": 1,
      "name": "Butter Chicken",
      "price": 250.00,
      "cook_name": "John Doe"
    }
  ]
}
```

---

### Get Meal By ID (Public)
**GET** `/meals/:mealId`

**Response:**
```json
{
  "success": true,
  "meal": {
    "id": 1,
    "cook_id": 1,
    "name": "Butter Chicken",
    "description": "Creamy tomato-based curry",
    "price": 250.00,
    "category": "Main Course",
    "cuisine_type": "North Indian",
    "preparation_time": 30,
    "spice_level": "medium",
    "is_vegetarian": false,
    "cook_name": "John Doe",
    "cook_image": null,
    "cook_rating": 4.50,
    "cook_total_orders": 120
  }
}
```

---

### Update Meal (Cook Only)
**PUT** `/meals/:mealId`

**Headers:** `Authorization: Bearer <token>`

**Request Body:** (all fields optional)
```json
{
  "name": "Butter Chicken Special",
  "price": 280.00,
  "is_available": true
}
```

**Response:**
```json
{
  "success": true,
  "message": "Meal updated successfully."
}
```

---

### Delete Meal (Cook Only)
**DELETE** `/meals/:mealId`

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "success": true,
  "message": "Meal deleted successfully."
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "success": false,
  "message": "Kitchen name, food type, and description are required."
}
```

### 401 Unauthorized
```json
{
  "success": false,
  "message": "No token provided. Access denied."
}
```

### 403 Forbidden
```json
{
  "success": false,
  "message": "Access denied. Only cook can do this."
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "Cook profile not found."
}
```

### 500 Server Error
```json
{
  "success": false,
  "message": "Server error.",
  "error": "Detailed error message"
}
```

---

## Notes

1. All timestamps are in ISO 8601 format
2. JWT tokens expire after 30 days
3. Prices are in decimal format (e.g., 250.00)
4. Spice levels: mild, medium, hot, very_hot
5. Roles: customer, cook
6. All protected endpoints require valid JWT token in Authorization header
