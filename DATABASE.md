# Database

Room database name: `book-reader.db` (version 1).

## Entities

### Book
`id`, `title`, `author`, `coverPath`, `sourceType` (`CAMERA_SCAN`/`PDF`/`MIXED`), `language` (BCP-47), `totalPages`, `createdAt`, `updatedAt`

### BookPage
`id`, `bookId` (cascade), `pageNumber` (unique per book), `imagePath`, `text`, `language`, `processingStatus` (`PENDING`/`PROCESSING`/`COMPLETED`/`FAILED`/`RETRYING`), `errorMessage`, `hasSelectableText`

### BookTextBlock
`id`, `pageId`, `sequence`, `text`, `language`, `startOffset`, `endOffset`, bounding box floats. Used for future word-level highlighting.

### ReadingProgress
Primary key `bookId`. `pageNumber`, `segmentId`, `characterOffset`, `updatedAt`.

### AudioSegment
`id`, `bookPageId`, `sequence`, `textHash` (never the raw sentence in queries we log), `language`, `audioPath`, `durationMs`, `voiceProfileId`, `status`, unique `cacheKey`.

### VoiceProfile
`id`, `name`, `provider`, `providerVoiceId`, `supportedLanguages`, `experimentalLanguages`, `status`, `isCloned`, `qualityNote`, timestamps.

### Bookmark / Note
Local only. Notes store the user’s comment and optional selected text.

## Backend SQLite

Separate from the phone:

- `users` — email + password hash (bcrypt)
- `voices` — provider ids and capability flags
- `audio_cache` — hash → file path

Book content is **not** synced to the server in this MVP.

## Indexes

- Pages: `(bookId, pageNumber)` unique
- Audio: unique `cacheKey`
- Voices/bookmarks/notes: `bookId` / `user_email`
