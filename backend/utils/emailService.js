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

/**
 * HTTPS transport, and the reason it exists.
 *
 * The hosted backend (Render free tier) blocks OUTBOUND SMTP — ports 25/465/587
 * never connect, so every nodemailer send there died on connectionTimeout after
 * ~8s. Locally the same credentials work, which is why this only ever broke in
 * production: forgot-password stored the reset code (the UPDATE runs before the
 * send) and then returned 500, so users saw "couldn't send the reset email" and
 * no code ever arrived. Registration OTP and cook-approval mail failed the same
 * way for the same reason.
 *
 * Brevo's transactional API is the identical mail service over HTTPS/443, which
 * is not blocked. Set BREVO_API_KEY (Brevo → SMTP & API → API keys, starts
 * "xkeysib-") and this becomes the primary path; SMTP stays as the fallback so
 * local development and any non-Brevo provider keep working untouched.
 */
const BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

/** '"TiffinCraft" <a@b.com>' → { name: "TiffinCraft", email: "a@b.com" } */
const parseAddress = (value) => {
    if (!value) return null;
    const angled = /^\s*"?([^"<]*)"?\s*<\s*([^>\s]+)\s*>\s*$/.exec(value);
    if (angled) {
        const name = angled[1].trim();
        return name ? { name, email: angled[2] } : { email: angled[2] };
    }
    return { email: String(value).trim() };
};

const recipients = (value) =>
    String(value)
        .split(",")
        .map(part => parseAddress(part))
        .filter(addr => addr && addr.email);

const sendViaBrevoApi = async (mailOptions) => {
    const sender = parseAddress(mailOptions.from) || {
        name: process.env.SMTP_FROM_NAME,
        email: process.env.SMTP_FROM_EMAIL
    };

    // Same ~15s ceiling the SMTP socket had: the HTTP request is awaited inside
    // the API handler, so it must stay inside the app's read timeout.
    const abort = AbortSignal.timeout(15000);

    const response = await fetch(BREVO_API_URL, {
        method: "POST",
        headers: {
            "api-key": process.env.BREVO_API_KEY,
            "content-type": "application/json",
            accept: "application/json"
        },
        body: JSON.stringify({
            sender,
            to: recipients(mailOptions.to),
            subject: mailOptions.subject,
            htmlContent: mailOptions.html,
            ...(mailOptions.text ? { textContent: mailOptions.text } : {})
        }),
        signal: abort
    });

    const body = (await response.text()).trim();
    if (!response.ok) {
        // Brevo answers 400 with {code, message} — surface it verbatim, since
        // "sender not verified" and "bad api key" need different fixes.
        throw new Error(`Brevo API ${response.status}: ${body.slice(0, 300)}`);
    }

    let messageId = "";
    try { messageId = JSON.parse(body).messageId || ""; } catch { /* body may be empty */ }
    return messageId;
};

const sendViaSmtp = async (mailOptions) => {
    const info = await transporter.sendMail(mailOptions);
    return info.messageId;
};

export const sendEmail = async (mailOptions) => {
    const label = `${mailOptions.to} | Subject: "${mailOptions.subject}"`;
    const useApi = Boolean(process.env.BREVO_API_KEY);
    let apiError = null;

    if (useApi) {
        try {
            const messageId = await sendViaBrevoApi(mailOptions);
            console.log(`✅ Email sent (Brevo API) → ${label} | MessageId: ${messageId || "n/a"}`);
            return { success: true, messageId, transport: "brevo-api" };
        } catch (error) {
            // Don't give up here: a key/sender problem should still get a chance
            // on SMTP, which works wherever outbound 587 is open.
            apiError = error.message;
            console.error(`⚠️  Brevo API send failed → ${label} | ${apiError} — falling back to SMTP`);
        }
    }

    try {
        const messageId = await sendViaSmtp(mailOptions);
        console.log(`✅ Email sent (SMTP) → ${label} | MessageId: ${messageId}`);
        return { success: true, messageId, transport: "smtp" };
    } catch (error) {
        const detail = apiError ? `${error.message} (API: ${apiError})` : error.message;
        console.error(`❌ Email failed → ${label} | Error: ${detail}`);
        if (!useApi && /ETIMEDOUT|ECONNREFUSED|ESOCKET|Greeting never received/i.test(error.message)) {
            console.error("   ↳ outbound SMTP looks blocked on this host — set BREVO_API_KEY to send over HTTPS instead.");
        }
        return { success: false, error: detail };
    }
};