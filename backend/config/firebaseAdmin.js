import { initializeApp, cert } from "firebase-admin/app";
import { getMessaging } from "firebase-admin/messaging";
import { readFileSync, existsSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

let firebaseApp = null;

/**
 * Initialize Firebase Admin SDK for FCM push notifications.
 *
 * Credential sources, in priority order:
 *   1. FIREBASE_SERVICE_ACCOUNT_JSON — the whole service-account JSON as a
 *      single env var. This is the deployment path (Render, Heroku, Docker):
 *      hosts give you env vars, not a filesystem to drop secrets onto.
 *   2. FIREBASE_SERVICE_ACCOUNT_PATH — path to the JSON file (local dev).
 *   3. FIREBASE_PROJECT_ID + FIREBASE_CLIENT_EMAIL + FIREBASE_PRIVATE_KEY.
 *
 * Never throws: an unconfigured Firebase disables push notifications, it does
 * not take the API down with it.
 */
export const initFirebase = () => {
  if (firebaseApp) return firebaseApp;

  // 1. Full JSON in one env var — the production/hosted path.
  const saJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (saJson && saJson.trim()) {
    try {
      const serviceAccount = JSON.parse(saJson);
      // Pasting into a dashboard textarea commonly turns the real newlines in
      // private_key into literal backslash-n. Repair rather than fail.
      if (typeof serviceAccount.private_key === "string") {
        serviceAccount.private_key = serviceAccount.private_key.replace(/\\n/g, "\n");
      }
      firebaseApp = initializeApp({ credential: cert(serviceAccount) });
      console.log("✅ Firebase Admin initialized (FIREBASE_SERVICE_ACCOUNT_JSON)");
      return firebaseApp;
    } catch (err) {
      // Deliberately does not echo the value — it contains a private key.
      console.error(
        `❌ FIREBASE_SERVICE_ACCOUNT_JSON is set but unusable (${err.message}). ` +
        "Expected the entire service-account JSON as one value. Falling through."
      );
    }
  }

  // 2. Service-account file on disk — local development.
  const saPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
  if (saPath && existsSync(saPath)) {
    const serviceAccount = JSON.parse(readFileSync(saPath, "utf-8"));
    firebaseApp = initializeApp({
      credential: cert(serviceAccount),
    });
    console.log("✅ Firebase Admin initialized (service-account file)");
    return firebaseApp;
  }

  // 3. Fallback: individual env vars
  const projectId = process.env.FIREBASE_PROJECT_ID;
  const clientEmail = process.env.FIREBASE_CLIENT_EMAIL;
  const privateKey = process.env.FIREBASE_PRIVATE_KEY;

  if (projectId && clientEmail && privateKey) {
    firebaseApp = initializeApp({
      credential: cert({
        projectId,
        clientEmail,
        privateKey: privateKey.replace(/\\n/g, "\n"),
      }),
    });
    console.log("✅ Firebase Admin initialized (env-var credentials)");
    return firebaseApp;
  }

  console.warn(
    "⚠️  Firebase not configured — set FIREBASE_SERVICE_ACCOUNT_JSON (recommended for " +
    "hosted deploys), or FIREBASE_SERVICE_ACCOUNT_PATH, or FIREBASE_PROJECT_ID + " +
    "FIREBASE_CLIENT_EMAIL + FIREBASE_PRIVATE_KEY, to enable FCM push notifications."
  );
  return null;
};

/**
 * Get the initialized Firebase Admin instance (or null if not configured).
 */
export const getFirebaseApp = () => firebaseApp;

/**
 * Send an FCM push notification to a specific device.
 * @param {string} fcmToken - The recipient's FCM device token
 * @param {string} title - Notification title
 * @param {string} body - Notification body text
 * @param {object} data - Optional data payload (key-value pairs, values must be strings)
 * @returns {Promise<{success: boolean, messageId?: string, error?: string}>}
 */
export const sendPush = async (fcmToken, title, body, data = {}) => {
  if (!fcmToken) {
    return { success: false, error: "No FCM token provided" };
  }

  const app = getFirebaseApp();
  if (!app) {
    return { success: false, error: "Firebase not initialized" };
  }

  try {
    // Always include title + body inside the data payload as well as in the
    // notification block.  When FCM delivers a data-only message (or when the
    // app is in the foreground), FcmService.onMessageReceived() reads from
    // data; the notification block is used by the system tray when the app is
    // killed/background.  Having both means every delivery path works.
    const message = {
      token: fcmToken,
      notification: { title, body },
      data: Object.fromEntries(
        Object.entries({ title, body, ...data }).map(([k, v]) => [k, String(v)])
      ),
      android: {
        priority: "high",
        notification: {
          channelId: "tiffincraft_alerts",
          sound: "default",
        },
      },
    };

    const response = await getMessaging().send(message);
    return { success: true, messageId: response };
  } catch (error) {
    console.error(`❌ FCM push failed for token ${fcmToken?.slice(0, 10)}...: ${error.message}`);

    // Token is invalid — remove it so we don't retry forever
    if (error.code === "messaging/invalid-registration-token" ||
        error.code === "messaging/registration-token-not-registered") {
      try {
        const { default: db } = await import("../config/db.js");
        await db.promise().query(
          "UPDATE users SET fcm_token = NULL WHERE fcm_token = ?",
          [fcmToken]
        );
        console.log(`🧹 Cleared stale FCM token for user`);
      } catch (dbErr) {
        console.error("Failed to clear stale FCM token:", dbErr.message);
      }
    }

    return { success: false, error: error.message };
  }
};

export default { initFirebase, getFirebaseApp, sendPush };
