// Sends one real email through the same helper every app path uses, and reports
// which transport carried it. This is the check for "why did no code arrive?":
//   transport: "brevo-api" → HTTPS path works (the one that works on Render)
//   transport: "smtp"      → API key missing/rejected, SMTP covered for it
//   success: false         → the error text says which of the two to fix
//
// Usage: node scripts/send_test_email.mjs you@example.com
import { sendEmail, transporter } from "../utils/emailService.js";

const to = process.argv[2];
if (!to) {
    console.error("usage: node scripts/probe_send_mail.mjs <recipient>");
    process.exit(1);
}

const res = await sendEmail({
    from: `"${process.env.SMTP_FROM_NAME}" <${process.env.SMTP_FROM_EMAIL}>`,
    to,
    subject: "TiffinCraft — SMTP delivery probe",
    html: "<p>Delivery probe. If you can read this, the reset-code path can send mail.</p>"
});
console.log(JSON.stringify(res, null, 2));
transporter.close();
process.exit(res.success ? 0 : 1);
