# How to Hide Contributors on GitHub

## Quick Steps:

1. **Update .mailmap file** (already created):
   - Open `.mailmap` file in the root directory
   - Replace the commented examples with your actual name and email
   - Map unwanted contributor emails to yours

2. **Example .mailmap content:**
   ```
   Your Name <your@email.com> deessafoundation <noreply@deessafoundation.com>
   Your Name <your@email.com> claude <noreply@anthropic.com>
   Your Name <your@email.com> Claude <noreply@anthropic.com>
   ```

3. **Apply the changes:**
   ```bash
   git add .mailmap .gitattributes .gitignore
   git commit -m "chore: configure contributor mapping and cleanup docs"
   git push
   ```

4. **Force GitHub to update contributors (optional):**
   ```bash
   # Make a small change to trigger recalculation
   git commit --allow-empty -m "chore: refresh contributor stats"
   git push
   ```

## What was done:

✅ Deleted unwanted MD files:
- CRASH_DEBUG_GUIDE.md
- DEBUG_MEALS_NOT_SHOWING.md
- FACEBOOK_OAUTH_REMOVAL.md
- FINAL_COMPLETE_FIX.md
- FIX_NETWORK_ERROR.md
- MEAL_INTEGRATION_COMPLETE.md
- PROFILE_PICTURE_UPLOAD_IMPLEMENTATION.md
- PROFILE_UPLOAD_QUICK_REFERENCE.md
- README_VSCODE.md
- SAFE_UPDATE_GUIDE.md
- START_MYSQL.md
- backend/API_DOCUMENTATION.md
- backend/DATABASE_UPDATE_REQUIRED.md

✅ Created .gitattributes to mark AI folders as generated
✅ Updated .gitignore to ignore future doc files
✅ Created .mailmap template for hiding contributors

## Remaining README files:
- README.md (root)
- backend/README.md

These are kept as they're the main project documentation.
