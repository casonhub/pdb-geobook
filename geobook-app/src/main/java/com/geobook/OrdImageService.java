package com.geobook;

import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import oracle.ord.im.OrdImage;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.sql.DataSource;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.io.File;
import java.nio.file.Files;


@Service
public class OrdImageService {

    @Autowired
    private DataSource dataSource;

    /**
     * Save MultipartFile → ORDImage
     */
    public void saveMultipartToOrdImage(Long multimediaId, MultipartFile file) throws Exception {
        try (InputStream in = file.getInputStream()) {
            saveStreamToOrdImage(multimediaId, in);
        }
    }

    /**
     * Save InputStream → ORDImage
     */
    public void saveStreamToOrdImage(Long multimediaId, InputStream in) throws Exception {

        Connection conn = DataSourceUtils.getConnection(dataSource);
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try {
            OrdImage ordImageObj = null;

            // 1️⃣ SELECT existing ORDImage locator FOR UPDATE
            try (PreparedStatement ps =
                         conn.prepareStatement("SELECT image FROM multimedia WHERE multimedia_id = ? FOR UPDATE")) {
                ps.setLong(1, multimediaId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        OracleResultSet ors = (OracleResultSet) rs;
                        ordImageObj = (OrdImage) ors.getORAData(1, OrdImage.getORADataFactory());
                    }
                }
            }

            // If row didn't exist OR image column is NULL → initialize
            if (ordImageObj == null) {
                try (PreparedStatement ps =
                             conn.prepareStatement("UPDATE multimedia SET image = ordsys.ordimage.init() WHERE multimedia_id = ?")) {
                    ps.setLong(1, multimediaId);
                    ps.executeUpdate();
                }

                // Select again (now FOR UPDATE)
                try (PreparedStatement ps =
                             conn.prepareStatement("SELECT image FROM multimedia WHERE multimedia_id = ? FOR UPDATE")) {
                    ps.setLong(1, multimediaId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            OracleResultSet ors = (OracleResultSet) rs;
                            ordImageObj = (OrdImage) ors.getORAData(1, OrdImage.getORADataFactory());
                        }
                    }
                }
            }

            // 2️⃣ Load data from input stream
            ordImageObj.loadDataFromInputStream(in);

            // 3️⃣ Auto-populate metadata (size, height, width…)
            ordImageObj.setProperties();

            // 4️⃣ UPDATE the ORDImage column
            try (PreparedStatement ps =
                         conn.prepareStatement("UPDATE multimedia SET image = ? WHERE multimedia_id = ?")) {

                OraclePreparedStatement ops = (OraclePreparedStatement) ps;
                ops.setORAData(1, ordImageObj);
                ps.setLong(2, multimediaId);

                ps.executeUpdate();
            }

            // 5️⃣ Compute SI_* (You already have a trigger; but adding SQL method too)
            computeStillImageDescriptors(conn, multimediaId);

