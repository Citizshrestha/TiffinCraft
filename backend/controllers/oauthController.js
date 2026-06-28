import jwt from 'jsonwebtoken';
import db from '../config/db.js';
import https from 'https';

export const verifyGoogleToken = async (req, res) => {
    try {
        const { idToken, role } = req.body;

        if (!idToken) {
            return res.status(400).json({ message: "ID token is required." });
        }

        if (role && !["customer", "cook"].includes(role)) {
            return res.status(400).json({ message: "Invalid role. Must be 'customer' or 'cook'." });
        }

        // Verify token with Google
        const googleUser = await verifyGoogleIdToken(idToken);

        if (!googleUser) {
            return res.status(401).json({ message: "Invalid Google token." });
        }

        const { email, name, sub: googleId } = googleUser;

        if (!email) {
            return res.status(400).json({ message: "Email not provided by Google." });
        }

        const [existingByGoogle] = await db.promise().query(
            'SELECT * FROM users WHERE google_id = ?',
            [googleId]
        );

        if (existingByGoogle.length > 0) {
            const user = existingByGoogle[0];

            if (!user.is_active) {
                return res.status(403).json({ message: "Account is deactivated." });
            }

            const token = jwt.sign(
                { id: user.id, role: user.role },
                process.env.JWT_SECRET,
                { expiresIn: '7d' }
            );

            return res.status(200).json({
                message: "Login successful.",
                token,
                user: {
                    id: user.id,
                    full_name: user.full_name,
                    email: user.email,
                    phone: user.phone,
                    role: user.role,
                    profile_image: user.profile_image
                }
            });
        }

        const [existingByEmail] = await db.promise().query(
            'SELECT * FROM users WHERE email = ?',
            [email]
        );

        if (existingByEmail.length > 0) {
            const user = existingByEmail[0];

            if (!user.is_active) {
                return res.status(403).json({ message: "Account is deactivated." });
            }

            await db.promise().query(
                'UPDATE users SET google_id = ?, auth_provider = ?, is_verified = true WHERE email = ?',
                [googleId, 'google', email]
            );

            const token = jwt.sign(
                { id: user.id, role: user.role },
                process.env.JWT_SECRET,
                { expiresIn: '7d' }
            );

            return res.status(200).json({
                message: "Account linked successfully.",
                token,
                user: {
                    id: user.id,
                    full_name: user.full_name,
                    email: user.email,
                    phone: user.phone,
                    role: user.role,
                    profile_image: user.profile_image
                }
            });
        }

        const userRole = role || 'customer';
        const [result] = await db.promise().query(
            `INSERT INTO users
             (full_name, email, password_hash, role, google_id,
              auth_provider, is_verified, is_active)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
            [name, email, '', userRole, googleId, 'google', true, true]
        );

        const [newUser] = await db.promise().query(
            'SELECT * FROM users WHERE id = ?',
            [result.insertId]
        );

        const user = newUser[0];
        const token = jwt.sign(
            { id: user.id, role: user.role },
            process.env.JWT_SECRET,
            { expiresIn: '7d' }
        );

        return res.status(201).json({
            message: "Account created successfully.",
            token,
            user: {
                id: user.id,
                full_name: user.full_name,
                email: user.email,
                phone: user.phone,
                role: user.role,
                profile_image: user.profile_image
            }
        });

    } catch (error) {
        console.error("Google verification error:", error);
        return res.status(500).json({ message: "Server error.", error: error.message });
    }
};

async function verifyGoogleIdToken(idToken) {
    return new Promise((resolve) => {
        const url = `https://oauth2.googleapis.com/tokeninfo?id_token=${idToken}`;

        const request = https.get(url, { timeout: 5000 }, (response) => {
            let data = '';

            response.on('data', (chunk) => {
                data += chunk;
            });

            response.on('end', () => {
                try {
                    const parsed = JSON.parse(data);

                    if (parsed.aud !== process.env.GOOGLE_CLIENT_ID) {
                        console.error("Invalid audience:", parsed.aud);
                        return resolve(null);
                    }

                    if (parsed.exp && Date.now() >= parsed.exp * 1000) {
                        console.error("Token expired");
                        return resolve(null);
                    }

                    resolve({
                        sub: parsed.sub,
                        email: parsed.email,
                        name: parsed.name,
                        picture: parsed.picture
                    });

                } catch (error) {
                    console.error("Google token parse error:", error);
                    resolve(null);
                }
            });

        }).on('error', (error) => {
            console.error("Google verification request error:", error);
            resolve(null);
        }).on('timeout', () => {
            request.destroy();
            console.error("Google verification timeout");
            resolve(null);
        });
    });
}

