package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.FileTooLargeException;
import fr.horizon.backend_api.exception.UnsupportedFileFormatException;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for the format and weight limits applied to uploaded files (RGESN 4.11).
 * <p>
 * The same limits are displayed and pre-checked by the frontend in
 * {@code front/app/utils/validateFiles.js}: both files must be kept in sync. The server-side
 * check exists because the browser check can be bypassed by calling the API directly.
 * <p>
 * Stateless static helper on purpose: services call it without any extra dependency to inject.
 */
public final class UploadPolicy {

    /** Maximum weight of one attached document (10 MiB, same value as spring.servlet.multipart.max-file-size). */
    public static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    /** Maximum cumulated weight of the documents sent in one request (20 MiB). */
    public static final long MAX_TOTAL_BYTES = 20L * 1024 * 1024;

    /** Maximum weight of a student import file (2 MiB): a class list is a few kilobytes of text. */
    public static final long MAX_IMPORT_BYTES = 2L * 1024 * 1024;

    private static final String DOCX_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String XLSX_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /** Allowed document extensions mapped to the MIME type the browser must declare for them. */
    private static final Map<String, String> DOCUMENT_TYPES = Map.of(
            "pdf", "application/pdf",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "docx", DOCX_TYPE,
            "xlsx", XLSX_TYPE
    );

    /** Extensions the student import can actually parse (legacy .xls is not supported by the parser). */
    private static final Set<String> IMPORT_EXTENSIONS = Set.of("csv", "xlsx");

    private static final String DOCUMENT_FORMATS_LABEL = "PDF, PNG, JPG, DOCX, XLSX";
    private static final String IMPORT_FORMATS_LABEL = "CSV, XLSX";

    private UploadPolicy() {
        // Utility class, never instantiated
    }

    /**
     * Checks the documents attached to an issue or a message before anything is written to disk.
     * Empty parts are ignored, as the storage loop skips them too.
     *
     * @param files files received in the multipart request (may be null or empty)
     * @throws UnsupportedFileFormatException if a file extension or declared MIME type is not allowed
     * @throws FileTooLargeException          if one file or the whole batch exceeds the weight limits
     */
    public static void validateDocuments(List<MultipartFile> files) {
        if (files == null) return;

        long totalBytes = 0;

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            String name = file.getOriginalFilename();
            String expectedType = DOCUMENT_TYPES.get(extensionOf(name));

            // Extension and declared type must agree, otherwise a renamed file would get through
            if (expectedType == null || !expectedType.equalsIgnoreCase(file.getContentType())) {
                throw new UnsupportedFileFormatException(name, DOCUMENT_FORMATS_LABEL);
            }
            if (file.getSize() > MAX_FILE_BYTES) {
                throw new FileTooLargeException(
                        "Fichier trop volumineux : " + name + " (" + toMebibytes(MAX_FILE_BYTES) + " Mo max. par fichier)");
            }
            totalBytes += file.getSize();
        }

        if (totalBytes > MAX_TOTAL_BYTES) {
            throw new FileTooLargeException(
                    "Taille totale des fichiers trop élevée (" + toMebibytes(MAX_TOTAL_BYTES) + " Mo max. au total)");
        }
    }

    /**
     * Checks a student import file before it is parsed.
     * Only the extension is checked: browsers declare very different MIME types for CSV files
     * depending on the operating system (text/csv, application/vnd.ms-excel, ...).
     *
     * @param file file received in the multipart request
     * @throws UnsupportedFileFormatException if the file is missing, empty or not a CSV/XLSX file
     * @throws FileTooLargeException          if the file exceeds {@link #MAX_IMPORT_BYTES}
     */
    public static void validateStudentImport(MultipartFile file) {
        String name = file == null ? null : file.getOriginalFilename();

        if (file == null || !IMPORT_EXTENSIONS.contains(extensionOf(name))) {
            throw new UnsupportedFileFormatException(name, IMPORT_FORMATS_LABEL);
        }
        if (file.getSize() > MAX_IMPORT_BYTES) {
            throw new FileTooLargeException(
                    "Fichier trop volumineux : " + name + " (" + toMebibytes(MAX_IMPORT_BYTES) + " Mo max.)");
        }
    }

    /**
     * Returns the lower-cased extension of a file name.
     *
     * @param filename original file name, possibly null
     * @return the extension without the dot, or an empty string when there is none
     */
    private static String extensionOf(String filename) {
        String extension = StringUtils.getFilenameExtension(filename);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    /**
     * Converts a byte count to whole mebibytes for user-facing messages.
     *
     * @param bytes size in bytes
     * @return size in mebibytes
     */
    private static long toMebibytes(long bytes) {
        return bytes / (1024 * 1024);
    }
}
