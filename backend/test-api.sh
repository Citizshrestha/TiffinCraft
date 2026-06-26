#!/bin/bash

# TiffinCraft Backend API Testing Script
# This script tests all the API endpoints to ensure they work correctly

BASE_URL="http://localhost:5000/api"
TOKEN=""

echo "========================================="
echo "TiffinCraft API Testing"
echo "========================================="
echo ""

# Test 1: Health Check
echo "Test 1: Health Check"
curl -s -X GET "$BASE_URL/health" | python -m json.tool
echo ""
echo ""

# Test 2: Register Cook
echo "Test 2: Register Cook"
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "full_name": "Test Cook User",
    "email": "testcook@example.com",
    "phone": "9876543210",
    "password": "password123",
    "role": "cook"
  }')
echo "$REGISTER_RESPONSE" | python -m json.tool
echo ""
echo ""

# Test 3: Login Cook
echo "Test 3: Login Cook"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testcook@example.com",
    "password": "password123"
  }')
echo "$LOGIN_RESPONSE" | python -m json.tool

# Extract token from login response
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
echo ""
echo "Extracted Token: $TOKEN"
echo ""
echo ""

# Test 4: Setup Cook Profile
echo "Test 4: Setup Cook Profile"
curl -s -X POST "$BASE_URL/cook/profile" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "kitchen_name": "Test Kitchen",
    "food_type": "North Indian, South Indian",
    "description": "Delicious homemade food with authentic flavors",
    "capacity_per_day": 50
  }' | python -m json.tool
echo ""
echo ""

# Test 5: Get My Cook Profile
echo "Test 5: Get My Cook Profile"
curl -s -X GET "$BASE_URL/cook/profile" \
  -H "Authorization: Bearer $TOKEN" | python -m json.tool
echo ""
echo ""

# Test 6: Add Meal
echo "Test 6: Add Meal"
MEAL_RESPONSE=$(curl -s -X POST "$BASE_URL/meals" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Butter Chicken",
    "description": "Creamy tomato-based curry with tender chicken pieces",
    "price": 250.00,
    "category": "Main Course",
    "cuisine_type": "North Indian",
    "is_available": true,
    "preparation_time": 30,
    "spice_level": "medium",
    "is_vegetarian": false,
    "is_vegan": false,
    "allergens": "dairy, nuts"
  }')
echo "$MEAL_RESPONSE" | python -m json.tool

# Extract meal ID
MEAL_ID=$(echo "$MEAL_RESPONSE" | grep -o '"mealId":[0-9]*' | cut -d':' -f2)
echo ""
echo "Created Meal ID: $MEAL_ID"
echo ""
echo ""

# Test 7: Get My Meals
echo "Test 7: Get My Meals"
curl -s -X GET "$BASE_URL/meals/my/list" \
  -H "Authorization: Bearer $TOKEN" | python -m json.tool
echo ""
echo ""

# Test 8: Get All Meals (Public)
echo "Test 8: Get All Meals (Public - No Auth)"
curl -s -X GET "$BASE_URL/meals" | python -m json.tool
echo ""
echo ""

# Test 9: Get All Cooks (Public)
echo "Test 9: Get All Cooks (Public - No Auth)"
curl -s -X GET "$BASE_URL/cook" | python -m json.tool
echo ""
echo ""

# Test 10: Update Meal
if [ ! -z "$MEAL_ID" ]; then
  echo "Test 10: Update Meal"
  curl -s -X PUT "$BASE_URL/meals/$MEAL_ID" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d '{
      "price": 280.00,
      "is_available": true
    }' | python -m json.tool
  echo ""
  echo ""
fi

# Test 11: Register Customer
echo "Test 11: Register Customer"
curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "full_name": "Test Customer User",
    "email": "testcustomer@example.com",
    "phone": "8765432109",
    "password": "password123",
    "role": "customer"
  }' | python -m json.tool
echo ""
echo ""

# Test 12: Customer Login
echo "Test 12: Customer Login"
CUSTOMER_LOGIN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testcustomer@example.com",
    "password": "password123"
  }')
echo "$CUSTOMER_LOGIN" | python -m json.tool

CUSTOMER_TOKEN=$(echo "$CUSTOMER_LOGIN" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
echo ""
echo "Customer Token: $CUSTOMER_TOKEN"
echo ""
echo ""

# Test 13: Customer tries to access cook-only endpoint (should fail)
echo "Test 13: Customer tries to setup cook profile (should fail with 403)"
curl -s -X POST "$BASE_URL/cook/profile" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{
    "kitchen_name": "Test",
    "food_type": "Test",
    "description": "Test"
  }' | python -m json.tool
echo ""
echo ""

echo "========================================="
echo "Testing Complete!"
echo "========================================="
