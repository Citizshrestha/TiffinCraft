import fetch from 'node-fetch';

const BASE_URL = 'http://localhost:5000/api';

// Test credentials (update these with actual test user credentials)
const CUSTOMER_EMAIL = 'test@customer.com';
const CUSTOMER_PASSWORD = 'password123';
const COOK_EMAIL = 'test@cook.com';
const COOK_PASSWORD = 'password123';

let customerToken = '';
let cookToken = '';

async function loginCustomer() {
    console.log('\n🔐 Logging in as customer...');
    const response = await fetch(`${BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            email: CUSTOMER_EMAIL,
            password: CUSTOMER_PASSWORD
        })
    });

    const data = await response.json();
    if (data.success && data.token) {
        customerToken = data.token;
        console.log('✅ Customer login successful');
        return true;
    } else {
        console.log('❌ Customer login failed:', data.message);
        return false;
    }
}

async function loginCook() {
    console.log('\n🔐 Logging in as cook...');
    const response = await fetch(`${BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            email: COOK_EMAIL,
            password: COOK_PASSWORD
        })
    });

    const data = await response.json();
    if (data.success && data.token) {
        cookToken = data.token;
        console.log('✅ Cook login successful');
        return true;
    } else {
        console.log('❌ Cook login failed:', data.message);
        return false;
    }
}

async function testCustomerDashboard() {
    console.log('\n📊 Testing Customer Dashboard...');
    const response = await fetch(`${BASE_URL}/customer/dashboard`, {
        headers: { 'Authorization': `Bearer ${customerToken}` }
    });

    const data = await response.json();
    if (data.success) {
        console.log('✅ Customer dashboard loaded successfully');
        console.log(`   - Cart items: ${data.data.stats.cartItemsCount}`);
        console.log(`   - Favorites: ${data.data.stats.favoriteCount}`);
        console.log(`   - Total orders: ${data.data.stats.totalOrders}`);
        console.log(`   - Popular cooks: ${data.data.popularCooks.length}`);
        console.log(`   - Unread notifications: ${data.data.unreadNotificationsCount}`);
    } else {
        console.log('❌ Customer dashboard failed:', data.message);
    }
}

async function testCookDashboard() {
    console.log('\n📊 Testing Cook Dashboard...');
    const response = await fetch(`${BASE_URL}/cook/dashboard`, {
        headers: { 'Authorization': `Bearer ${cookToken}` }
    });

    const data = await response.json();
    if (data.success) {
        console.log('✅ Cook dashboard loaded successfully');
        console.log(`   - Today orders: ${data.dashboard.today_orders.count}`);
        console.log(`   - Today earnings: ₹${data.dashboard.today_earnings.amount}`);
        console.log(`   - Active orders: ${data.dashboard.active_orders.count}`);
        console.log(`   - Average rating: ${data.dashboard.average_rating.rating}`);
    } else {
        console.log('❌ Cook dashboard failed:', data.message);
    }
}

async function testGetCustomerProfile() {
    console.log('\n👤 Testing Get Customer Profile...');
    const response = await fetch(`${BASE_URL}/auth/profile`, {
        headers: { 'Authorization': `Bearer ${customerToken}` }
    });

    const data = await response.json();
    if (data.success) {
        console.log('✅ Customer profile retrieved');
        console.log(`   - Name: ${data.user.fullName}`);
        console.log(`   - Email: ${data.user.email}`);
        console.log(`   - Phone: ${data.user.phone}`);
    } else {
        console.log('❌ Get customer profile failed:', data.message);
    }
    return data;
}

async function testUpdateCustomerProfile() {
    console.log('\n✏️  Testing Update Customer Profile...');
    const response = await fetch(`${BASE_URL}/auth/profile`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${customerToken}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            full_name: 'Test Customer Updated',
            phone: '9876543210',
            address: '123 Test Street, Test City'
        })
    });

    const data = await response.json();
    if (data.success) {
        console.log('✅ Customer profile updated successfully');
        console.log(`   - New name: ${data.user.fullName}`);
        console.log(`   - New phone: ${data.user.phone}`);
        console.log(`   - New address: ${data.user.address}`);
    } else {
        console.log('❌ Update customer profile failed:', data.message);
    }
}

