ALTER TABLE module
  ADD COLUMN title VARCHAR(128),
  ADD COLUMN keywords TEXT[],
  ADD COLUMN description TEXT;