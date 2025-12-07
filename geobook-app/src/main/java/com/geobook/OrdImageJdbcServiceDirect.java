package com.geobook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.OutputStream;

@Service
public class OrdImageJdbcServiceDirect {

    @Autowired
    private DataSource dataSource;

    @Autowired(required = false)
    private OrdImageService ordImageService; // the reflection-based service

    /**
     * Delegates to OrdImageService.saveMultipartToOrdImage if available.
     */
    public void saveMultipartToOrdImageDirect(Long multimediaId, MultipartFile file) throws Exception {
        if (ordImageService == null) {
            throw new IllegalStateException("Oracle OrdImage classes not available on classpath. Place Oracle multimedia jars in libs/ and restart.");
        }
        ordImageService.saveMultipartToOrdImage(multimediaId, file);
    }

    /**
     * Delegates to OrdImageService load/save methods or throws when unavailable.
     */
    public void loadImageFromDbToFile(Long multimediaId, String filename) throws Exception {
        throw new UnsupportedOperationException("Use OrdImageService or implement BLOB fallback. This method is not supported in the direct service stub.");
    }

//    public void saveImageFileToDb(Long multimediaId, String filename, String mimeType) throws Exception {
//        if (ordImageService == null) {
//            throw new IllegalStateException("Oracle OrdImage classes not available on classpath. Place Oracle multimedia jars in libs/ and restart.");
//        }
//        try (InputStream in = java.nio.file.Files.newInputStream(java.nio.file.Paths.get(filename))) {
//            ordImageService.saveStreamToOrdImage(multimediaId, in, mimeType);
//        }
//    }

    public void streamImageToOutput(Long multimediaId, OutputStream out) throws Exception {
        throw new UnsupportedOperationException("Streaming from ORDSYS directly is not implemented in this stub. Use OrdImageService or add the Oracle multimedia jars and implement streaming.");
    }

    public Long insertMultimediaAndSaveImage(MultipartFile file, Long locationId, String fileType, String description, String thumbnailPath) throws Exception {
        if (ordImageService == null) {
            throw new IllegalStateException("Oracle OrdImage classes not available on classpath. Place Oracle multimedia jars in libs/ and restart.");
        }
        // Create a minimal row via application repositories if needed — delegate only to ordImageService for LOB handling.
        throw new UnsupportedOperationException("Use MultimediaController which saves metadata then calls OrdImageService. This method is intentionally not implemented in the direct stub.");
    }
}
