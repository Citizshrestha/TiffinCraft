import cloudinary from '../config/cloudinary.js';
import { Readable } from 'stream';


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


export const deleteFromCloudinary = async (publicId, resourceType = "image") => {
  try {
    return await cloudinary.uploader.destroy(publicId, {
      resource_type: resourceType || "image",
    });
  } catch (error) {
    throw new Error(`Failed to delete media from Cloudinary: ${error.message}`);
  }
};


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


export const uploadMultipleToCloudinary = async (fileBuffers, folder) => {
  return Promise.all(fileBuffers.map((buf) => uploadToCloudinary(buf, folder)));
};


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
