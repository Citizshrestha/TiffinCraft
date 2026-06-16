import db from "../config/db.js";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";

export const registerUser = async (req, res) => {
    try {
        const { full_name, email, phone, password, role } = req.body;

        // Check if email already exists
        const [existingUser] = await db.promise().query(
            "SELECT id FROM users WHERE email = ?",
            [email.toLowerCase()]
        );

        if (existingUser.length > 0) {
            return res.status(400).json({
                success: false,
                message: "Email already registered."
            });
        }

        // Check if phone already exists
        const [existingPhone] = await db.promise().query(
            "SELECT id FROM users WHERE phone = ?",
            [phone]
        );

        if (existingPhone.length > 0) {
            return res.status(400).json({
                success: false,
                message: "Phone number already registered."
            });
        }

        // Hash password with salt rounds of 12 for better security
        const password_hash = await bcrypt.hash(password, 12);

        // Insert user
        const [result] = await db.promise().query(
            "INSERT INTO users (full_name, email, phone, password_hash, role) VALUES (?, ?, ?, ?, ?)",
            [full_name, email.toLowerCase(), phone, password_hash, role]
        );

        // If cook, create cook profile
        if (role === 'cook') {
            await db.promise().query(
                "INSERT INTO cook_profiles (user_id) VALUES (?)",
                [result.insertId]
            );
        }

        return res.status(201).json({
            success: true,
            message: "Account created successfully!",
            userId: result.insertId
        });

    } catch (error) {
        console.error('Registration error:', error);
        return res.status(500).json({
            success: false,
            message: "Server error. Please try again later.",
            error: error.message
        });
    }
};

export const loginUser = async (req, res) => {
    try {
        const { email, password } = req.body;

        // Find user by email
        const [users] = await db.promise().query(
            "SELECT * FROM users WHERE email = ?",
            [email.toLowerCase()]
        );

        if (users.length === 0) {
            return res.status(401).json({
                success: false,
                message: "Invalid email or password."
            });
        }

        const user = users[0];

        // Check if account is active
        if (!user.is_active) {
            return res.status(403).json({
                success: false,
                message: "Account is deactivated. Contact support."
            });
        }

        // Verify password
        const isMatch = await bcrypt.compare(password, user.password_hash);

        if (!isMatch) {
            return res.status(401).json({
                success: false,
                message: "Invalid email or password."
            });
        }

        // Generate JWT token
        const token = jwt.sign(
            {
                id: user.id,
                role: user.role
            },
            process.env.JWT_SECRET,
            { expiresIn: "30d" }
        );

        return res.status(200).json({
            success: true,
            message: "Login successful",
            token: token,
            user: {
                id: user.id,
                fullName: user.full_name,
                email: user.email,
                phone: user.phone,
                role: user.role,
                profileImage: user.profile_image
            }
        });

    } catch (error) {
        console.error('Login error:', error);
        return res.status(500).json({
            success: false,
            message: "Server error. Please try again later.",
            error: error.message
        });
    }
};

export const getCurrentUser = async (req, res) => {
    try {
        const userId = req.user.id;

        const [users] = await db.promise().query(
            "SELECT id, full_name, email, phone, role, created_at FROM users WHERE id = ?",
            [userId]
        );

        if (users.length === 0) {
            return res.status(404).json({
                message: "User not found."
            });
        }

        return res.status(200).json({
            user: users[0]
        });

    } catch (error) {
        return res.status(500).json({
            message: "Server error.",
            error: error.message
        });
    }
};

export const logoutUser = async (req, res) => {
    try {
        // In a stateless JWT system, logout is handled client-side
        // by removing the token. We just return success.
        // If you implement token blacklisting later, add logic here.

        return res.status(200).json({
            success: true,
            message: "Logged out successfully"
        });

    } catch (error) {
        return res.status(500).json({
            success: false,
            message: "Server error during logout",
            error: error.message
        });
    }
};

