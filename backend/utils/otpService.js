import crypto from "crypto";
import { sendEmail } from "./emailService.js";

export const generateOTP = () => {
    return crypto.randomInt(100000, 999999).toString();
};

export const getOTPExpiry = () => {
    const expiry = new Date();
    expiry.setMinutes(expiry.getMinutes() + 10);
    return expiry;
};

export const sendOTPEmail = async (email, otp, fullName) => {
    const mailOptions = {
        from: `"${process.env.SMTP_FROM_NAME}" <${process.env.SMTP_FROM_EMAIL}>`,
        to: email,
        subject: 'TiffinCraft — Your OTP Verification Code',
        html: `
        <div style="font-family: Arial, sans-serif; max-width: 480px; 
                    margin: auto; padding: 32px; border-radius: 12px;
                    border: 1px solid #eee; background: #ffffff;">
            
            <div style="text-align: center; margin-bottom: 24px;">
                <h2 style="color: #2E7D32; font-size: 24px; margin: 0;">
                    TiffinCraft
                </h2>
                <p style="color: #888; font-size: 14px; margin-top: 4px;">
                    Homemade meals, crafted with love
                </p>
            </div>

            <p style="color: #333; font-size: 16px;">
                Hi <strong>${fullName}</strong>,
            </p>
            
            <p style="color: #555; font-size: 15px; line-height: 1.6;">
                Welcome to TiffinCraft! Use the OTP below to verify 
                your email address and complete your registration.
            </p>

            <div style="text-align: center; margin: 32px 0;">
                <div style="display: inline-block; background: #F1FFF1;
                            border: 2px dashed #4CAF50; border-radius: 12px;
                            padding: 20px 40px;">
                    <span style="font-size: 36px; font-weight: bold; 
                                 letter-spacing: 12px; color: #2E7D32;">
                        ${otp}
                    </span>
                </div>
            </div>

            <p style="color: #888; font-size: 13px; text-align: center;">
                ⏱ This OTP expires in <strong>10 minutes</strong>
            </p>

            <p style="color: #888; font-size: 13px; text-align: center;
                      margin-top: 24px;">
                If you didn't request this, please ignore this email.
            </p>

            <hr style="border: none; border-top: 1px solid #eee; 
                       margin: 24px 0;">
            
            <p style="color: #bbb; font-size: 12px; text-align: center;">
                © 2024 TiffinCraft. All rights reserved.
            </p>
        </div>
        `
    };

    return await sendEmail(mailOptions);
};