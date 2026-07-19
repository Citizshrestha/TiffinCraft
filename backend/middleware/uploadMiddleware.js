import multer from 'multer';

// Configure multer to use memory storage (temporary)
// Files will be held in memory as Buffer objects
const storage = multer.memoryStorage();

// File filter to accept only images
const fileFilter = (req, file, cb) => {
  const allowedMimeTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
  
  if (allowedMimeTypes.includes(file.mimetype)) {
    cb(null, true);
  } else {
    cb(new Error('Invalid file type. Only JPEG, PNG, GIF, and WebP images are allowed.'), false);
  }
};

// Configure multer
const upload = multer({
  storage: storage,
  fileFilter: fileFilter,
  limits: {
    fileSize: 5 * 1024 * 1024 // 5MB limit
  }
});

const chatMediaFileFilter = (req, file, cb) => {
  const allowedMimeTypes = [
    'image/jpeg',
    'image/jpg',
    'image/png',
    'image/gif',
    'image/webp',
    'video/mp4',
    'video/mpeg',
    'video/quicktime',
    'video/webm',
    'video/x-matroska',
    'video/3gpp'
  ];

  if (allowedMimeTypes.includes(file.mimetype)) {
    cb(null, true);
  } else {
    cb(new Error('Invalid file type. Only images and videos are allowed.'), false);
  }
};

const chatMediaUpload = multer({
  storage: storage,
  fileFilter: chatMediaFileFilter,
  limits: {
    fileSize: 100 * 1024 * 1024
  }
});

// Middleware for single image upload
export const uploadSingle = (fieldName) => upload.single(fieldName);

// Middleware for multiple images upload
export const uploadMultiple = (fieldName, maxCount = 10) => upload.array(fieldName, maxCount);

// Middleware for multiple fields with images
export const uploadFields = (fields) => upload.fields(fields);

// Middleware for chat image/video upload
export const uploadChatMediaSingle = (fieldName) => chatMediaUpload.single(fieldName);

// Error handler for multer errors
export const handleUploadError = (error, req, res, next) => {
  if (error instanceof multer.MulterError) {
    if (error.code === 'LIMIT_FILE_SIZE') {
      return res.status(400).json({
        success: false,
        message: 'File size too large. Maximum size is 5MB.'
      });
    }
    if (error.code === 'LIMIT_FILE_COUNT') {
      return res.status(400).json({
        success: false,
        message: 'Too many files uploaded.'
      });
    }
    return res.status(400).json({
      success: false,
      message: `Upload error: ${error.message}`
    });
  }
  
  if (error) {
    return res.status(400).json({
      success: false,
      message: error.message
    });
  }
  
  next();
};

export default {
  uploadSingle,
  uploadMultiple,
  uploadFields,
  uploadChatMediaSingle,
  handleUploadError
};

