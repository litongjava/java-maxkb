-- Retain full descriptive filenames used by legislation and public notices.
ALTER TABLE max_kb_file ALTER COLUMN filename TYPE text;
