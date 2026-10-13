package product_service.service;

import product_service.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ImageStorageService {

    public static final String URL_PREFIX = "/api/products/images/";

    private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);
    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final Pattern SAFE_NAME = Pattern.compile("^[a-f0-9\\-]{36}\\.(jpg|png|webp)$");

    private final Path root;

    public ImageStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create upload directory: " + root, e);
        }
        log.info("Image upload directory: {}", root);
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Image file is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Image must be at most 2 MB");
        }

        String extension = detectExtension(file);
        String filename = UUID.randomUUID() + "." + extension;
        Path target = root.resolve(filename).normalize();

        if (!target.startsWith(root)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target);
        } catch (IOException e) {
            log.error("Could not save image", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save image");
        }
        return filename;
    }

    public Resource load(String filename) {
        if (filename == null || !SAFE_NAME.matcher(filename).matches()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Image not found");
        }
        Path path = root.resolve(filename).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Image not found");
        }
        return new FileSystemResource(path);
    }

    public String contentType(String filename) {
        if (filename.endsWith(".png")) {
            return "image/png";
        }
        if (filename.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    public void deleteByUrl(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(URL_PREFIX)) {
            return;
        }
        String filename = imageUrl.substring(URL_PREFIX.length());
        if (!SAFE_NAME.matcher(filename).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(root.resolve(filename).normalize());
        } catch (IOException e) {
            log.warn("Could not delete old image {}", filename);
        }
    }

    /** फाइलच्या सुरुवातीच्या बाइट्सवरून खरा प्रकार ओळखतो. नावावर किंवा Content-Type वर विश्वास ठेवत नाही. */
    private String detectExtension(MultipartFile file) {
        byte[] head;
        try (InputStream in = file.getInputStream()) {
            head = in.readNBytes(12);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read image");
        }

        if (head.length >= 3
                && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (head.length >= 8
                && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G'
                && head[4] == 0x0D && head[5] == 0x0A && head[6] == 0x1A && head[7] == 0x0A) {
            return "png";
        }
        if (head.length >= 12
                && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return "webp";
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Only JPG, PNG or WEBP images are allowed");
    }
}