import upload from "../config/multer.js";


export const uploadProfileImage = (req, res, next) => {
    req.uploadType = "profile";
    upload.single("profile_image")(req, res, (err) => {
        if (err) {
            return res.status(400).json({
                success: false,
                message: err.message || "File upload failed."
            });
        }
        next();
    });
};

export const uploadMealImage = (req, res, next) => {
    req.uploadType = "meal";
    upload.single("meal_image")(req, res, (err) => {
        if (err) {
            return res.status(400).json({
                success: false,
                message: err.message || "File upload failed."
            });
        }
        next();
    });
};