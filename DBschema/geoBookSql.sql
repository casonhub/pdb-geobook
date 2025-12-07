-- GeoBook Database Schema for Oracle Database

-- Remove tables (in dependency order)
DROP TABLE multimedia CASCADE CONSTRAINTS PURGE;
DROP TABLE spatial_entities CASCADE CONSTRAINTS PURGE;
DROP TABLE locations CASCADE CONSTRAINTS PURGE;
DROP TABLE chapters CASCADE CONSTRAINTS PURGE;
DROP TABLE books CASCADE CONSTRAINTS PURGE;
DROP TABLE users CASCADE CONSTRAINTS PURGE;

-- Remove sequences
DROP SEQUENCE users_seq;
DROP SEQUENCE books_seq;
DROP SEQUENCE chapters_seq;
DROP SEQUENCE locations_seq;
DROP SEQUENCE multimedia_seq;
DROP SEQUENCE spatial_entities_seq;

PROMPT Database cleanup completed.

PROMPT Creating core application tables...

-- User Management Table
-- ========================================================================================================
CREATE TABLE users (
    user_id         NUMBER          PRIMARY KEY,
    username        VARCHAR2(100)   UNIQUE NOT NULL,
    password        VARCHAR2(255)   NOT NULL,
    email          VARCHAR2(255)   UNIQUE NOT NULL,
    enabled        CHAR(1)         DEFAULT 'Y' CHECK (enabled IN ('Y', 'N')),
    created_at     DATE            DEFAULT SYSDATE
);

COMMENT ON TABLE users IS 'Application users with authentication credentials';
COMMENT ON COLUMN users.enabled IS 'Account status: Y=Active, N=Disabled';

-- Book Catalog Table  
CREATE TABLE books (
                       book_id         NUMBER          PRIMARY KEY,
                       title          VARCHAR2(255)   NOT NULL,
                       author         VARCHAR2(255),
                       isbn           VARCHAR2(20)    UNIQUE,
                       description    CLOB
);

COMMENT ON TABLE books IS 'Central catalog of books with their geographic references';
COMMENT ON COLUMN books.isbn IS 'International Standard Book Number - unique identifier';

-- Chapter Organization Table
CREATE TABLE chapters (
                          chapter_id             NUMBER          PRIMARY KEY,
                          book_id               NUMBER          NOT NULL REFERENCES books(book_id) ON DELETE CASCADE,
                          chapter_number        NUMBER          NOT NULL,
                          title                 VARCHAR2(255),
                          location_description  CLOB,
                          CONSTRAINT uk_book_chapter UNIQUE (book_id, chapter_number)
);

COMMENT ON TABLE chapters IS 'Individual chapters within books, containing location references';
COMMENT ON COLUMN chapters.location_description IS 'Textual description of places mentioned in chapter';

-- Geographic Locations Table
CREATE TABLE locations (
                           location_id     NUMBER          PRIMARY KEY,
                           chapter_id      NUMBER          NOT NULL REFERENCES chapters(chapter_id) ON DELETE CASCADE,
                           latitude        FLOAT,
                           longitude       FLOAT,
                           place_name      VARCHAR2(255),
                           spatial_data SDO_GEOMETRY
);

COMMENT ON TABLE locations IS 'Geographic coordinates of places referenced in book content';
COMMENT ON COLUMN locations.spatial_data IS 'Spatial geometry data as text (for JPA compatibility)';

-- Multimedia Content Table
CREATE TABLE multimedia (
                            multimedia_id   NUMBER          PRIMARY KEY,
                            location_id     NUMBER          NOT NULL REFERENCES locations(location_id) ON DELETE CASCADE,
                            file_type       VARCHAR2(50)    NOT NULL,
                            file_path       VARCHAR2(500)   NOT NULL,
                            description     CLOB,
                            upload_date     DATE            DEFAULT SYSDATE,
                            is_active       CHAR(1)         DEFAULT 'Y' CHECK (is_active IN ('Y', 'N')),
                            thumbnail_path  VARCHAR2(500),

    -- Standard BLOB storage for images
                            image_blob      BLOB,

    -- Oracle Multimedia features (requires Oracle Multimedia installation)
                            image           ORDSYS.ORDImage,
                            image_si        ORDSYS.SI_StillImage,
                            image_ac        ORDSYS.SI_AverageColor,
                            image_ch        ORDSYS.SI_ColorHistogram,
                            image_pc        ORDSYS.SI_PositionalColor,
                            image_tx        ORDSYS.SI_Texture
);

