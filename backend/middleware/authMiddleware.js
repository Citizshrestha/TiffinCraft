import jwt from "jsonwebtoken";
import db from "../config/db.js";

const verifyToken = async (req, res, next) => {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith("Bearer ")) {
        return res.status(401).json({ message: "No token provided" });
    }

    const token = authHeader.split(" ")[1];

    let decoded;
    try {
        decoded = jwt.verify(token, process.env.JWT_SECRET);
    } catch (_error) {
        return res.status(401).json({ message: "Invalid or expired token" });
    }

    try {
        const [users] = await db.promise().query(
            "SELECT id, role, is_active FROM users WHERE id = ? LIMIT 1",
            [decoded.id]
        );
        const currentUser = users[0];
        if (!currentUser || !currentUser.is_active) {
            return res.status(401).json({ message: "Account is inactive or no longer exists" });
        }

        // Never trust a role cached in a week-old token. Admin role changes and
        // deactivation must take effect on the very next request.
        req.user = {
            ...decoded,
            id: currentUser.id,
            role: currentUser.role
        };
        next();
    } catch (error) {
        console.error("Authentication account lookup failed:", error);
        return res.status(503).json({ message: "Authentication service temporarily unavailable" });
    }
};

export const protect = verifyToken;

export const roleOnly = (...roles) => (req, res, next) => {
    if (!req.user) {
        return res.status(401).json({ message: "Not authenticated" });
    }
    if (!roles.includes(req.user.role)) {
        return res.status(403).json({ message: "Access denied: insufficient role" });
    }
    next();
};

export const authMiddleware = verifyToken;

export default verifyToken;
