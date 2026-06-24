import { createRequire } from 'module';
const require = createRequire(import.meta.url);
const passport = require('passport');
import { Strategy as GoogleStrategy } from 'passport-google-oauth20';
import db from './db.js';
import dotenv from 'dotenv';
dotenv.config();

// ── GOOGLE STRATEGY ──
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

        // Check if user exists with this Google ID
        const [existingByGoogle] = await db.promise().query(
            'SELECT * FROM users WHERE google_id = ?',
            [googleId]
        );

        if (existingByGoogle.length > 0) {
            // User exists — return them
            return done(null, existingByGoogle[0]);
        }

        // Check if email already registered locally
        const [existingByEmail] = await db.promise().query(
            'SELECT * FROM users WHERE email = ?',
            [email]
        );

        if (existingByEmail.length > 0) {
            // Link Google ID to existing account
            await db.promise().query(
                'UPDATE users SET google_id = ?, auth_provider = ? WHERE email = ?',
                [googleId, 'google', email]
            );
            const [updated] = await db.promise().query(
                'SELECT * FROM users WHERE email = ?', [email]
            );
            return done(null, updated[0]);
        }

        // New user — create account
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

export default passport;