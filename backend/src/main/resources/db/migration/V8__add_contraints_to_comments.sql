ALTER TABLE comments
ADD CONSTRAINT chk_comments_content_length
CHECK (char_length(content) <= 500);