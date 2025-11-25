-- Sample data for GeoBook app

-- Insert users
INSERT INTO users (username, password, email) VALUES ('user1', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.9cTQjwW9iFzJ4E8XeOwVqE7vQKcKPj2', 'user1@example.com');

-- Insert books
INSERT INTO books (title, author, description) VALUES ('Around the World in 80 Days', 'Jules Verne', 'A classic adventure story.');
INSERT INTO books (title, author, description) VALUES ('The Hobbit', 'J.R.R. Tolkien', 'A fantasy adventure.');

-- Insert chapters
INSERT INTO chapters (book_id, title, location_description) VALUES (1, 'Chapter 1', 'Content of chapter 1.');
INSERT INTO chapters (book_id, title, location_description) VALUES (1, 'Chapter 2', 'Content of chapter 2.');
INSERT INTO chapters (book_id, title, location_description) VALUES (2, 'Chapter 1', 'Hobbit content.');

-- Insert locations
INSERT INTO locations (chapter_id, latitude, longitude, place_name) VALUES (1, 51.5074, -0.1278, 'London');
INSERT INTO locations (chapter_id, latitude, longitude, place_name) VALUES (1, 48.8566, 2.3522, 'Paris');
INSERT INTO locations (chapter_id, latitude, longitude, place_name) VALUES (2, 40.7128, -74.0060, 'New York');
INSERT INTO locations (chapter_id, latitude, longitude, place_name) VALUES (3, 37.7749, -122.4194, 'San Francisco');