            conn.commit();

        } catch (Exception ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }



    /**
     * Load ORDImage locator FOR UPDATE
     */
    private OrdImage loadOrdImageForUpdate(Connection conn, Long id) throws Exception {

        OrdImage ord = null;

        String sql = "SELECT image FROM multimedia WHERE multimedia_id = ? FOR UPDATE";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    OracleResultSet ors = (OracleResultSet) rs;
                    ord = (OrdImage) ors.getORAData(1, OrdImage.getORADataFactory());
                }
            }
        }

        // Initialize if null
        if (ord == null) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE multimedia SET image = ordsys.ordimage.init() WHERE multimedia_id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            // Fetch again
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        OracleResultSet ors = (OracleResultSet) rs;
                        ord = (OrdImage) ors.getORAData(1, OrdImage.getORADataFactory());
                    }
                }
            }
        }

        return ord;
    }

    /**
     * Load ORDImage and convert to byte[]
     */
    public byte[] loadOrdImageBytes(Long id) throws Exception {

        Connection conn = DataSourceUtils.getConnection(dataSource);

        try {
            OrdImage img = null;

            String sql = "SELECT image FROM multimedia WHERE multimedia_id = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        OracleResultSet ors = (OracleResultSet) rs;
                        img = (OrdImage) ors.getORAData(1, OrdImage.getORADataFactory());
                    }
                }
            }

            if (img == null)
                return null;

            // Save ORDImage to a temporary file then read as bytes
            File temp = File.createTempFile("ordimage_", ".tmp");
            temp.deleteOnExit();

            img.getDataInFile(temp.getAbsolutePath());

            return Files.readAllBytes(temp.toPath());
        }
        finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    /**
     * Compute SI descriptors
     */
    private void computeStillImageDescriptors(Connection conn, Long id) throws SQLException {

        String si = "UPDATE multimedia p " +
                "SET p.image_si = SI_StillImage(p.image.getContent()) WHERE multimedia_id = ?";

        String features = "UPDATE multimedia SET " +
                "image_ac = SI_AverageColor(image_si), " +
                "image_ch = SI_ColorHistogram(image_si), " +
                "image_pc = SI_PositionalColor(image_si), " +
                "image_tx = SI_Texture(image_si) " +
                "WHERE multimedia_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(si)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement(features)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Rotate the image by degrees (90, 180, 270)
     */
    public byte[] loadImageAsBytes(Long multimediaId) throws Exception {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            OrdImage ordImageObj = null;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT image FROM multimedia WHERE multimedia_id = ?")) {

                ps.setLong(1, multimediaId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        OracleResultSet ors = rs.unwrap(OracleResultSet.class);
                        ordImageObj = (OrdImage) ors.getORAData(1, OrdImage.getORADataFactory());
                    }
                }
            }

            if (ordImageObj == null) {
                throw new Exception("ORDImage not found for ID: " + multimediaId);
            }

            // Read bytes
            InputStream in = ordImageObj.getDataInStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return baos.toByteArray();

        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    /**
     * Rotate image 90 degrees clockwise and update in DB
     */
    public void rotateImage(Long multimediaId) throws Exception {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try {
            OrdImage ordImageObj = null;

            // Fetch ORDImage with FOR UPDATE
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT image FROM multimedia WHERE multimedia_id = ? FOR UPDATE")) {

                ps.setLong(1, multimediaId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        OracleResultSet ors = rs.unwrap(OracleResultSet.class);
                        ordImageObj = (OrdImage) ors.getORAData(1, OrdImage.getORADataFactory());
                    }
                }
            }

            if (ordImageObj == null) {
                throw new Exception("ORDImage not found for ID: " + multimediaId);
            }

            // Read original bytes
            InputStream in = ordImageObj.getDataInStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            byte[] originalBytes = baos.toByteArray();

            // Rotate bytes
            byte[] rotatedBytes = rotate90Clockwise(originalBytes);

            // Load rotated bytes back into ORDImage
            ordImageObj.loadDataFromByteArray(rotatedBytes);
            ordImageObj.setProperties();

            // Update database
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE multimedia SET image = ? WHERE multimedia_id = ?")) {

                OraclePreparedStatement ops = ps.unwrap(OraclePreparedStatement.class);
                ops.setORAData(1, ordImageObj);
                ps.setLong(2, multimediaId);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    /**
     * Rotate image bytes 90 degrees clockwise
     */
    private byte[] rotate90Clockwise(byte[] imageBytes) throws Exception {
        InputStream input = new ByteArrayInputStream(imageBytes);
        BufferedImage original = ImageIO.read(input);
        int width = original.getWidth();
        int height = original.getHeight();

        BufferedImage rotated = new BufferedImage(height, width, original.getType());
        Graphics2D g2d = rotated.createGraphics();
        g2d.translate((height - width) / 2.0, (height - width) / 2.0);
        g2d.rotate(Math.toRadians(90), height / 2.0, width / 2.0);
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(rotated, "PNG", baos);
        return baos.toByteArray();
    }

}
