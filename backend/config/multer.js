import multer from "multer";
import path from "path";
import { v4 as uuidv4 } from "uuid";
import fs from "fs";

const createDir = (dirPath) => {
    if (!fs.existsSync(dirPath)) {
        fs.mkdirSync(dirPath, { recursive: true });
        console.log(`📁 Created directory: ${dirPath}`);
    }
};

createDir("uploads/profiles");
createDir("uploads/meals");
createDir("uploads/temp");

const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        if (req.uploadType === "profile") {
            cb(null, "uploads/profiles/");
        } else if (req.uploadType === "meal") {
            cb(null, "uploads/meals/");
        } else {
            cb(null, "uploads/temp/");
        }
    },
    filename: (req, file, cb) => {
        const ext = path.extname(file.originalname).toLowerCase();
        const uniqueName = `${uuidv4()}${ext}`;
        cb(null, uniqueName);
    }
});

const fileFilter = (req, file, cb) => {
    const allowedTypes = ["image/jpeg", "image/jpg", "image/png", "image/webp"];

    if (allowedTypes.includes(file.mimetype)) {
        cb(null, true);
    } else {
        cb(new Error("Only JPEG, PNG and WebP images are allowed."), false);
    }
};

const upload = multer({
    storage,
    fileFilter,
    limits: {
        fileSize: 5 * 1024 * 1024, // 5MB max
    }
});

export default upload;