COMMENT ON TABLE multimedia IS 'Images, videos, and other media associated with geographic locations';
COMMENT ON COLUMN multimedia.image_blob IS 'Fallback BLOB storage when Oracle Multimedia is unavailable';


PROMPT Creating spatial analysis tables...

-- Advanced Spatial Entities Table
CREATE TABLE spatial_entities (
    entity_id       NUMBER          PRIMARY KEY,
    name           VARCHAR2(255)   NOT NULL,
    entity_type    VARCHAR2(50)    NOT NULL CHECK (entity_type IN ('POINT', 'LINESTRING', 'POLYGON', 'CIRCLE', 'RECTANGLE')),
    geometry       VARCHAR2(4000)  NOT NULL, -- Using VARCHAR2 for JPA compatibility
    description    VARCHAR2(1000),
    color          VARCHAR2(20)    DEFAULT '#3388ff',
    created_date   TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_date   TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    chapter_id     NUMBER          REFERENCES chapters(chapter_id) ON DELETE SET NULL
);

COMMENT ON TABLE spatial_entities IS 'Advanced geometric entities for spatial analysis and visualization';
COMMENT ON COLUMN spatial_entities.entity_type IS 'Type of geometry: POINT, LINESTRING, POLYGON, CIRCLE, or RECTANGLE';
COMMENT ON COLUMN spatial_entities.color IS 'Hex color code for map visualization';


PROMPT Creating sequences and triggers for auto-increment...

-- Users sequence and trigger
CREATE SEQUENCE users_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER users_auto_id
    BEFORE INSERT ON users
    FOR EACH ROW
BEGIN
    IF :NEW.user_id IS NULL THEN
        :NEW.user_id := users_seq.NEXTVAL;
END IF;
END;
/

-- Books sequence and trigger  
CREATE SEQUENCE books_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER books_auto_id
    BEFORE INSERT ON books
    FOR EACH ROW
BEGIN
    IF :NEW.book_id IS NULL THEN
        :NEW.book_id := books_seq.NEXTVAL;
END IF;
END;
/

-- Chapters sequence and trigger
CREATE SEQUENCE chapters_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER chapters_auto_id
    BEFORE INSERT ON chapters
    FOR EACH ROW
BEGIN
    IF :NEW.chapter_id IS NULL THEN
        :NEW.chapter_id := chapters_seq.NEXTVAL;
END IF;
END;
/

-- Locations sequence and trigger
CREATE SEQUENCE locations_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER locations_auto_id
    BEFORE INSERT ON locations
    FOR EACH ROW
BEGIN
    IF :NEW.location_id IS NULL THEN
        :NEW.location_id := locations_seq.NEXTVAL;
END IF;
END;
/

-- Multimedia sequence and trigger
CREATE SEQUENCE multimedia_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER multimedia_auto_id
    BEFORE INSERT ON multimedia
    FOR EACH ROW
BEGIN
    IF :NEW.multimedia_id IS NULL THEN
        :NEW.multimedia_id := multimedia_seq.NEXTVAL;
END IF;
END;
/

-- Multimedia  trigger for storing image
CREATE OR REPLACE TRIGGER multimedia_generateFeatures
  BEFORE INSERT OR UPDATE OF image ON multimedia
    FOR EACH ROW
DECLARE
si ORDSYS.SI_StillImage;
BEGIN
  IF :NEW.image IS NOT NULL AND :NEW.image.height IS NOT NULL THEN
    si := ORDSYS.SI_StillImage(:NEW.image.getContent());
    :NEW.image_si := si;
    :NEW.image_ac := ORDSYS.SI_AverageColor(si);
    :NEW.image_ch := ORDSYS.SI_ColorHistogram(si);
    :NEW.image_pc := ORDSYS.SI_PositionalColor(si);
    :NEW.image_tx := ORDSYS.SI_Texture(si);
END IF;
END multimedia_generate Features;
/

-- Spatial entities sequence and triggers
CREATE SEQUENCE spatial_entities_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER spatial_entities_auto_id
    BEFORE INSERT ON spatial_entities
    FOR EACH ROW
BEGIN
    IF :NEW.entity_id IS NULL THEN
        :NEW.entity_id := spatial_entities_seq.NEXTVAL;
