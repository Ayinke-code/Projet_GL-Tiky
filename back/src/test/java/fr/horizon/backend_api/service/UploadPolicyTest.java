package fr.horizon.backend_api.service;

import fr.horizon.backend_api.exception.FileTooLargeException;
import fr.horizon.backend_api.exception.UnsupportedFileFormatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests of the upload format and weight limits (RGESN 4.11).
 */
class UploadPolicyTest {

    private static final String PDF = "application/pdf";
    private static final long MIB = 1024L * 1024;

    /**
     * Builds a small in-memory multipart file.
     *
     * @param name        original file name
     * @param contentType MIME type declared by the client
     * @return a non-empty file of a few bytes
     */
    private static MultipartFile file(String name, String contentType) {
        return new MockMultipartFile("files", name, contentType, "content".getBytes());
    }

    /**
     * Builds a mocked multipart file of an arbitrary size without allocating that much memory.
     *
     * @param name        original file name
     * @param contentType MIME type declared by the client
     * @param size        size reported by the file, in bytes
     * @return a mock answering only what the policy reads
     */
    private static MultipartFile sizedFile(String name, String contentType, long size) {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn(name);
        when(file.getContentType()).thenReturn(contentType);
        when(file.getSize()).thenReturn(size);
        return file;
    }

    // =============================================
    // validateDocuments()
    // =============================================

    @ParameterizedTest
    @DisplayName("validateDocuments : accepte chaque format autorisé")
    @CsvSource({
            "rapport.pdf, application/pdf",
            "capture.png, image/png",
            "photo.jpg, image/jpeg",
            "photo.jpeg, image/jpeg",
            "PHOTO.JPG, image/jpeg",
            "note.docx, application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "suivi.xlsx, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    })
    void validateDocuments_should_accept_allowed_formats(String name, String contentType) {
        assertThatCode(() -> UploadPolicy.validateDocuments(List.of(file(name, contentType))))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @DisplayName("validateDocuments : refuse les formats non autorisés ou incohérents")
    @CsvSource({
            "script.exe, application/x-msdownload",
            "archive.zip, application/zip",
            "page.html, text/html",
            // Extension allowed but declared type does not match: renamed file
            "virus.pdf, application/x-msdownload",
            // Declared type allowed but extension is not
            "virus.exe, application/pdf",
            "sans_extension, application/pdf"
    })
    void validateDocuments_should_reject_unsupported_formats(String name, String contentType) {
        List<MultipartFile> files = List.of(file(name, contentType));

        assertThatThrownBy(() -> UploadPolicy.validateDocuments(files))
                .isInstanceOf(UnsupportedFileFormatException.class)
                .hasMessageContaining(name)
                .hasMessageContaining("PDF, PNG, JPG, DOCX, XLSX");
    }

    @Test
    @DisplayName("validateDocuments : refuse un fichier sans type déclaré")
    void validateDocuments_should_reject_missing_content_type() {
        List<MultipartFile> files = List.of(file("rapport.pdf", null));

        assertThatThrownBy(() -> UploadPolicy.validateDocuments(files))
                .isInstanceOf(UnsupportedFileFormatException.class);
    }

    @Test
    @DisplayName("validateDocuments : accepte un fichier pile à la limite de 10 Mo")
    void validateDocuments_should_accept_file_at_exact_limit() {
        List<MultipartFile> files = List.of(sizedFile("a.pdf", PDF, UploadPolicy.MAX_FILE_BYTES));

        assertThatCode(() -> UploadPolicy.validateDocuments(files)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateDocuments : refuse un fichier de plus de 10 Mo")
    void validateDocuments_should_reject_file_over_limit() {
        List<MultipartFile> files = List.of(sizedFile("gros.pdf", PDF, UploadPolicy.MAX_FILE_BYTES + 1));

        assertThatThrownBy(() -> UploadPolicy.validateDocuments(files))
                .isInstanceOf(FileTooLargeException.class)
                .hasMessageContaining("gros.pdf")
                .hasMessageContaining("10 Mo");
    }

    @Test
    @DisplayName("validateDocuments : accepte un lot pile à la limite de 20 Mo")
    void validateDocuments_should_accept_batch_at_exact_total_limit() {
        List<MultipartFile> files = List.of(
                sizedFile("a.pdf", PDF, 10 * MIB),
                sizedFile("b.pdf", PDF, 10 * MIB));

        assertThatCode(() -> UploadPolicy.validateDocuments(files)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateDocuments : refuse un lot de plus de 20 Mo au total")
    void validateDocuments_should_reject_batch_over_total_limit() {
        List<MultipartFile> files = List.of(
                sizedFile("a.pdf", PDF, 8 * MIB),
                sizedFile("b.pdf", PDF, 8 * MIB),
                sizedFile("c.pdf", PDF, 8 * MIB));

        assertThatThrownBy(() -> UploadPolicy.validateDocuments(files))
                .isInstanceOf(FileTooLargeException.class)
                .hasMessageContaining("20 Mo");
    }

    @Test
    @DisplayName("validateDocuments : ignore les listes nulles, vides et les parties vides")
    void validateDocuments_should_ignore_null_and_empty_parts() {
        MultipartFile emptyPart = new MockMultipartFile("files", "", null, new byte[0]);

        assertThatCode(() -> UploadPolicy.validateDocuments(null)).doesNotThrowAnyException();
        assertThatCode(() -> UploadPolicy.validateDocuments(List.of())).doesNotThrowAnyException();
        assertThatCode(() -> UploadPolicy.validateDocuments(Arrays.asList(emptyPart, null)))
                .doesNotThrowAnyException();
    }

    // =============================================
    // validateStudentImport()
    // =============================================

    @ParameterizedTest
    @DisplayName("validateStudentImport : accepte CSV et XLSX quel que soit le type déclaré")
    @CsvSource({
            "eleves.csv, text/csv",
            // Windows browsers with Excel installed declare this type for .csv files
            "eleves.csv, application/vnd.ms-excel",
            "ELEVES.CSV, text/plain",
            "eleves.xlsx, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    })
    void validateStudentImport_should_accept_csv_and_xlsx(String name, String contentType) {
        assertThatCode(() -> UploadPolicy.validateStudentImport(file(name, contentType)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @DisplayName("validateStudentImport : refuse les autres formats")
    @CsvSource({
            "eleves.xls, application/vnd.ms-excel",
            "eleves.pdf, application/pdf",
            "eleves.txt, text/plain",
            "eleves, text/csv"
    })
    void validateStudentImport_should_reject_other_formats(String name, String contentType) {
        MultipartFile file = file(name, contentType);

        assertThatThrownBy(() -> UploadPolicy.validateStudentImport(file))
                .isInstanceOf(UnsupportedFileFormatException.class)
                .hasMessageContaining("CSV, XLSX");
    }

    @Test
    @DisplayName("validateStudentImport : refuse un fichier absent")
    void validateStudentImport_should_reject_missing_file() {
        assertThatThrownBy(() -> UploadPolicy.validateStudentImport(null))
                .isInstanceOf(UnsupportedFileFormatException.class);
    }

    @Test
    @DisplayName("validateStudentImport : refuse un fichier de plus de 2 Mo")
    void validateStudentImport_should_reject_file_over_limit() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("eleves.csv");
        when(file.getSize()).thenReturn(UploadPolicy.MAX_IMPORT_BYTES + 1);

        assertThatThrownBy(() -> UploadPolicy.validateStudentImport(file))
                .isInstanceOf(FileTooLargeException.class)
                .hasMessageContaining("2 Mo");
    }
}
