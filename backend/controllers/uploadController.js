import { uploadToCloudinary, deleteFromCloudinary, extractPublicId } from '../services/uploadService.js';

/**
 * Upload meal image
 */
export const uploadMealImage = async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        message: 'No image file provided'
      });
    }

    // Upload to Cloudinary
    const result = await uploadToCloudinary(
      req.file.buffer,
      'tiffincraft/meals',
      {
        transformation: [
          { width: 800, height: 600, crop: 'fill', gravity: 'auto' },
          { quality: 'auto:good' },
          { fetch_format: 'auto' }
        ]
      }
    );

    res.status(200).json({
      success: true,
      message: 'Image uploaded successfully',
      data: {
        url: result.secure_url,
        publicId: result.public_id,
        width: result.width,
        height: result.height,
        format: result.format
      }
    });
  } catch (error) {
    console.error('Upload meal image error:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to upload image',
      error: error.message
    });
  }
};

/**
 * Upload profile image (cook or customer)
 */
export const uploadProfileImage = async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        message: 'No image file provided'
      });
    }

    // Upload to Cloudinary with circular crop for profile
    const result = await uploadToCloudinary(
      req.file.buffer,
      'tiffincraft/profiles',
      {
        transformation: [
          { width: 400, height: 400, crop: 'fill', gravity: 'face' },
          { quality: 'auto:good' },
          { fetch_format: 'auto' }
        ]
      }
    );

    res.status(200).json({
      success: true,
      message: 'Profile image uploaded successfully',
      data: {
        url: result.secure_url,
        publicId: result.public_id,
        width: result.width,
        height: result.height,
        format: result.format
      }
    });
  } catch (error) {
    console.error('Upload profile image error:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to upload profile image',
      error: error.message
    });
  }
};

/**
 * Upload cook certificate/document
 */
export const uploadDocument = async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        message: 'No document provided'
      });
    }

    // Upload to Cloudinary
    const result = await uploadToCloudinary(
      req.file.buffer,
      'tiffincraft/documents',
      {
        resource_type: 'auto' // Allows PDFs and other documents
      }
    );

    res.status(200).json({
      success: true,
      message: 'Document uploaded successfully',
      data: {
        url: result.secure_url,
        publicId: result.public_id,
        format: result.format
      }
    });
  } catch (error) {
    console.error('Upload document error:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to upload document',
      error: error.message
    });
  }
};

/**
 * Delete image from Cloudinary
 */
export const deleteImage = async (req, res) => {
  try {
    const { imageUrl } = req.body;

    if (!imageUrl) {
      return res.status(400).json({
        success: false,
        message: 'Image URL is required'
      });
    }

    // Extract public ID from URL
    const publicId = extractPublicId(imageUrl);
    
    if (!publicId) {
      return res.status(400).json({
        success: false,
        message: 'Invalid image URL'
      });
    }

    // Delete from Cloudinary
    const result = await deleteFromCloudinary(publicId);

    res.status(200).json({
      success: true,
      message: 'Image deleted successfully',
      data: result
    });
  } catch (error) {
    console.error('Delete image error:', error);
    res.status(500).json({
      success: false,
      message: 'Failed to delete image',
      error: error.message
    });
  }
};

export default {
  uploadMealImage,
  uploadProfileImage,
  uploadDocument,
  deleteImage
};
