package com.tienda.files;

import com.tienda.common.exception.ResourceNotFoundException;
import com.tienda.files.dto.UploadedFile;
import com.tienda.users.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.UUID;

/** Guarda archivos subidos en disco y registra sus metadatos. Las imágenes de producto se optimizan. */
@Service
public class FileStorageService {

    private static final int MAX_SIDE = 1600;
    private static final int RECEIPT_MAX_SIDE = 2200;
    private static final float RECEIPT_QUALITY = 0.9f;
    private static final long MAX_PIXELS = 50_000_000L;
    private static final float JPEG_QUALITY = 0.85f;

    private final StoredFileRepository repository;
    private final Path root;
    private final String publicBaseUrl;

    public FileStorageService(StoredFileRepository repository,
                              @Value("${app.storage.root:uploads}") String root,
                              @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.repository = repository;
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public UploadedFile storeProductImage(MultipartFile file, User uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío.");
        }
        byte[] original;
        try {
            original = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el archivo.");
        }

        Processed processed = process(original);
        String hash = sha256(processed.bytes());

        StoredFile existing = repository.findFirstByKindAndSha256(FileKind.PRODUCT_IMAGE, hash).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        LocalDate today = LocalDate.now();
        String relative = "product-images/%d/%02d/%s.%s".formatted(
                today.getYear(), today.getMonthValue(), UUID.randomUUID(), processed.extension());
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, processed.bytes());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo.", e);
        }

        StoredFile saved = repository.save(StoredFile.builder()
                .kind(FileKind.PRODUCT_IMAGE)
                .originalName(cleanName(file.getOriginalFilename()))
                .contentType(processed.contentType())
                .sizeBytes(processed.bytes().length)
                .sha256(hash)
                .storagePath(relative)
                .uploadedBy(uploadedBy)
                .build());
        return toResponse(saved);
    }

    /** Guarda el comprobante de una transferencia (imagen o PDF). No es público. */
    @Transactional
    public StoredFile storeReceipt(MultipartFile file, User uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Sube la captura o el PDF del comprobante.");
        }
        byte[] original;
        try {
            original = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el archivo.");
        }

        Processed processed = isPdf(original)
                ? new Processed(original, "application/pdf", "pdf")
                : process(original, RECEIPT_MAX_SIDE, RECEIPT_QUALITY);
        String hash = sha256(processed.bytes());

        StoredFile existing = repository.findFirstByKindAndSha256(FileKind.PAYMENT_RECEIPT, hash).orElse(null);
        if (existing != null) {
            return existing;
        }

        LocalDate today = LocalDate.now();
        String relative = "receipts/%d/%02d/%s.%s".formatted(
                today.getYear(), today.getMonthValue(), UUID.randomUUID(), processed.extension());
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, processed.bytes());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo.", e);
        }

        return repository.save(StoredFile.builder()
                .kind(FileKind.PAYMENT_RECEIPT)
                .originalName(cleanName(file.getOriginalFilename()))
                .contentType(processed.contentType())
                .sizeBytes(processed.bytes().length)
                .sha256(hash)
                .storagePath(relative)
                .uploadedBy(uploadedBy)
                .build());
    }

    @Transactional(readOnly = true)
    public StoredFile getPublicImage(UUID id) {
        return repository.findById(id)
                .filter(file -> file.getKind() == FileKind.PRODUCT_IMAGE)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo no encontrado: " + id));
    }

    public Path pathOf(StoredFile file) {
        return resolve(file.getStoragePath());
    }

    private UploadedFile toResponse(StoredFile file) {
        return new UploadedFile(file.getId(), publicBaseUrl + "/api/files/" + file.getId(),
                file.getOriginalName(), file.getSizeBytes());
    }

    private Path resolve(String relative) {
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Ruta de archivo inválida.");
        }
        return path;
    }

    private record Processed(byte[] bytes, String contentType, String extension) {
    }

    private Processed process(byte[] bytes) {
        return process(bytes, MAX_SIDE, JPEG_QUALITY);
    }

    private Processed process(byte[] bytes, int maxSide, float quality) {
        if (isWebp(bytes)) {
            return new Processed(bytes, "image/webp", "webp");
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("El archivo no es una imagen válida. Usa JPG, PNG o WebP.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > MAX_PIXELS) {
                    throw new IllegalArgumentException("La imagen es demasiado grande.");
                }
                return toJpeg(reader.read(0), maxSide, quality);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo procesar la imagen.");
        }
    }

    private Processed toJpeg(BufferedImage source, int maxSide, float quality) throws IOException {
        double scale = Math.min(1.0, (double) maxSide / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));

        BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream imageOut = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(imageOut);
            writer.write(null, new IIOImage(rgb, null, null), param);
        } finally {
            writer.dispose();
        }
        return new Processed(out.toByteArray(), "image/jpeg", "jpg");
    }

    private static boolean isPdf(byte[] bytes) {
        return bytes.length > 5 && new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-");
    }

    private static boolean isWebp(byte[] bytes) {
        return bytes.length > 12
                && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String cleanName(String name) {
        if (name == null) {
            return null;
        }
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        return base.length() > 255 ? base.substring(0, 255) : base;
    }
}
