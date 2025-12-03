-- Sample data for GeoBook app - Oracle version

-- Insert users only if not exists
MERGE INTO users u
USING (SELECT 'user' as username, '$2a$10$dXJ3SW6G7P50lGmMkkmwe.9cTQjwW9iFzJ4E8XeOwVqE7vQKcKPj2' as password, 'user@example.com' as email FROM dual) src
ON (u.username = src.username)
WHEN NOT MATCHED THEN
  INSERT (username, password, email) VALUES (src.username, src.password, src.email);

-- Insert books only if not exists
MERGE INTO books b
USING (SELECT 'Around the World in 80 Days' as title, 'Jules Verne' as author, 'A classic adventure story.' as description FROM dual) src
ON (b.title = src.title)
WHEN NOT MATCHED THEN
  INSERT (title, author, description) VALUES (src.title, src.author, src.description);

MERGE INTO books b
USING (SELECT 'The Hobbit' as title, 'J.R.R. Tolkien' as author, 'A fantasy adventure.' as description FROM dual) src
ON (b.title = src.title)
WHEN NOT MATCHED THEN
  INSERT (title, author, description) VALUES (src.title, src.author, src.description);

-- Insert chapters only if not exists (assuming book IDs 1 and 2 exist)
MERGE INTO chapters c
USING (SELECT 1 as book_id, 'Chapter 1' as title, 'Content of chapter 1.' as location_description FROM dual) src
ON (c.book_id = src.book_id AND c.title = src.title)
WHEN NOT MATCHED THEN
  INSERT (book_id, title, location_description) VALUES (src.book_id, src.title, src.location_description);

MERGE INTO chapters c
USING (SELECT 1 as book_id, 'Chapter 2' as title, 'Content of chapter 2.' as location_description FROM dual) src
ON (c.book_id = src.book_id AND c.title = src.title)
WHEN NOT MATCHED THEN
  INSERT (book_id, title, location_description) VALUES (src.book_id, src.title, src.location_description);

MERGE INTO chapters c
USING (SELECT 2 as book_id, 'Chapter 1' as title, 'Hobbit content.' as location_description FROM dual) src
ON (c.book_id = src.book_id AND c.title = src.title)
WHEN NOT MATCHED THEN
  INSERT (book_id, title, location_description) VALUES (src.book_id, src.title, src.location_description);

-- Insert locations only if not exists (assuming chapter IDs exist)
MERGE INTO locations l
USING (SELECT 1 as chapter_id, 51.5074 as latitude, -0.1278 as longitude, 'London' as place_name FROM dual) src
ON (l.chapter_id = src.chapter_id AND l.place_name = src.place_name)
WHEN NOT MATCHED THEN
  INSERT (chapter_id, latitude, longitude, place_name) VALUES (src.chapter_id, src.latitude, src.longitude, src.place_name);

MERGE INTO locations l
USING (SELECT 1 as chapter_id, 48.8566 as latitude, 2.3522 as longitude, 'Paris' as place_name FROM dual) src
ON (l.chapter_id = src.chapter_id AND l.place_name = src.place_name)
WHEN NOT MATCHED THEN
  INSERT (chapter_id, latitude, longitude, place_name) VALUES (src.chapter_id, src.latitude, src.longitude, src.place_name);

MERGE INTO locations l
USING (SELECT 2 as chapter_id, 40.7128 as latitude, -74.0060 as longitude, 'New York' as place_name FROM dual) src
ON (l.chapter_id = src.chapter_id AND l.place_name = src.place_name)
WHEN NOT MATCHED THEN
  INSERT (chapter_id, latitude, longitude, place_name) VALUES (src.chapter_id, src.latitude, src.longitude, src.place_name);

COMMIT;
