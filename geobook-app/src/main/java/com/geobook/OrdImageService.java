package com.geobook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrdImageService {

    private static final Logger logger = LoggerFactory.getLogger(OrdImageService.class);

    @Autowired
    private DataSource dataSource;

    /**
     * Save uploaded multipart file into the ORDSYS.ORDImage column for the given multimedia_id.
     * Uses reflection to call Oracle OrdImage APIs at runtime so compilation does not require Oracle jars.
     */
    public void saveMultipartToOrdImage(Long multimediaId, MultipartFile file) throws Exception {
        try (InputStream in = file.getInputStream()) {
            saveStreamToOrdImage(multimediaId, in, file.getContentType());
        }
    }

    /**
     * Update ORDSYS.ORDImage from a File InputStream (used after rotating the on-disk file).
     * When Oracle Multimedia JARs are missing, falls back to image_blob BLOB column.
     */
    public void saveStreamToOrdImage(Long multimediaId, InputStream in, String mimeType) throws Exception {
        java.sql.Connection conn = DataSourceUtils.getConnection(dataSource);
        boolean ok = false;
        try {
            conn.setAutoCommit(false);

            // FIRST: Try ORDImage flow (preferred when JARs are available)
            try {
                // Ensure there is an ORDImage locator by using ORDSYS.ORDIMAGE() in UPDATE
                try (PreparedStatement upd = conn.prepareStatement("UPDATE multimedia SET image = ORDSYS.ORDIMAGE() WHERE multimedia_id = ?")) {
                    upd.setLong(1, multimediaId);
                    upd.executeUpdate();
                }

                try (PreparedStatement sel = conn.prepareStatement("SELECT image FROM multimedia WHERE multimedia_id = ? FOR UPDATE")) {
                    sel.setLong(1, multimediaId);
                    try (ResultSet rs = sel.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("multimedia row not found: " + multimediaId);
                        }
                        Object imageObj = rs.getObject(1);

                        // Dynamically load oracle.ord.im.OrdImage and call methods via reflection
                        logger.debug("Attempting to load oracle.ord.im.OrdImage class");
                        Class<?> ordImageClass = Class.forName("oracle.ord.im.OrdImage");
                        logger.debug("Successfully loaded OrdImage class: {}", ordImageClass.getName());

                        // Create OrdImage from the STRUCT using ORADataFactory.create
                        Class<?> datumClass = Class.forName("oracle.sql.Datum");
                        java.lang.reflect.Method createMethod = ordImageClass.getMethod("create", datumClass, int.class);
                        Object ordImage = createMethod.invoke(null, imageObj, 0);
                        logger.debug("Successfully created OrdImage instance from STRUCT");

                        // Load data from stream
                        java.lang.reflect.Method loadData = ordImageClass.getMethod("loadDataFromInputStream", java.io.InputStream.class);
                        loadData.invoke(ordImage, in);

                        // IMPORTANT: call setProperties() to process the image
                        java.lang.reflect.Method setProperties = ordImageClass.getMethod("setProperties");
                        setProperties.invoke(ordImage);

                        // UPDATE the ORDImage column using setORAData
                        try (PreparedStatement upd = conn.prepareStatement("UPDATE multimedia SET image = ? WHERE multimedia_id = ?")) {
                            Class<?> oraclePreparedStatementClass = Class.forName("oracle.jdbc.OraclePreparedStatement");
                            java.lang.reflect.Method setORAData = oraclePreparedStatementClass.getMethod("setORAData", int.class, Class.forName("oracle.sql.ORAData"));
                            setORAData.invoke(upd, 1, ordImage);
                            upd.setLong(2, multimediaId);
                            upd.executeUpdate();
                        }
                    }
                }

                // Now compute SI_* descriptors (2-step SQL like the lab example)
                computeStillImageDescriptors(conn, multimediaId);

                conn.commit();
                ok = true;
                logger.info("Successfully saved ORDImage for multimedia id={}", multimediaId);
                return; // SUCCESS - ORDImage worked

            } catch (ClassNotFoundException cnfe) {
                // Oracle ORDImage classes not available on classpath - fall back to image_blob
                logger.warn("Oracle Multimedia JARs not found (ClassNotFoundException: {}), falling back to image_blob for multimedia id={}", cnfe.getMessage(), multimediaId);
                try { conn.rollback(); } catch (Exception ignore) {} // rollback any partial ORDImage work
            } catch (Exception ordImageEx) {
                // ORDImage flow failed for other reasons - fall back to image_blob
                logger.warn("ORDImage flow failed for multimedia id={}, falling back to image_blob: {}", multimediaId, ordImageEx.getMessage(), ordImageEx);
                try { conn.rollback(); } catch (Exception ignore) {}
            }

            // FALLBACK: Try image_blob BLOB column
            try {
                try (java.sql.PreparedStatement upd = conn.prepareStatement("UPDATE multimedia SET image_blob = EMPTY_BLOB() WHERE multimedia_id = ?")) {
                    upd.setLong(1, multimediaId);
                    upd.executeUpdate();
                }

                try (java.sql.PreparedStatement sel = conn.prepareStatement("SELECT image_blob FROM multimedia WHERE multimedia_id = ? FOR UPDATE")) {
                    sel.setLong(1, multimediaId);
                    try (java.sql.ResultSet rs = sel.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("multimedia row not found: " + multimediaId);
                        }
                        java.sql.Blob blob = rs.getBlob(1);
                        if (blob != null) {
                            try (java.io.OutputStream out = blob.setBinaryStream(1)) {
                                in.transferTo(out);
                            }
                            try { rs.updateBlob(1, blob); rs.updateRow(); } catch (Throwable ignore) {}
                        } else {
                            throw new IllegalStateException("image_blob is null after EMPTY_BLOB() for multimedia id: " + multimediaId);
                        }
                    }
                }
                conn.commit();
                ok = true;
                logger.info("Successfully saved to image_blob fallback for multimedia id={}", multimediaId);
                return; // SUCCESS - image_blob worked

            } catch (Exception blobEx) {
                logger.error("Both ORDImage and image_blob fallback failed for multimedia id={}: {}", multimediaId, blobEx.getMessage());
                try { conn.rollback(); } catch (Exception ignore) {}
                throw new IllegalStateException("Failed to save image - both ORDImage and image_blob fallback failed", blobEx);
            }
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    /**
     * Migrate existing plain BLOB stored in `image_blob` into the ORDSYS.ORDImage column.
     * This reads image_blob FOR UPDATE, initializes ORDSYS locator and uses reflection to call OrdImage APIs.
     * Assumes Oracle Multimedia JARs are available (no fallback to image_blob).
     */
    public void migrateBlobToOrdImage(Long multimediaId, String mimeType) throws Exception {
        java.sql.Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            conn.setAutoCommit(false);

            // Read existing image_blob FOR UPDATE
            try (java.sql.PreparedStatement sel = conn.prepareStatement("SELECT image_blob FROM multimedia WHERE multimedia_id = ? FOR UPDATE")) {
                sel.setLong(1, multimediaId);
                try (java.sql.ResultSet rs = sel.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException("multimedia row not found: " + multimediaId);
                    }
                    java.sql.Blob blob = rs.getBlob(1);
                    if (blob == null) {
                        throw new IllegalStateException("image_blob is null for multimedia id: " + multimediaId);
                    }

                    // Initialize ORDImage locator in the row
                    try (java.sql.PreparedStatement init = conn.prepareStatement("UPDATE multimedia SET image = ORDSYS.ORDIMAGE() WHERE multimedia_id = ?")) {
                        init.setLong(1, multimediaId);
                        init.executeUpdate();
                    }

                    // Select the ORDSYS image locator FOR UPDATE
                    try (java.sql.PreparedStatement sel2 = conn.prepareStatement("SELECT image FROM multimedia WHERE multimedia_id = ? FOR UPDATE")) {
                        sel2.setLong(1, multimediaId);
                        try (java.sql.ResultSet rs2 = sel2.executeQuery()) {
                            if (!rs2.next()) {
                                throw new IllegalStateException("multimedia row not found after init: " + multimediaId);
                            }
                            Object imageObj = rs2.getObject(1);

                            // Use reflection to create OrdImage and write data
                            Class<?> ordImageClass = Class.forName("oracle.ord.im.OrdImage");
                            // Create OrdImage from the STRUCT using ORADataFactory.create
                            Class<?> datumClass = Class.forName("oracle.sql.Datum");
                            java.lang.reflect.Method createMethod = ordImageClass.getMethod("create", datumClass, int.class);
                            Object ordImage = createMethod.invoke(null, imageObj, 0);

                            // stream data from image_blob into OrdImage
                            try (java.io.InputStream in = blob.getBinaryStream()) {
                                java.lang.reflect.Method loadData = ordImageClass.getMethod("loadDataFromInputStream", java.io.InputStream.class);
                                loadData.invoke(ordImage, in);
                            }

                            // IMPORTANT: call setProperties() to process the image
                            java.lang.reflect.Method setProperties = ordImageClass.getMethod("setProperties");
                            setProperties.invoke(ordImage);

                            // UPDATE the ORDImage column using setORAData
                            try (PreparedStatement upd = conn.prepareStatement("UPDATE multimedia SET image = ? WHERE multimedia_id = ?")) {
                                Class<?> oraclePreparedStatementClass = Class.forName("oracle.jdbc.OraclePreparedStatement");
                                java.lang.reflect.Method setORAData = oraclePreparedStatementClass.getMethod("setORAData", int.class, Class.forName("oracle.sql.ORAData"));
                                setORAData.invoke(upd, 1, ordImage);
                                upd.setLong(2, multimediaId);
                                upd.executeUpdate();
                            }
                        }
                    }
                }
            }

            // Compute SI_* descriptors (2-step SQL)
            computeStillImageDescriptors(conn, multimediaId);

            conn.commit();
            logger.info("Successfully migrated image_blob to ORDImage for multimedia id={}", multimediaId);
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalStateException("Oracle ORDImage classes not found on classpath. Add Oracle Multimedia jars to enable migration.", cnfe);
        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignore) {}
            throw e;
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    /**
     * Export ORDImage content for the multimedia row into a temporary file and return its Path.
     * Returns null if ORDImage classes are not available or the row has no image.
     */
    public java.nio.file.Path exportOrdImageToTempFile(Long multimediaId) throws Exception {
        java.sql.Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            // First try the plain BLOB column image_blob
            try (java.sql.PreparedStatement selBlob = conn.prepareStatement("SELECT image_blob FROM multimedia WHERE multimedia_id = ?")) {
                selBlob.setLong(1, multimediaId);
                try (java.sql.ResultSet rsb = selBlob.executeQuery()) {
                    if (rsb.next()) {
                        java.sql.Blob b = rsb.getBlob(1);
                        if (b != null) {
                            java.nio.file.Path tmp = java.nio.file.Files.createTempFile("imageblob-", ".bin");
                            try (java.io.InputStream in = b.getBinaryStream(); java.io.OutputStream out = java.nio.file.Files.newOutputStream(tmp)) {
                                in.transferTo(out);
                            }
                            tmp.toFile().deleteOnExit();
                            return tmp;
                        }
                    }
                }
            } catch (java.sql.SQLException ignore) {
                // image_blob not present or other error — fallthrough to ORDImage
            }

            // Fallback: use ORDImage via reflection
            try (java.sql.PreparedStatement sel = conn.prepareStatement("SELECT image FROM multimedia WHERE multimedia_id = ?")) {
                sel.setLong(1, multimediaId);
                try (java.sql.ResultSet rs = sel.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    Object blobObj = rs.getObject(1);

                    // Dynamically create oracle.ord.im.OrdImage and call getDataInFile via reflection
                    Class<?> ordImageClass = Class.forName("oracle.ord.im.OrdImage");
                    // Create OrdImage from the STRUCT using ORADataFactory.create
                    Class<?> datumClass = Class.forName("oracle.sql.Datum");
                    java.lang.reflect.Method createMethod = ordImageClass.getMethod("create", datumClass, int.class);
                    Object ordImage = createMethod.invoke(null, blobObj, 0);

                    java.nio.file.Path tmp = java.nio.file.Files.createTempFile("ordimage-", ".bin");
                    // invoke getDataInFile(String filename)
                    java.lang.reflect.Method getDataInFile = ordImageClass.getMethod("getDataInFile", String.class);
                    getDataInFile.invoke(ordImage, tmp.toString());
                    // best-effort cleanup on JVM exit
                    tmp.toFile().deleteOnExit();
                    return tmp;
                }
            }
        } catch (ClassNotFoundException cnfe) {
            // Oracle ORDImage classes not present on classpath
            return null;
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    /**
     * Compute SI_StillImage and its feature descriptors (SI_AverageColor, SI_ColorHistogram, SI_PositionalColor, SI_Texture).
     * Follows the oracle-lab-multimedia pattern: 2-step SQL execution.
     * Step 1: Create SI_StillImage from ORDImage.getContent()
     * Step 2: Compute SI_* feature descriptors from SI_StillImage
     */
    private void computeStillImageDescriptors(Connection conn, Long multimediaId) {
        try {
            // Step 1: Create SI_StillImage from ORDImage.getContent()
            try (PreparedStatement step1 = conn.prepareStatement(
                    "UPDATE multimedia p SET p.image_si = SI_StillImage(p.image.getContent()) WHERE p.multimedia_id = ?")) {
                step1.setLong(1, multimediaId);
                step1.executeUpdate();
                logger.info("Created SI_StillImage for multimedia id={}", multimediaId);
            }

            // Step 2: Compute all SI_* feature descriptors from SI_StillImage
            try (PreparedStatement step2 = conn.prepareStatement(
                    "UPDATE multimedia SET " +
                    "image_ac = SI_AverageColor(image_si), " +
                    "image_ch = SI_ColorHistogram(image_si), " +
                    "image_pc = SI_PositionalColor(image_si), " +
                    "image_tx = SI_Texture(image_si) " +
                    "WHERE multimedia_id = ?")) {
                step2.setLong(1, multimediaId);
                step2.executeUpdate();
                logger.info("Computed SI_* feature descriptors for multimedia id={}", multimediaId);
            }
        } catch (Exception e) {
            // Log but don't fail the transaction - SI_* descriptors are optional enhancements
            logger.warn("Failed to compute SI_* descriptors for multimedia id={}: {}", multimediaId, e.getMessage());
        }
    }
}
