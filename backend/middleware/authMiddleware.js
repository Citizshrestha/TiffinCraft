import jwt from "jsonwebtoken";

// Protect middleware - verifies JWT token
export const protect = (req, res, next) => {
    try {
        const authHeader = req.headers.authorization;

        if (!authHeader || !authHeader.startsWith("Bearer ")) {
            return res.status(401).json({
                success: false,
                message: "No token provided. Access denied."
            });
        }

        const token = authHeader.split(" ")[1];

        const decoded = jwt.verify(token, process.env.JWT_SECRET);

        req.user = {
            id: decoded.id,
            role: decoded.role
        };

        next();

    } catch (error) {
        if (error.name === "TokenExpiredError") {
            return res.status(401).json({
                success: false,
                message: "Session expired. Please login again."
            });
        }
        return res.status(401).json({
            success: false,
            message: "Invalid token. Access denied."
        });
    }
};

// Role-based access control - pass one or more allowed roles
// Usage: roleOnly("cook") or roleOnly("admin", "cook")
export const roleOnly = (...roles) => {
    return (req, res, next) => {
        if (!roles.includes(req.user.role)) {
            return res.status(403).json({
                success: false,
                message: `Access denied. Only ${roles.join(" or ")} can do this.`
            });
        }
        next();
    };
};
