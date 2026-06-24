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
    const baseUrl = `${req.protocol}://${req.get("host")}`;
    return `${baseUrl}/uploads/${folder}/${filename}`;
};