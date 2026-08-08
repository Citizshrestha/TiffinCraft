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
 * POST /api/upload/chat-media
 * Upload a chat image/video to <role>/<username>/chats/images|videos.
 */
export const uploadChatMedia = async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ success: false, message: 'No media file provided.' });
    }

    const userId = req.user.id;
    const [users] = await db.promise().query(
      'SELECT full_name, email, role FROM users WHERE id = ?',
      [userId]
    );

    if (users.length === 0) {
      return res.status(404).json({ success: false, message: 'User not found.' });
    }

    const user = users[0];
    const role = sanitizeFolderName(user.role || req.user.role || 'user');
    const username = sanitizeFolderName(user.full_name || user.email || String(userId));
    const isVideo = req.file.mimetype.startsWith('video/');
    const mediaFolder = isVideo ? 'videos' : 'images';
    const folder = `${role}/${username}/chats/${mediaFolder}`;

    const result = await uploadToCloudinary(req.file.buffer, folder, {
      resource_type: 'auto',
      transformation: isVideo
        ? undefined
        : [
            { width: 1200, height: 1200, crop: 'limit' },
            { quality: 'auto:good' },
            { fetch_format: 'auto' },
          ],
    });

    return res.status(200).json({
      success: true,
      message: 'Chat media uploaded successfully.',
      data: {
        url: result.secure_url,
        publicId: result.public_id,
        width: result.width,
        height: result.height,
        format: result.format,
        resourceType: result.resource_type,
      },
    });
  } catch (error) {
    console.error('uploadChatMedia error:', error);
    return res.status(500).json({
      success: false,
      message: 'Failed to upload chat media.',
      error: error.message,
    });
  }
};

/**
 * POST /api/upload/bank-qr
 * Upload a payment QR code (eSewa / Khalti / Bank) for either a cook (their
 * own receiving QR, shown to customers) or the admin (the platform's QR,
 * shown to cooks paying their commission — see commissionController.js).
 * Stored under cook/<cook name>/Bank Details/<Esewa|Khalti|Bank>/ or
 * admin/Platform/Bank Details/<Esewa|Khalti|Bank>/ so QR codes are easy to
 * find in the Cloudinary media library. If the caller already has a QR of
 * this type, the old asset is deleted first so replacing one doesn't leave
 * orphaned files behind.
 */
const QR_TYPE_LABELS = { esewa: 'Esewa', khalti: 'Khalti', bank: 'Bank' };
const QR_TYPE_DB_KEYS = { esewa: 'esewa_qr_url', khalti: 'khalti_qr_url', bank: 'bank_qr_url' };

export const uploadBankQr = async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ success: false, message: 'No QR image provided.' });
    }

    const qrType = (req.body.qrType || '').toLowerCase();
    if (!QR_TYPE_LABELS[qrType]) {
      return res.status(400).json({ success: false, message: 'qrType must be one of: esewa, khalti, bank.' });
    }

    const isAdmin = req.user.role === 'admin';
    let folderName, existingBankDetailsJson;

    if (isAdmin) {
      const [[settings]] = await db.promise().query(
        'SELECT bank_details FROM platform_settings WHERE id = 1'
      );
      folderName = 'Platform';
      existingBankDetailsJson = settings ? settings.bank_details : null;
    } else {
      const userId = req.user.id;
      const [profiles] = await db.promise().query(
        'SELECT cp.kitchen_name, cp.bank_details, u.full_name FROM cook_profiles cp ' +
        'JOIN users u ON u.id = cp.user_id WHERE cp.user_id = ?',
        [userId]
      );

      if (profiles.length === 0) {
        return res.status(404).json({ success: false, message: 'Cook profile not found.' });
      }

      folderName = sanitizeFolderName(profiles[0].kitchen_name || profiles[0].full_name || String(userId))
        .replace(/_/g, ' ')
        .replace(/\b\w/g, (c) => c.toUpperCase());
      existingBankDetailsJson = profiles[0].bank_details;
    }

    // Delete the previous QR of this type, if one exists, so re-uploading doesn't
    // leave the old image orphaned in Cloudinary.
    try {
      const existing = existingBankDetailsJson ? JSON.parse(existingBankDetailsJson) : null;
      const oldUrl = existing ? existing[QR_TYPE_DB_KEYS[qrType]] : null;
      if (oldUrl) {
        const oldPublicId = extractPublicId(oldUrl);
        if (oldPublicId) await deleteFromCloudinary(oldPublicId).catch(() => {});
      }
    } catch (_) {
      // Malformed/legacy bank_details JSON — nothing to clean up, continue.
    }

    const folder = isAdmin
      ? `admin/Platform/Bank Details/${QR_TYPE_LABELS[qrType]}`
      : `cook/${folderName}/Bank Details/${QR_TYPE_LABELS[qrType]}`;

    const result = await uploadToCloudinary(req.file.buffer, folder, {
      transformation: [
        { width: 1000, height: 1000, crop: 'limit' },
        { quality: 'auto:good' },
        { fetch_format: 'auto' },
      ],
    });

    return res.status(200).json({
      success: true,
      message: 'QR code uploaded successfully.',
      data: {
        url: result.secure_url,
        publicId: result.public_id,
        width: result.width,
        height: result.height,
        format: result.format,
      },
    });
  } catch (error) {
    console.error('uploadBankQr error:', error);
    return res.status(500).json({ success: false, message: 'Failed to upload QR code.', error: error.message });
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

export default { uploadMealImage, uploadProfileImage, uploadDocument, uploadChatMedia, uploadBankQr, deleteImage };