async function testGetCookProfile() {
    console.log('\n👨‍🍳 Testing Get Cook Profile...');
    const response = await fetch(`${BASE_URL}/cook/profile`, {
        headers: { 'Authorization': `Bearer ${cookToken}` }
    });

    const data = await response.json();
    if (data.success) {
        console.log('✅ Cook profile retrieved');
        console.log(`   - Name: ${data.profile.full_name}`);
        console.log(`   - Kitchen: ${data.profile.kitchen_name}`);
        console.log(`   - Food Type: ${data.profile.food_type}`);
    } else {
        console.log('❌ Get cook profile failed:', data.message);
    }
}

async function testUpdateCookProfile() {
    console.log('\n✏️  Testing Update Cook Profile...');
    const response = await fetch(`${BASE_URL}/cook/profile`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${cookToken}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            full_name: 'Test Cook Updated',
            phone: '9876543211',
            kitchen_name: 'Updated Test Kitchen',
            food_type: 'North Indian',
            description: 'Authentic homemade North Indian cuisine',
            capacity_per_day: 50
        })
    });

    const data = await response.json();
    if (data.success) {
        console.log('✅ Cook profile updated successfully');
    } else {
        console.log('❌ Update cook profile failed:', data.message);
    }
}

async function testNotifications() {
    console.log('\n🔔 Testing Notifications...');
    const response = await fetch(`${BASE_URL}/notifications`, {
        headers: { 'Authorization': `Bearer ${customerToken}` }
    });

    const data = await response.json();
    if (data.success) {
        console.log(`✅ Notifications retrieved: ${data.notifications.length} notifications`);
    } else {
        console.log('❌ Get notifications failed:', data.message);
    }
}

async function testUnreadNotificationCount() {
    console.log('\n🔔 Testing Unread Notification Count...');
    const response = await fetch(`${BASE_URL}/notifications/unread-count`, {
        headers: { 'Authorization': `Bearer ${customerToken}` }
    });

    const data = await response.json();
    if (data.success) {
        console.log(`✅ Unread notifications: ${data.unread_count}`);
    } else {
        console.log('❌ Get unread count failed:', data.message);
    }
}

async function runAllTests() {
    console.log('🚀 Starting TiffinCraft Profile Edit & Dashboard Tests\n');
    console.log('='.repeat(60));

    // Login
    const customerLoggedIn = await loginCustomer();
    const cookLoggedIn = await loginCook();

    if (!customerLoggedIn && !cookLoggedIn) {
        console.log('\n❌ Cannot proceed without login credentials');
        console.log('Please update CUSTOMER_EMAIL, CUSTOMER_PASSWORD, COOK_EMAIL, and COOK_PASSWORD in this script');
        return;
    }

    // Customer Tests
    if (customerLoggedIn) {
        console.log('\n' + '='.repeat(60));
        console.log('CUSTOMER TESTS');
        console.log('='.repeat(60));
        
        await testCustomerDashboard();
        await testGetCustomerProfile();
        await testUpdateCustomerProfile();
        await testNotifications();
        await testUnreadNotificationCount();
    }

    // Cook Tests
    if (cookLoggedIn) {
        console.log('\n' + '='.repeat(60));
        console.log('COOK TESTS');
        console.log('='.repeat(60));
        
        await testCookDashboard();
        await testGetCookProfile();
        await testUpdateCookProfile();
    }

    console.log('\n' + '='.repeat(60));
    console.log('✅ All tests completed!');
    console.log('='.repeat(60));
}

// Run all tests
runAllTests().catch(err => {
    console.error('\n❌ Test failed with error:', err);
    process.exit(1);
});
