import { uploadToCloudinary, deleteFromCloudinary, extractPublicId, sanitizeFolderName } from '../services/uploadService.js';
import db from '../config/db.js';

/**
 * POST /api/upload/meal-image
 * Generic meal image upload — uses the authenticated user's ID as the cook folder.
 * For the meal-specific route (PUT /api/meals/:mealId/image) use mealController instead.
 */
export const uploadMealImage = async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ success: false, message: 'No image file provided.' });
    }

    const cookId = req.user.id;
    const folder = `tiffincraft/meals/${cookId}`;

    const result = await uploadToCloudinary(req.file.buffer, folder, {
      transformation: [
        { width: 800, height: 600, crop: 'fill', gravity: 'auto' },
        { quality: 'auto:good' },
        { fetch_format: 'auto' },
      ],
    });

    return res.status(200).json({
      success: true,
      message: 'Image uploaded successfully.',
      data: {
        url: result.secure_url,
        publicId: result.public_id,
        width: result.width,
        height: result.height,
        format: result.format,
      },
    });
  } catch (error) {
    console.error('uploadMealImage (uploadController) error:', error);
    return res.status(500).json({ success: false, message: 'Failed to upload image.', error: error.message });
  }
};

/**
 * POST /api/upload/profile-image
 * Generic profile image upload — scoped to tiffincraft/profiles/<username>/
 */
export const uploadProfileImage = async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ success: false, message: 'No image file provided.' });
    }

    const userId = req.user.id;

    // Fetch user to build folder name and delete old image
    const [users] = await db.promise().query(
      'SELECT profile_image, full_name, email FROM users WHERE id = ?',
      [userId]
    );

    // Delete old Cloudinary image if it exists
    if (users[0]?.profile_image) {
      const oldPublicId = extractPublicId(users[0].profile_image);
      if (oldPublicId) {
        await deleteFromCloudinary(oldPublicId).catch(() => {});
      }
    }

    const username = sanitizeFolderName(users[0]?.full_name || users[0]?.email || String(userId));
    const folder = `tiffincraft/profiles/${username}`;

    const result = await uploadToCloudinary(req.file.buffer, folder, {
      transformation: [
        { width: 400, height: 400, crop: 'fill', gravity: 'face' },
        { quality: 'auto:good' },
        { fetch_format: 'auto' },
      ],
    });

    const imageUrl = result.secure_url;

    // Persist new image URL
    await db.promise().query('UPDATE users SET profile_image = ? WHERE id = ?', [imageUrl, userId]);

    return res.status(200).json({
      success: true,
      message: 'Profile image uploaded successfully.',
      data: {
        url: imageUrl,
        publicId: result.public_id,
        width: result.width,
        height: result.height,
        format: result.format,
      },
    });
  } catch (error) {
    console.error('uploadProfileImage (uploadController) error:', error);
    return res.status(500).json({ success: false, message: 'Failed to upload profile image.', error: error.message });
  }
};

/**
 * POST /api/upload/document
 * Upload a cook certificate / document.
 * Stored under tiffincraft/documents/<userId>/
 */
export const uploadDocument = async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ success: false, message: 'No document provided.' });
    }

    const userId = req.user.id;
    const folder = `tiffincraft/documents/${userId}`;

    const result = await uploadToCloudinary(req.file.buffer, folder, {
      resource_type: 'auto',
      transformation: undefined, // no image transformation for documents
    });

    return res.status(200).json({
      success: true,
      message: 'Document uploaded successfully.',
      data: {
        url: result.secure_url,
        publicId: result.public_id,
        format: result.format,
      },
    });
  } catch (error) {
    console.error('uploadDocument error:', error);
    return res.status(500).json({ success: false, message: 'Failed to upload document.', error: error.message });
  }
};

/**
 * DELETE /api/upload/image
 * Delete an image from Cloudinary by its URL.
 */
export const deleteImage = async (req, res) => {
  try {
    const { imageUrl } = req.body;

    if (!imageUrl) {
      return res.status(400).json({ success: false, message: 'imageUrl is required.' });
    }

    const publicId = extractPublicId(imageUrl);
    if (!publicId) {
      return res.status(400).json({ success: false, message: 'Invalid Cloudinary image URL.' });
    }

    const result = await deleteFromCloudinary(publicId);

    return res.status(200).json({ success: true, message: 'Image deleted successfully.', data: result });
  } catch (error) {
    console.error('deleteImage error:', error);
    return res.status(500).json({ success: false, message: 'Failed to delete image.', error: error.message });
  }
};

export default { uploadMealImage, uploadProfileImage, uploadDocument, deleteImage };
