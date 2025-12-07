CREATE TABLE books (
    book_id NUMBER PRIMARY KEY,
    title VARCHAR2(255) NOT NULL,
    author VARCHAR2(255),
    isbn VARCHAR2(20) UNIQUE
);
CREATE TABLE chapters (
    chapter_id NUMBER PRIMARY KEY,
    book_id NUMBER REFERENCES books(book_id) ON DELETE CASCADE,
    chapter_number NUMBER,
    title VARCHAR2(255),
    location_description CLOB
);
CREATE TABLE locations (
    location_id NUMBER PRIMARY KEY,
    chapter_id NUMBER REFERENCES chapters(chapter_id) ON DELETE CASCADE,
    latitude NUMBER(9, 6),
    longitude NUMBER(9, 6),
    place_name VARCHAR2(255),
    spatial_data SDO_GEOMETRY
);

CREATE TABLE multimedia (
    multimedia_id NUMBER PRIMARY KEY,
    location_id NUMBER REFERENCES locations(location_id) ON DELETE CASCADE,
    file_type VARCHAR2(50),        -- Type of multimedia (e.g., image, video)
    file_path VARCHAR2(500),       -- Path or URL to access the multimedia file
    description CLOB,              -- Textual description of the multimedia content
    upload_date DATE DEFAULT SYSDATE,  -- Date when the multimedia was uploaded
    is_active CHAR(1) DEFAULT 'Y',     -- Status flag to indicate if the multimedia is active (Y/N)
    thumbnail_path VARCHAR2(500),   -- Path to a thumbnail image for quick previews
    image ORDSYS.ORDImage,
    image_si ORDSYS.SI_StillImage,
    image_ac ORDSYS.SI_AverageColor,
    image_pc ORDSYS.SI_PositionalColor,
    image_tx ORDSYS.SI_Texture,
    image_ch ORDSYS.SI_ColorHistogram,
);

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
END multimedia_generateFeatures;
/