END IF;
    :NEW.created_date := CURRENT_TIMESTAMP;
    :NEW.updated_date := CURRENT_TIMESTAMP;
END;
/

CREATE OR REPLACE TRIGGER spatial_entities_update_time
    BEFORE UPDATE ON spatial_entities
                      FOR EACH ROW
BEGIN
    :NEW.updated_date := CURRENT_TIMESTAMP;
END;
/


PROMPT Creating performance indexes...

-- Foreign key indexes for better join performance
CREATE INDEX idx_chapters_book_id ON chapters(book_id);
CREATE INDEX idx_locations_chapter_id ON locations(chapter_id);
CREATE INDEX idx_multimedia_location_id ON multimedia(location_id);
CREATE INDEX idx_spatial_entities_chapter_id ON spatial_entities(chapter_id);

-- Text search indexes
CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_books_author ON books(author);
CREATE INDEX idx_locations_place_name ON locations(place_name);
CREATE INDEX idx_users_username ON users(username);

-- Status and type filters
CREATE INDEX idx_multimedia_type_active ON multimedia(file_type, is_active);
CREATE INDEX idx_spatial_entities_type ON spatial_entities(entity_type);


PROMPT Inserting sample data for testing...

-- Create a test user
INSERT INTO users (username, password, email) VALUES 
    ('user', '$2a$10$zBDr2T67ZLXq.IjB76CGNOfOakQeIKYUJTqx5H6ymONOFwTSQkdlu', 'demo@geobook.com');

-- Create a sample book
INSERT INTO books (title, author, isbn, description) VALUES
    ('Around the World in 80 Days', 'Jules Verne', '978-0-14-044-082-5',
     'The classic adventure following Phileas Fogg''s remarkable journey around the globe.');

-- Create sample chapters
INSERT INTO chapters (book_id, chapter_number, title, location_description) VALUES
    (1, 1, 'In which Phileas Fogg and Passepartout accept each other',
     'The story begins at the Reform Club in London, England.');

INSERT INTO chapters (book_id, chapter_number, title, location_description) VALUES
    (1, 2, 'In which Passepartout is convinced he has found his ideal',
     'Fogg''s daily routine in London is described in detail.');

-- Create sample locations
INSERT INTO locations (chapter_id, latitude, longitude, place_name) VALUES
    (1, 51.5074, -0.1278, 'Reform Club, London');

INSERT INTO locations (chapter_id, latitude, longitude, place_name) VALUES
    (2, 51.5074, -0.1278, 'Savile Row, London');

-- Create sample spatial entities
INSERT INTO spatial_entities (name, entity_type, geometry, description, color, chapter_id) VALUES
    ('London Starting Point', 'POINT',
     'POINT(-0.1278 51.5074)',
     'The Reform Club where Phileas Fogg made his famous wager', '#ff4444', 1);

INSERT INTO spatial_entities (name, entity_type, geometry, description, color, chapter_id) VALUES
    ('Central London Area', 'CIRCLE',
     'CIRCLE(-0.1278 51.5074 0.01)',
     'The general area around the Reform Club', '#44ff44', 1);

-- Commit all sample data
COMMIT;


PROMPT Verifying database installation...

-- Display created tables
SELECT 'Tables Created:' AS status FROM dual;
SELECT table_name, num_rows
FROM user_tables
WHERE table_name IN ('USERS', 'BOOKS', 'CHAPTERS', 'LOCATIONS', 'MULTIMEDIA', 'SPATIAL_ENTITIES')
ORDER BY table_name;

-- Display created sequences  
SELECT 'Sequences Created:' AS status FROM dual;
SELECT sequence_name, last_number
FROM user_sequences
WHERE sequence_name LIKE '%_SEQ'
ORDER BY sequence_name;

-- Display sample data counts
SELECT 'Sample Data Summary:' AS status FROM dual;
SELECT 'Users' as table_name, COUNT(*) as records FROM users
UNION ALL
SELECT 'Books', COUNT(*) FROM books
UNION ALL
SELECT 'Chapters', COUNT(*) FROM chapters
UNION ALL
SELECT 'Locations', COUNT(*) FROM locations
UNION ALL
SELECT 'Spatial Entities', COUNT(*) FROM spatial_entities
ORDER BY table_name;

PROMPT GeoBook Database Schema Installation Complete!