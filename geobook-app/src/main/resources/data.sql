-- Sample data for GeoBook app

-- Insert users only if not exists
INSERT INTO users (username, password, email) 
SELECT 'user1', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.9cTQjwW9iFzJ4E8XeOwVqE7vQKcKPj2', 'user1@example.com'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'user1');

-- Insert books only if not exists
INSERT INTO books (title, author, description) 
SELECT 'Around the World in 80 Days', 'Jules Verne', 'A classic adventure story.'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM books WHERE title = 'Around the World in 80 Days');

INSERT INTO books (title, author, description) 
SELECT 'The Hobbit', 'J.R.R. Tolkien', 'A fantasy adventure.'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM books WHERE title = 'The Hobbit');

-- Insert chapters only if not exists
INSERT INTO chapters (book_id, title, location_description) 
SELECT 1, 'Chapter 1', 'Content of chapter 1.'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM chapters WHERE book_id = 1 AND title = 'Chapter 1');

INSERT INTO chapters (book_id, title, location_description) 
SELECT 1, 'Chapter 2', 'Content of chapter 2.'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM chapters WHERE book_id = 1 AND title = 'Chapter 2');

INSERT INTO chapters (book_id, title, location_description) 
SELECT 2, 'Chapter 1', 'Hobbit content.'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM chapters WHERE book_id = 2 AND title = 'Chapter 1');

-- Insert locations only if not exists
INSERT INTO locations (chapter_id, latitude, longitude, place_name) 
SELECT 1, 51.5074, -0.1278, 'London'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM locations WHERE chapter_id = 1 AND place_name = 'London');

INSERT INTO locations (chapter_id, latitude, longitude, place_name) 
SELECT 1, 48.8566, 2.3522, 'Paris'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM locations WHERE chapter_id = 1 AND place_name = 'Paris');

INSERT INTO locations (chapter_id, latitude, longitude, place_name) 
SELECT 2, 40.7128, -74.0060, 'New York'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM locations WHERE chapter_id = 2 AND place_name = 'New York');

INSERT INTO locations (chapter_id, latitude, longitude, place_name) 
SELECT 3, 37.7749, -122.4194, 'San Francisco'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM locations WHERE chapter_id = 3 AND place_name = 'San Francisco');
