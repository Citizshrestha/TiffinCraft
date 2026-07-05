import cloudinary from '../config/cloudinary.js';
import { Readable } from 'stream';

/**
 * Upload image buffer to Cloudinary
 * @param {Buffer} fileBuffer - Image file buffer from multer memoryStorage
 * @param {string} folder     - Cloudinary folder path (e.g. 'tiffincraft/profiles/john_doe')
 * @param {Object} options    - Extra Cloudinary upload options
 * @returns {Promise<Object>} Cloudinary upload result
 */
export const uploadToCloudinary = (fileBuffer, folder, options = {}) => {
  return new Promise((resolve, reject) => {
    const { transformation, ...restOptions } = options;

    const uploadStream = cloudinary.uploader.upload_stream(
      {
        folder,
        resource_type: 'image',
        transformation: transformation || [
          { width: 1200, height: 1200, crop: 'limit' },
          { quality: 'auto:good' },
          { fetch_format: 'auto' },
        ],
        ...restOptions,
      },
      (error, result) => {
        if (error) reject(error);
        else resolve(result);
      }
    );

    Readable.from(fileBuffer).pipe(uploadStream);
  });
};

/**
 * Delete an image from Cloudinary by its public_id
 * @param {string} publicId - Cloudinary public_id
 */
export const deleteFromCloudinary = async (publicId) => {
  try {
    return await cloudinary.uploader.destroy(publicId);
  } catch (error) {
    throw new Error(`Failed to delete image from Cloudinary: ${error.message}`);
  }
};

/**
 * Extract the Cloudinary public_id from a secure_url
 * Example URL: https://res.cloudinary.com/<cloud>/image/upload/v1234/folder/sub/name.jpg
 * Returns: folder/sub/name
 */
export const extractPublicId = (url) => {
  if (!url) return null;
  const parts = url.split('/');
  const uploadIndex = parts.indexOf('upload');
  if (uploadIndex === -1) return null;
  // Skip 'upload' and the version segment (v1234…)
  const afterUpload = parts.slice(uploadIndex + 1);
  const startIndex = afterUpload[0]?.startsWith('v') && /^v\d+$/.test(afterUpload[0]) ? 1 : 0;
  return afterUpload.slice(startIndex).join('/').replace(/\.[^/.]+$/, '');
};

/**
 * Upload multiple images to the same folder
 */
export const uploadMultipleToCloudinary = async (fileBuffers, folder) => {
  return Promise.all(fileBuffers.map((buf) => uploadToCloudinary(buf, folder)));
};

/**
 * Build a sanitized Cloudinary folder-safe string from any identifier
 * Lowercases, strips special characters, replaces spaces/@ with underscores
 */
export const sanitizeFolderName = (name) => {
  if (!name) return 'unknown';
  return name
    .toLowerCase()
    .replace(/@/g, '_at_')
    .replace(/[^a-z0-9_.-]/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_|_$/g, '');
};

export default {
  uploadToCloudinary,
  deleteFromCloudinary,
  extractPublicId,
  uploadMultipleToCloudinary,
  sanitizeFolderName,
};
