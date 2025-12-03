package com.geobook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import net.coobird.thumbnailator.Thumbnails;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.io.FileInputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@Controller
@RequestMapping("/multimedia")
public class MultimediaController {

    private static final Logger logger = LoggerFactory.getLogger(MultimediaController.class);

    @Autowired
    private MultimediaRepository multimediaRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private OrdImageService ordImageService; // added service to write ORDSYS columns

    @GetMapping
    public String listMultimedia(Model model) {
        List<Multimedia> multimediaList = multimediaRepository.findAll();
        model.addAttribute("multimediaList", multimediaList);
        return "multimedia";
    }

    @GetMapping("/new")
    public String newMultimediaForm(Model model) {
        model.addAttribute("multimedia", new Multimedia());
        model.addAttribute("locations", locationRepository.findAll());
        return "multimedia-form";
    }

    @PostMapping
    public String createMultimedia(@RequestParam("imageFile") MultipartFile file,
                                   @RequestParam("locationId") Long locationId,
                                   @RequestParam("description") String description,
                                   @RequestParam("fileType") String fileType) {
        if (!file.isEmpty()) {
            try {
                String basePath = System.getProperty("user.dir") + "/target/classes/static/images/";
                Path uploadPath = Paths.get(basePath);
                Files.createDirectories(uploadPath);

                String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path filePath = uploadPath.resolve(filename);
                Files.write(filePath, file.getBytes());

                Multimedia multimedia = new Multimedia();
                multimedia.setLocation(locationRepository.findById(locationId).orElseThrow());
                multimedia.setFileType(fileType);
                multimedia.setFilePath("/images/" + filename);
                multimedia.setDescription(description);
                multimedia.setUploadDate(LocalDate.now());
                multimedia.setIsActive('Y');
                multimedia.setThumbnailPath("/thumbnails/" + filename); // Placeholder

                Multimedia saved = multimediaRepository.save(multimedia);
                logger.info("Saved multimedia record: id={}, filePath={}", saved.getMultimediaId(), saved.getFilePath());
                System.out.println("Saved multimedia id=" + saved.getMultimediaId() + " path=" + saved.getFilePath());

                // ORDSYS-first write: attempt JDBC flow using ORDSYS.ORDIMAGE() to create ORDImage locator,
                // then SELECT image FOR UPDATE and write binary into returned Blob locator. This works with ojdbc
                // without requiring Oracle multimedia Java jars. If it fails, fall back to the reflection-based OrdImageService.
                boolean ordWritten = false;
                try {
                    java.sql.Connection conn = org.springframework.jdbc.datasource.DataSourceUtils.getConnection(dataSource);
                    boolean prevAuto = conn.getAutoCommit();
                    try {
                        conn.setAutoCommit(false);
                        // initialize ORDImage locator in the row
                        try (java.sql.PreparedStatement init = conn.prepareStatement("UPDATE multimedia SET image = ORDSYS.ORDIMAGE() WHERE multimedia_id = ?")) {
                            init.setLong(1, saved.getMultimediaId());
                            init.executeUpdate();
                        }

                        try (java.sql.PreparedStatement sel = conn.prepareStatement("SELECT image FROM multimedia WHERE multimedia_id = ? FOR UPDATE")) {
                            sel.setLong(1, saved.getMultimediaId());
                            try (java.sql.ResultSet rs = sel.executeQuery()) {
                                if (!rs.next()) {
                                    throw new IllegalStateException("multimedia row not found: " + saved.getMultimediaId());
                                }
                                java.sql.Blob blob = rs.getBlob(1);
                                if (blob == null) {
                                    // driver may return an object UDT; try getting as Object and handle later
                                    Object obj = rs.getObject(1);
                                    if (obj == null) throw new IllegalStateException("No image locator returned for id=" + saved.getMultimediaId());
                                    // attempt reflection-based path as backup
                                    throw new IllegalStateException("ORDImage locator is not a JDBC Blob; fallback to OrdImageService");
                                }
                                try (java.io.InputStream in = file.getInputStream(); java.io.OutputStream out = blob.setBinaryStream(1)) {
                                    in.transferTo(out);
                                }
                                try { rs.updateBlob(1, blob); rs.updateRow(); } catch (Throwable ignore) {}
                                ordWritten = true;
                                logger.info("Wrote ORDImage blob content for multimedia id={}", saved.getMultimediaId());
                                System.out.println("Wrote ORDImage blob for multimedia id=" + saved.getMultimediaId());
                            }
                        }

                        // try compute SI_* descriptors in SQL (best-effort, 2-step pattern like lab example)
                        try {
                            // Step 1: Create SI_StillImage from ORDImage.getContent()
                            try (java.sql.PreparedStatement step1 = conn.prepareStatement(
                                    "UPDATE multimedia p SET p.image_si = SI_StillImage(p.image.getContent()) WHERE p.multimedia_id = ?")) {
                                step1.setLong(1, saved.getMultimediaId());
                                step1.executeUpdate();
                            }
                            // Step 2: Compute SI_* feature descriptors from SI_StillImage
                            try (java.sql.PreparedStatement step2 = conn.prepareStatement(
                                    "UPDATE multimedia SET image_ac = SI_AverageColor(image_si), image_ch = SI_ColorHistogram(image_si), image_pc = SI_PositionalColor(image_si), image_tx = SI_Texture(image_si) WHERE multimedia_id = ?")) {
                                step2.setLong(1, saved.getMultimediaId());
                                step2.executeUpdate();
                            }
                            logger.info("Computed SI_* descriptors for multimedia id={}", saved.getMultimediaId());
                        } catch (Throwable metaEx) {
                            logger.debug("SI_* SQL update skipped/failed for id={}: {}", saved.getMultimediaId(), metaEx.getMessage());
                        }

                        conn.commit();
                    } catch (Exception e) {
                        try { conn.rollback(); } catch (Exception ignore) {}
                        throw e;
                    } finally {
                        try { conn.setAutoCommit(prevAuto); } catch (Exception ignore) {}
                        org.springframework.jdbc.datasource.DataSourceUtils.releaseConnection(conn, dataSource);
                    }
                } catch (Exception ordEx) {
                    logger.warn("JDBC ORDSYS write failed for multimedia id={}. Falling back to reflection OrdImageService: {}", saved.getMultimediaId(), ordEx.getMessage());
                    System.err.println("JDBC ORDSYS write failed: " + ordEx.getMessage());
                    try {
                        ordImageService.saveMultipartToOrdImage(saved.getMultimediaId(), file);
                        ordWritten = true;
                    } catch (Exception ordServiceEx) {
                        logger.error("OrdImageService also failed for multimedia id={}", saved.getMultimediaId(), ordServiceEx);
                        System.err.println("OrdImageService failed: " + ordServiceEx.getMessage());
                    }
                }

                // create thumbnail for UI
                try {
                    File src = filePath.toFile();
                    File thumbDir = new File(uploadPath.toFile().getParentFile(), "thumbnails");
                    if (!thumbDir.exists()) thumbDir.mkdirs();
                    File thumbFile = new File(thumbDir, filename);
                    Thumbnails.of(src).size(200, 200).toFile(thumbFile);
                    saved.setThumbnailPath("/thumbnails/" + filename);
                    multimediaRepository.save(saved);
                } catch (Exception thumbEx) {
                    logger.debug("Thumbnail creation failed: {}", thumbEx.getMessage());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "redirect:/multimedia";
    }

    @GetMapping("/{id}/edit")
    public String editMultimediaForm(@PathVariable Long id, Model model) {
        Multimedia multimedia = multimediaRepository.findById(id).orElseThrow();
        model.addAttribute("multimedia", multimedia);
        model.addAttribute("locations", locationRepository.findAll());
        return "multimedia-form";
    }

    @PostMapping("/{id}")
    public String updateMultimedia(@PathVariable Long id,
                                   @RequestParam("description") String description,
                                   @RequestParam("fileType") String fileType,
                                   @RequestParam("locationId") Long locationId) {
        Multimedia multimedia = multimediaRepository.findById(id).orElseThrow();
        multimedia.setDescription(description);
        multimedia.setFileType(fileType);
        multimedia.setLocation(locationRepository.findById(locationId).orElseThrow());
        multimediaRepository.save(multimedia);
        return "redirect:/multimedia";
    }

    @PostMapping("/{id}/delete")
    public String deleteMultimedia(@PathVariable Long id) {
        multimediaRepository.deleteById(id);
        return "redirect:/multimedia";
    }

    @PostMapping("/{id}/rotate")
    @ResponseBody
    public String rotateImage(@PathVariable Long id) {
        try {
            Multimedia media = multimediaRepository.findById(id).orElseThrow();
            String basePath = System.getProperty("user.dir") + "/target/classes/static";
            String filePath = basePath + media.getFilePath();
            File file = new File(filePath);
            if (!file.exists()) {
                // Check in src path for legacy files
                basePath = System.getProperty("user.dir") + "/src/main/resources/static";
                filePath = basePath + media.getFilePath();
                file = new File(filePath);
            }
            if (!file.exists()) {
                return "Error: File not found: " + filePath;
            }
            BufferedImage original = ImageIO.read(file);
            if (original == null) {
                return "Error: Failed to read image: " + filePath;
            }
            if (original.getWidth() <= 0 || original.getHeight() <= 0) {
                return "Error: Invalid image dimensions: " + filePath;
            }
            // Rotate using Graphics2D
            BufferedImage rotated = new BufferedImage(original.getHeight(), original.getWidth(), original.getType() != 0 ? original.getType() : BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = rotated.createGraphics();
            g2d.rotate(Math.PI / 2);
            g2d.drawImage(original, 0, -original.getWidth(), null);
            g2d.dispose();
            String format = getImageFormat(file);
            boolean success = ImageIO.write(rotated, format, file);
            if (!success) {
                return "Error: Failed to write rotated image: " + filePath;
            }
            // Update ORDImage in DB from the rotated file
            try (FileInputStream fis = new FileInputStream(file)) {
                ordImageService.saveStreamToOrdImage(media.getMultimediaId(), fis, "image/" + format);
            } catch (Exception ordEx) {
                ordEx.printStackTrace();
                // continue even if ORDImage update fails
            }
            return "Success: Image rotated";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String getImageFormat(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "jpg";
        if (name.endsWith(".png")) return "png";
        if (name.endsWith(".gif")) return "gif";
        return "jpg"; // default
    }

    @GetMapping("/search")
    public String searchForm() {
        return "multimedia-search";
    }

    @PostMapping("/search")
    public String searchMultimedia(@RequestParam("query") String query, Model model) {
        // Simple text search on description
        List<Multimedia> results = multimediaRepository.findAll().stream()
            .filter(m -> m.getDescription().toLowerCase().contains(query.toLowerCase()))
            .toList();
        model.addAttribute("results", results);
        return "multimedia-search-results";
    }

    @GetMapping("/debug")
    @ResponseBody
    public String debugMultimedia() {
        List<Multimedia> list = multimediaRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("multimedia.count=").append(list.size()).append("\n");
        String base1 = System.getProperty("user.dir") + "/target/classes/static";
        String base2 = System.getProperty("user.dir") + "/src/main/resources/static";
        for (Multimedia m : list) {
            String path = m.getFilePath();
            sb.append("id=").append(m.getMultimediaId())
              .append(", filePath=").append(path);
            File f1 = new File(base1 + path);
            File f2 = new File(base2 + path);
            sb.append(", exists_in_target=").append(f1.exists())
              .append(", exists_in_src=").append(f2.exists()).append("\n");
        }
        return sb.toString();
    }

    @GetMapping("/{id}/image")
    public void streamImage(@PathVariable Long id, HttpServletResponse response) {
        try {
            Multimedia media = multimediaRepository.findById(id).orElseThrow();
            // First try to export ORDSYS image to a temp file (requires Oracle multimedia jars at runtime)
            try {
                java.nio.file.Path tmp = ordImageService.exportOrdImageToTempFile(id);
                if (tmp != null && java.nio.file.Files.exists(tmp)) {
                    String contentType = media.getFileType();
                    if (contentType == null || contentType.isEmpty()) {
                        contentType = java.nio.file.Files.probeContentType(tmp);
                    }
                    if (contentType == null) contentType = "application/octet-stream";
                    response.setContentType(contentType);
                    java.nio.file.Files.copy(tmp, response.getOutputStream());
                    response.flushBuffer();
                    return;
                }
            } catch (Exception ordEx) {
                // ORDSYS not available or export failed - fall back to static file
                ordEx.printStackTrace();
            }

            // Fallback: serve the file stored under static images
            String base1 = System.getProperty("user.dir") + "/target/classes/static";
            java.nio.file.Path p = java.nio.file.Paths.get(base1 + media.getFilePath());
            if (!java.nio.file.Files.exists(p)) {
                String base2 = System.getProperty("user.dir") + "/src/main/resources/static";
                p = java.nio.file.Paths.get(base2 + media.getFilePath());
            }
            if (java.nio.file.Files.exists(p)) {
                String contentType = media.getFileType();
                if (contentType == null || contentType.isEmpty()) {
                    contentType = java.nio.file.Files.probeContentType(p);
                }
                if (contentType == null) contentType = "application/octet-stream";
                response.setContentType(contentType);
                java.nio.file.Files.copy(p, response.getOutputStream());
                response.flushBuffer();
                return;
            }
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            try { response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage()); } catch (Exception ignore) {}
        }
    }

    @PostMapping("/admin/multimedia/{id}/migrate")
    @ResponseBody
    public ResponseEntity<?> migrateMultimediaToOrd(@PathVariable Long id, @RequestParam(required = false) String mimeType) {
        try {
            ordImageService.migrateBlobToOrdImage(id, mimeType);
            return ResponseEntity.ok(Map.of("status", "ok", "id", id));
        } catch (Exception e) {
            logger.error("Migration failed for id={}", id, e);
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
