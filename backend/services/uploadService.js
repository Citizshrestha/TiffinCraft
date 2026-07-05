import cloudinary from '../config/cloudinary.js';
import { Readable } from 'stream';

/**
 * Upload image buffer to Cloudinary
 * @param {Buffer} fileBuffer - Image file buffer from multer
 * @param {string} folder - Cloudinary folder name (e.g., 'tiffincraft/meals')
 * @param {Object} options - Additional Cloudinary upload options
 * @returns {Promise<Object>} Cloudinary upload result
 */
export const uploadToCloudinary = (fileBuffer, folder, options = {}) => {
  return new Promise((resolve, reject) => {
    const uploadStream = cloudinary.uploader.upload_stream(
      {
        folder: folder,
        resource_type: 'auto',
        transformation: options.transformation || [
          { width: 1200, height: 1200, crop: 'limit' },
          { quality: 'auto:good' },
          { fetch_format: 'auto' }
        ],
        ...options
      },
      (error, result) => {
        if (error) {
          reject(error);
        } else {
          resolve(result);
        }
      }
    );

    // Convert buffer to stream and pipe to Cloudinary
    const bufferStream = Readable.from(fileBuffer);
    bufferStream.pipe(uploadStream);
  });
};

/**
 * Delete image from Cloudinary
 * @param {string} publicId - Cloudinary public ID of the image
 * @returns {Promise<Object>} Deletion result
 */
export const deleteFromCloudinary = async (publicId) => {
  try {
    const result = await cloudinary.uploader.destroy(publicId);
    return result;
  } catch (error) {
    throw new Error(`Failed to delete image: ${error.message}`);
  }
};

/**
 * Extract public ID from Cloudinary URL
 * @param {string} url - Cloudinary image URL
 * @returns {string} Public ID
 */
export const extractPublicId = (url) => {
  if (!url) return null;
  
  // Extract public ID from URL
  // Example: https://res.cloudinary.com/demo/image/upload/v1234567/folder/image.jpg
  // Returns: folder/image
  const parts = url.split('/');
  const uploadIndex = parts.indexOf('upload');
  if (uploadIndex === -1) return null;
  
  const publicIdParts = parts.slice(uploadIndex + 2); // Skip 'upload' and version
  const publicId = publicIdParts.join('/').split('.')[0]; // Remove extension
  
  return publicId;
};

/**
 * Upload multiple images
 * @param {Array<Buffer>} fileBuffers - Array of image buffers
 * @param {string} folder - Cloudinary folder name
 * @returns {Promise<Array<Object>>} Array of upload results
 */
export const uploadMultipleToCloudinary = async (fileBuffers, folder) => {
  try {
    const uploadPromises = fileBuffers.map(buffer => 
      uploadToCloudinary(buffer, folder)
    );
    return await Promise.all(uploadPromises);
  } catch (error) {
    throw new Error(`Failed to upload multiple images: ${error.message}`);
  }
};

export default {
  uploadToCloudinary,
  deleteFromCloudinary,
  extractPublicId,
  uploadMultipleToCloudinary
};
