-- Sample data for GeoBook app - Oracle version

-- Function to parse SDO_GEOMETRY string to SDO_GEOMETRY type
CREATE OR REPLACE FUNCTION parse_sdo_geom(geom_str IN VARCHAR2) RETURN SDO_GEOMETRY IS
  result SDO_GEOMETRY;
BEGIN
  IF UPPER(SUBSTR(geom_str, 1, 6)) = 'CIRCLE' THEN
    -- Parse CIRCLE(lon lat radius)
    DECLARE
      params VARCHAR2(4000);
      pos_space1 NUMBER;
      lon_str VARCHAR2(100);
      rest VARCHAR2(4000);
      pos_space2 NUMBER;
      lat_str VARCHAR2(100);
      rad_str VARCHAR2(100);
      lon NUMBER;
      lat NUMBER;
      rad NUMBER;
      wkt VARCHAR2(4000);
      num_points NUMBER;
      angle_step NUMBER;
      i NUMBER;
      x NUMBER;
      y NUMBER;
    BEGIN
      params := SUBSTR(geom_str, 8, LENGTH(geom_str) - 8);
      params := SUBSTR(params, 1, LENGTH(params) - 1);
      pos_space1 := INSTR(params, ' ');
      lon_str := SUBSTR(params, 1, pos_space1 - 1);
      rest := SUBSTR(params, pos_space1 + 1);
      pos_space2 := INSTR(rest, ' ');
      lat_str := SUBSTR(rest, 1, pos_space2 - 1);
      rad_str := SUBSTR(rest, pos_space2 + 1);
      lon := TO_NUMBER(lon_str);
      lat := TO_NUMBER(lat_str);
      rad := TO_NUMBER(rad_str);
      wkt := 'POLYGON((';
      num_points := 32;
      angle_step := 2 * 3.141592653589793 / num_points;
      FOR i IN 0 .. num_points - 1 LOOP
        x := lon + rad * COS(i * angle_step);
        y := lat + rad * SIN(i * angle_step);
        wkt := wkt || TO_CHAR(x) || ' ' || TO_CHAR(y) || ',';
      END LOOP;
      -- Close the polygon
      x := lon + rad * COS(0);
      y := lat + rad * SIN(0);
      wkt := wkt || TO_CHAR(x) || ' ' || TO_CHAR(y) || '))';
      result := SDO_UTIL.FROM_WKTGEOMETRY(wkt);
    END;
  ELSIF UPPER(SUBSTR(geom_str, 1, 12)) = 'SDO_GEOMETRY' THEN
    EXECUTE IMMEDIATE 'SELECT ' || geom_str || ' FROM dual' INTO result;
  ELSE
    result := SDO_UTIL.FROM_WKTGEOMETRY(geom_str);
  END IF;
  -- Ensure SRID is set to 4326 for consistency
  result.SDO_SRID := 4326;
  RETURN result;
END;
/

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

-- Fix spatial metadata and index
MERGE INTO USER_SDO_GEOM_METADATA m
USING (SELECT 'LOCATIONS' table_name, 'SPATIAL_DATA' column_name FROM DUAL) d
ON (m.table_name = d.table_name AND m.column_name = d.column_name)
WHEN NOT MATCHED THEN
INSERT (TABLE_NAME, COLUMN_NAME, DIMINFO, SRID)
VALUES ('LOCATIONS', 'SPATIAL_DATA',
SDO_DIM_ARRAY(SDO_DIM_ELEMENT('X', -180, 180, 0.005), SDO_DIM_ELEMENT('Y', -90, 90, 0.005)),
4326);

-- Update existing locations to populate spatial_data
UPDATE locations SET spatial_data = SDO_GEOMETRY(2001, 4326, SDO_POINT_TYPE(longitude, latitude, NULL), NULL, NULL) WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND spatial_data IS NULL;

-- Drop and recreate spatial index if unusable
DECLARE
  idx_exists NUMBER;
  idx_status VARCHAR2(10);
BEGIN
  SELECT COUNT(*) INTO idx_exists FROM user_indexes WHERE index_name = 'IDX_LOCATIONS_SPATIAL';
  IF idx_exists > 0 THEN
    SELECT status INTO idx_status FROM user_indexes WHERE index_name = 'IDX_LOCATIONS_SPATIAL';
    IF idx_status IN ('UNUSABLE', 'FAILED', 'LOADING') THEN
      EXECUTE IMMEDIATE 'DROP INDEX idx_locations_spatial';
      EXECUTE IMMEDIATE 'CREATE INDEX idx_locations_spatial ON locations(spatial_data) INDEXTYPE IS MDSYS.SPATIAL_INDEX';
    END IF;
  ELSE
    EXECUTE IMMEDIATE 'CREATE INDEX idx_locations_spatial ON locations(spatial_data) INDEXTYPE IS MDSYS.SPATIAL_INDEX';
  END IF;
END;
/

COMMIT;
