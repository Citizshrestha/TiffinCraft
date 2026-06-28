import fs from "fs";
import path from "path";

export const deleteFile = (filePath) => {
    if (!filePath) return;

    const relativePath = filePath.includes("uploads/")
        ? filePath.split("uploads/")[1]
        : filePath;

    const fullPath = path.join("uploads", relativePath);

    if (fs.existsSync(fullPath)) {
        fs.unlinkSync(fullPath);
        console.log(`🗑️  Deleted file: ${fullPath}`);
    }
};

export const buildFileUrl = (req, folder, filename) => {
    if (!filename) return null;
    // Store relative path in database - app will add base URL
    return `/uploads/${folder}/${filename}`;
};