import nodemailer from "nodemailer";
import dotenv from "dotenv";

dotenv.config();

/**
 * Timeouts are the point of this config, not a detail.
 *
 * Nodemailer defaults to a 2-minute socket timeout, and every caller here
 * AWAITS the send before answering the HTTP request. So a slow SMTP handshake
 * (routine between a free-tier host and Gmail) held the response open far past
 * the app's 40s read timeout, and the user got "Network error. Please try
 * again." on a forgot-password request that had already stored their reset
 * code. Capping the send at ~25s worst case keeps the whole request inside the
 * client's budget, so a mail problem surfaces as a mail error instead of
 * masquerading as a dead network.
 */
export const transporter = nodemailer.createTransport({
    host: process.env.SMTP_HOST,
    port: parseInt(process.env.SMTP_PORT),
    secure: false,
    requireTLS: true,
    auth: {
        user: process.env.SMTP_USER,
        pass: process.env.SMTP_PASS
    },
    connectionTimeout: 8000,
    greetingTimeout: 8000,
    socketTimeout: 15000,
    // One warm connection instead of a fresh TLS+AUTH handshake per email; the
    // handshake is most of the latency on this path.
    pool: true,
    maxConnections: 2
});

export const sendEmail = async (mailOptions) => {
    try {
        const info = await transporter.sendMail(mailOptions);
        console.log(`✅ Email sent → ${mailOptions.to} | Subject: "${mailOptions.subject}" | MessageId: ${info.messageId}`);
        return { success: true, messageId: info.messageId };
    } catch (error) {
        console.error(`❌ Email failed → ${mailOptions.to} | Subject: "${mailOptions.subject}" | Error: ${error.message}`);
        return { success: false, error: error.message };
    }
}