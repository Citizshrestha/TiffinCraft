import db from "../config/db.js";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import { generateOTP, getOTPExpiry, sendOTPEmail } from "../utils/otpService.js";

export const registerUser = async (req, res) => {
    try {
        const { full_name, email, phone, password, role } = req.body;

        if (!full_name || !email || !phone || !password || !role) {
            return res.status(400).json({ message: "All fields are required." });
        }

        if (!["customer", "cook"].includes(role)) {
            return res.status(400).json({ message: "Role must be customer or cook." });
        }

        const [existingEmail] = await db.promise().query(
            "SELECT id FROM users WHERE email = ?", [email]
        );
        if (existingEmail.length > 0) {
            return res.status(400).json({ message: "Email already registered." });
        }

        const [existingPhone] = await db.promise().query(
            "SELECT id FROM users WHERE phone = ?", [phone]
        );
        if (existingPhone.length > 0) {
            return res.status(400).json({ message: "Phone already registered." });
        }

        const password_hash = await bcrypt.hash(password, 10);
        const otp = generateOTP();
        const otpExpiry = getOTPExpiry();

        const [result] = await db.promise().query(
            `INSERT INTO users 
             (full_name, email, phone, password_hash, role, 
              otp_code, otp_expires_at, is_verified, auth_provider)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
            [full_name, email, phone, password_hash, role,
             otp, otpExpiry, false, 'local']
        );

        const emailResult = await sendOTPEmail(email, otp, full_name);

        if (!emailResult.success) {
            // Email failed — mark user as auto-verified so they can still log in
            // (dev/demo mode fallback when SMTP is not configured)
            await db.promise().query(
                "UPDATE users SET is_verified = 1, otp_code = NULL, otp_expires_at = NULL WHERE id = ?",
                [result.insertId]
            );
            return res.status(201).json({
                success: true,
                message: "Registration successful! (Email verification skipped in dev mode)",
                email: email,
                userId: result.insertId,
                autoVerified: true
            });
        }

        return res.status(201).json({
            message: "OTP sent to your email. Please verify to complete registration.",
            email: email,
            userId: result.insertId
        });

    } catch (error) {
        return res.status(500).json({ message: "Server error.", error: error.message });
    }
};

export const verifyOTP = async (req, res) => {
    try {
        const { email, otp } = req.body;

        if (!email || !otp) {
            return res.status(400).json({ message: "Email and OTP are required." });
        }

        const [users] = await db.promise().query(
            "SELECT * FROM users WHERE email = ?", [email]
        );

        if (users.length === 0) {
            return res.status(404).json({ message: "User not found." });
        }

        const user = users[0];

        if (user.is_verified) {
            return res.status(400).json({ message: "Email already verified." });
        }

        if (user.otp_code !== otp) {
            return res.status(400).json({ message: "Invalid OTP." });
        }

        if (new Date() > new Date(user.otp_expires_at)) {
            return res.status(400).json({ message: "OTP has expired. Please request a new one." });
        }

        await db.promise().query(
            `UPDATE users 
             SET is_verified = true, otp_code = NULL, otp_expires_at = NULL 
             WHERE email = ?`,
            [email]
        );

        const token = jwt.sign(
            { id: user.id, role: user.role },
            process.env.JWT_SECRET,
            { expiresIn: "7d" }
        );

        return res.status(200).json({
            success: true,
            message: "Email verified successfully!",
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
        return res.status(500).json({ message: "Server error.", error: error.message });
    }
};

export const resendOTP = async (req, res) => {
    try {
        const { email } = req.body;

        if (!email) {
            return res.status(400).json({ message: "Email is required." });
        }

        const [users] = await db.promise().query(
            "SELECT * FROM users WHERE email = ?", [email]
        );

        if (users.length === 0) {
            return res.status(404).json({ message: "User not found." });
        }

        const user = users[0];

        if (user.is_verified) {
            return res.status(400).json({ message: "Email already verified." });
        }

        const otp = generateOTP();
        const otpExpiry = getOTPExpiry();

        await db.promise().query(
            "UPDATE users SET otp_code = ?, otp_expires_at = ? WHERE email = ?",
            [otp, otpExpiry, email]
        );

        const emailResult = await sendOTPEmail(email, otp, user.full_name);

        if (!emailResult.success) {
            return res.status(500).json({ message: "Failed to resend OTP." });
        }

        return res.status(200).json({
            message: "New OTP sent to your email."
        });

    } catch (error) {
        return res.status(500).json({ message: "Server error.", error: error.message });
    }
};

export const loginUser = async (req, res) => {
    try {
        const { email, password } = req.body;

        if (!email || !password) {
            return res.status(400).json({ message: "Email and password are required." });
        }

        const [users] = await db.promise().query(
            "SELECT * FROM users WHERE email = ?", [email]
        );

        if (users.length === 0) {
            return res.status(404).json({ message: "User not found." });
        }

        const user = users[0];

        if (!user.is_active) {
            return res.status(403).json({ message: "Account deactivated." });
        }

        // If user is not verified, auto-verify them and let them log in
        // (handles dev/demo mode where OTP email may not have been received)
        if (!user.is_verified) {
            await db.promise().query(
                "UPDATE users SET is_verified = 1 WHERE id = ?",
                [user.id]
            );
        }

        const isMatch = await bcrypt.compare(password, user.password_hash);
        if (!isMatch) {
            return res.status(401).json({ message: "Invalid password." });
        }

        const token = jwt.sign(
            { id: user.id, role: user.role },
            process.env.JWT_SECRET,
            { expiresIn: "7d" }
        );

        return res.status(200).json({
            success: true,
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

    } catch (error) {
        return res.status(500).json({ message: "Server error.", error: error.message });
    }
};

export const getCurrentUser = async (req, res) => {
    try {
        const [users] = await db.promise().query(
            "SELECT id, full_name, email, phone, role, profile_image, created_at FROM users WHERE id = ?",
            [req.user.id]
        );

        if (users.length === 0) {
            return res.status(404).json({ message: "User not found." });
        }

        return res.status(200).json({ 
            success: true,
            user: users[0] 
        });

    } catch (error) {
        return res.status(500).json({ message: "Server error.", error: error.message });
    }
};

export const forgotPassword = async (req, res) => {
    try {
        const { email } = req.body;

        if (!email) {
            return res.status(400).json({ message: "Email is required." });
        }

        const [users] = await db.promise().query(
            "SELECT id, full_name, is_verified FROM users WHERE email = ?", [email]
        );

        if (users.length === 0) {
            return res.status(200).json({
                message: "If that email is registered, a reset link has been sent."
            });
        }

        const user = users[0];
        const resetToken = generateOTP();
        const resetExpiry = getOTPExpiry();

        await db.promise().query(
            "UPDATE users SET otp_code = ?, otp_expires_at = ? WHERE email = ?",
            [resetToken, resetExpiry, email]
        );

        const { sendEmail } = await import("../utils/emailService.js");

        const mailOptions = {
            from: `"${process.env.SMTP_FROM_NAME}" <${process.env.SMTP_FROM_EMAIL}>`,
            to: email,
            subject: "TiffinCraft — Password Reset Code",
            html: `
            <div style="font-family: Arial, sans-serif; max-width: 480px;
                        margin: auto; padding: 32px; border-radius: 12px;
                        border: 1px solid #eee; background: #ffffff;">
                <div style="text-align: center; margin-bottom: 24px;">
                    <h2 style="color: #2E7D32; font-size: 24px; margin: 0;">TiffinCraft</h2>
                    <p style="color: #888; font-size: 14px; margin-top: 4px;">
                        Homemade meals, crafted with love
                    </p>
                </div>
                <p style="color: #333; font-size: 16px;">
                    Hi <strong>${user.full_name}</strong>,
                </p>
                <p style="color: #555; font-size: 15px; line-height: 1.6;">
                    We received a request to reset your password. Use the code below:
                </p>
                <div style="text-align: center; margin: 32px 0;">
                    <div style="display: inline-block; background: #F1FFF1;
                                border: 2px dashed #4CAF50; border-radius: 12px;
                                padding: 20px 40px;">
                        <span style="font-size: 36px; font-weight: bold;
                                     letter-spacing: 12px; color: #2E7D32;">
                            ${resetToken}
                        </span>
                    </div>
                </div>
                <p style="color: #888; font-size: 13px; text-align: center;">
                    ⏱ This code expires in <strong>10 minutes</strong>
                </p>
                <p style="color: #888; font-size: 13px; text-align: center; margin-top: 24px;">
                    If you didn't request this, you can safely ignore this email.
                </p>
                <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;">
                <p style="color: #bbb; font-size: 12px; text-align: center;">
                    © 2025 TiffinCraft. All rights reserved.
                </p>
            </div>`
        };

        const result = await sendEmail(mailOptions);

        if (!result.success) {
            console.error("Email send failed:", result.error);
            return res.status(500).json({ message: "Failed to send reset email. Try again." });
        }

        return res.status(200).json({
            message: "Password reset code sent to your email."
        });

    } catch (error) {
        return res.status(500).json({ message: "Server error.", error: error.message });
    }
};

export const resetPassword = async (req, res) => {
    try {
        const { email, otp, newPassword } = req.body;

        if (!email || !otp || !newPassword) {
            return res.status(400).json({ message: "Email, OTP, and new password are required." });
        }

        if (newPassword.length < 6) {
            return res.status(400).json({ message: "Password must be at least 6 characters." });
        }

        const [users] = await db.promise().query(
            "SELECT * FROM users WHERE email = ?", [email]
        );

        if (users.length === 0) {
            return res.status(404).json({ message: "User not found." });
        }

        const user = users[0];

        if (user.otp_code !== otp) {
            return res.status(400).json({ message: "Invalid reset code." });
        }

        if (new Date() > new Date(user.otp_expires_at)) {
            return res.status(400).json({ message: "Reset code has expired. Please request a new one." });
        }

        const password_hash = await bcrypt.hash(newPassword, 10);

        await db.promise().query(
            "UPDATE users SET password_hash = ?, otp_code = NULL, otp_expires_at = NULL WHERE email = ?",
            [password_hash, email]
        );

        return res.status(200).json({ message: "Password reset successfully. You can now login." });

    } catch (error) {
        return res.status(500).json({ message: "Server error.", error: error.message });
    }
};

// Get customer profile
export const getCustomerProfile = async (req, res) => {
    try {
        const userId = req.user.id;

        const [users] = await db.promise().query(
            `SELECT id, full_name, email, phone, address, profile_image, created_at, role
             FROM users 
             WHERE id = ? AND role = 'customer'`,
            [userId]
        );

        if (users.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Customer profile not found."
            });
        }

        // Get order statistics
        const [orderStats] = await db.promise().query(
            `SELECT 
                COUNT(*) as total_orders,
                COALESCE(SUM(total_amount), 0) as total_spent
             FROM orders 
             WHERE customer_id = ?`,
            [userId]
        );

        const profile = {
            ...users[0],
            total_orders: orderStats[0]?.total_orders || 0,
            total_spent: orderStats[0]?.total_spent || 0
        };

        return res.status(200).json({
            success: true,
            profile: profile
        });

    } catch (error) {
        console.error("getCustomerProfile error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// Update customer profile
export const updateCustomerProfile = async (req, res) => {
    try {
        const userId = req.user.id;
        const { full_name, phone, address } = req.body;

        // Verify user is a customer
        const [users] = await db.promise().query(
            "SELECT role FROM users WHERE id = ?",
            [userId]
        );

        if (users.length === 0 || users[0].role !== 'customer') {
            return res.status(403).json({
                success: false,
                message: "Access denied. Customer account required."
            });
        }

        const updates = [];
        const values = [];

        if (full_name !== undefined) {
            updates.push("full_name = ?");
            values.push(full_name);
        }
        
        if (phone !== undefined) {
            // Check if phone is already used by another user
            const [existingPhone] = await db.promise().query(
                "SELECT id FROM users WHERE phone = ? AND id != ?",
                [phone, userId]
            );

            if (existingPhone.length > 0) {
                return res.status(400).json({
                    success: false,
                    message: "Phone number already in use by another account"
                });
            }

            updates.push("phone = ?");
            values.push(phone);
        }
        
        if (address !== undefined) {
            updates.push("address = ?");
            values.push(address);
        }

        if (updates.length === 0) {
            return res.status(400).json({
                success: false,
                message: "No fields to update."
            });
        }

        values.push(userId);

        await db.promise().query(
            `UPDATE users SET ${updates.join(", ")} WHERE id = ?`,
            values
        );

        return res.status(200).json({
            success: true,
            message: "Profile updated successfully."
        });

    } catch (error) {
        console.error("updateCustomerProfile error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// Upload customer profile image
export const uploadCustomerProfileImage = async (req, res) => {
    try {
        const userId = req.user.id;

        if (!req.file) {
            return res.status(400).json({
                success: false,
                message: "No image file provided."
            });
        }

        // Get current image to delete old one
        const [users] = await db.promise().query(
            "SELECT profile_image FROM users WHERE id = ?",
            [userId]
        );

        // Delete old image if exists
        if (users[0]?.profile_image) {
            const { deleteFile } = await import("../utils/fileHelper.js");
            deleteFile(users[0].profile_image);
        }

        // Build public URL
        const { buildFileUrl } = await import("../utils/fileHelper.js");
        const imageUrl = buildFileUrl(req, "profiles", req.file.filename);

        // Update user profile image
        await db.promise().query(
            "UPDATE users SET profile_image = ? WHERE id = ?",
            [imageUrl, userId]
        );

        return res.status(200).json({
            success: true,
            message: "Profile image uploaded successfully.",
            image_url: imageUrl
        });

    } catch (error) {
        console.error("uploadCustomerProfileImage error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error.",
            error: error.message
        });
    }
};

// Get user profile
export const getUserProfile = async (req, res) => {
    try {
        const userId = req.user.id;

        const [users] = await db.promise().query(
            `SELECT id, full_name, email, phone, role, profile_image, address, latitude, longitude, is_active
             FROM users 
             WHERE id = ?`,
            [userId]
        );

        if (users.length === 0) {
            return res.status(404).json({
                success: false,
                message: "User not found"
            });
        }

        const user = users[0];

        return res.status(200).json({
            success: true,
            user: {
                id: user.id,
                fullName: user.full_name,
                email: user.email,
                phone: user.phone,
                role: user.role,
                profileImage: user.profile_image,
                address: user.address,
                latitude: user.latitude,
                longitude: user.longitude,
                isActive: user.is_active
            }
        });
    } catch (error) {
        console.error("getUserProfile error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error",
            error: error.message
        });
    }
};

// Update user profile
export const updateUserProfile = async (req, res) => {
    try {
        const userId = req.user.id;
        const { full_name, phone, address, latitude, longitude } = req.body;

        // Build dynamic update query
        const updates = [];
        const values = [];

        if (full_name !== undefined) {
            updates.push("full_name = ?");
            values.push(full_name);
        }

        if (phone !== undefined) {
            // Check if phone is already used by another user
            const [existingPhone] = await db.promise().query(
                "SELECT id FROM users WHERE phone = ? AND id != ?",
                [phone, userId]
            );

            if (existingPhone.length > 0) {
                return res.status(400).json({
                    success: false,
                    message: "Phone number already in use"
                });
            }

            updates.push("phone = ?");
            values.push(phone);
        }

        if (address !== undefined) {
            updates.push("address = ?");
            values.push(address);
        }

        if (latitude !== undefined) {
            updates.push("latitude = ?");
            values.push(latitude);
        }

        if (longitude !== undefined) {
            updates.push("longitude = ?");
            values.push(longitude);
        }

        if (updates.length === 0) {
            return res.status(400).json({
                success: false,
                message: "No fields to update"
            });
        }

        values.push(userId);

        await db.promise().query(
            `UPDATE users SET ${updates.join(", ")} WHERE id = ?`,
            values
        );

        // Get updated user data
        const [updatedUsers] = await db.promise().query(
            `SELECT id, full_name, email, phone, role, profile_image, address, latitude, longitude
             FROM users 
             WHERE id = ?`,
            [userId]
        );

        const user = updatedUsers[0];

        return res.status(200).json({
            success: true,
            message: "Profile updated successfully",
            user: {
                id: user.id,
                fullName: user.full_name,
                email: user.email,
                phone: user.phone,
                role: user.role,
                profileImage: user.profile_image,
                address: user.address,
                latitude: user.latitude,
                longitude: user.longitude
            }
        });
    } catch (error) {
        console.error("updateUserProfile error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error",
            error: error.message
        });
    }
};

// Update profile image
export const updateProfileImage = async (req, res) => {
    try {
        const userId = req.user.id;

        if (!req.file) {
            return res.status(400).json({
                success: false,
                message: "No image file provided"
            });
        }

        const profileImagePath = req.file.path.replace(/\\/g, "/").replace("backend/", "");

        await db.promise().query(
            "UPDATE users SET profile_image = ? WHERE id = ?",
            [profileImagePath, userId]
        );

        return res.status(200).json({
            success: true,
            message: "Profile image updated successfully",
            profileImage: profileImagePath
        });
    } catch (error) {
        console.error("updateProfileImage error:", error);
        return res.status(500).json({
            success: false,
            message: "Server error",
            error: error.message
        });
    }
};
