// ========================================
// PASSPORT CONFIGURATION (DISABLED)
// ========================================
// Facebook OAuth has been removed from the application.
// Google OAuth is handled via mobile token verification (verifyGoogleToken in oauthController.js)
// This file is kept for future OAuth integration if needed.

// To re-enable web-based Google OAuth in the future:
// 1. Uncomment the code below
// 2. Install passport-google-oauth20: npm install passport-google-oauth20
// 3. Configure GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET in .env
// 4. Import and initialize passport in server.js: app.use(passport.initialize())
// 5. Add routes in authRoutes.js for /auth/google and /auth/google/callback

/*
import { createRequire } from 'module';
const require = createRequire(import.meta.url);
const passport = require('passport');
import { Strategy as GoogleStrategy } from 'passport-google-oauth20';
import db from './db.js';
import dotenv from 'dotenv';
dotenv.config();

// ── GOOGLE STRATEGY ──
if (process.env.GOOGLE_CLIENT_ID && 
    process.env.GOOGLE_CLIENT_ID !== 'placeholder-client-id' &&
    process.env.GOOGLE_CLIENT_SECRET && 
    process.env.GOOGLE_CLIENT_SECRET !== 'placeholder-client-secret') {
    
    passport.use(new GoogleStrategy({
        clientID: process.env.GOOGLE_CLIENT_ID,
        clientSecret: process.env.GOOGLE_CLIENT_SECRET,
        callbackURL: process.env.GOOGLE_CALLBACK_URL,
        scope: ['profile', 'email']
    },
    async (accessToken, refreshToken, profile, done) => {
        try {
            const email = profile.emails[0].value;
            const fullName = profile.displayName;
            const googleId = profile.id;

            const [existingByGoogle] = await db.promise().query(
                'SELECT * FROM users WHERE google_id = ?',
                [googleId]
            );

            if (existingByGoogle.length > 0) {
                return done(null, existingByGoogle[0]);
            }

            const [existingByEmail] = await db.promise().query(
                'SELECT * FROM users WHERE email = ?',
                [email]
            );

            if (existingByEmail.length > 0) {
                await db.promise().query(
                    'UPDATE users SET google_id = ?, auth_provider = ? WHERE email = ?',
                    [googleId, 'google', email]
                );
                const [updated] = await db.promise().query(
                    'SELECT * FROM users WHERE email = ?', [email]
                );
                return done(null, updated[0]);
            }

            const [result] = await db.promise().query(
                `INSERT INTO users 
                 (full_name, email, password_hash, role, google_id, 
                  auth_provider, is_verified, is_active)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
                [fullName, email, '', 'customer', googleId, 
                 'google', true, true]
            );

            const [newUser] = await db.promise().query(
                'SELECT * FROM users WHERE id = ?', [result.insertId]
            );

            return done(null, newUser[0]);

        } catch (error) {
            return done(error, null);
        }
    }));
} else {
    console.log('⚠️  Google OAuth not configured - using placeholder credentials');
}

export default passport;
*/

// Export empty object for now
export default {};
