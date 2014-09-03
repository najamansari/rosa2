package rosa.archive.core.check;

import org.apache.commons.lang3.StringUtils;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.config.AppConfig;
import rosa.archive.model.Book;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookScene;
import rosa.archive.model.BookStructure;
import rosa.archive.model.BookText;
import rosa.archive.model.ChecksumData;
import rosa.archive.model.ChecksumInfo;
import rosa.archive.model.CropData;
import rosa.archive.model.CropInfo;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.ImageList;
import rosa.archive.model.Item;
import rosa.archive.model.NarrativeTagging;
import rosa.archive.model.StructureColumn;
import rosa.archive.model.StructurePage;
import rosa.archive.model.StructurePageSide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @see rosa.archive.core.check.Checker
 * @see rosa.archive.model.Book
 */
public class BookChecker implements Checker<Book> {

    private AppConfig config;

    BookChecker(AppConfig config) {
        this.config = config;
    }

    @Override
    public boolean checkContent(Book book, ByteStreamGroup bsg, boolean checkBits) {
        List<String> errors = new ArrayList<>();

        // Check the following items:
        //   images, croppedImages
        errors.addAll(check(book.getImages(), book));
        errors.addAll(check(book.getCroppedImages(), book));
        //   cropInfo
        errors.addAll(check(book.getCropInfo(), book));

        for (String lang : config.languages()) {
            //   bookMetadata
            errors.addAll(check(book.getBookMetadata(lang), book));
            //   bookDescription (currently not present in model)
            //   permissions
        }

        //   checksumInfo
        errors.addAll(check(book.getChecksumInfo(), book));
        //   content
        //   bookStructure
        errors.addAll(check(book.getBookStructure(), book));
        //   illustrationTagging
        errors.addAll(check(book.getIllustrationTagging(), book));
        //   manualNarrativeTagging
        errors.addAll(check(book.getManualNarrativeTagging(), book));
        //   automaticNarrativeTagging
        errors.addAll(check(book.getAutomaticNarrativeTagging(), book));

        // If checkBits is true
        //   calculate hash digest of each of the above items, compare it to the hash values
        //   stored in checksumInfo

        // Return TRUE if all above checks pass
        // Return FALSE if any of the above tests fail
        return errors.isEmpty();
    }

    /**
     * Check to see if an ID is present in an array of content.
     *
     * @param id ID
     * @param contents array of contents to check
     * @return TRUE if the ID is present
     */
    private boolean isInArchive(String id, String[] contents) {
        return Arrays.binarySearch(contents, id) >= 0;
    }

    /**
     * Check data consistency of a {@link rosa.archive.model.BookMetadata} object according
     * to the rules:
     *
     * <ul>
     *     <li>The metadata must exist as a data model object.</li>
     *     <li>The associated stored metadata file must exist in the archive and be readable.</li>
     *     <li>The metadata fields must not be NULL.</li>
     *     <li>The {@code yearStart} field must fall logically before the {@code yearEnd}.</li>
     * </ul>
     *
     * @param metadata data to check
     * @param parent parent container
     * @return list of errors found during the check
     */
    private List<String> check(BookMetadata metadata, Book parent) {
        List<String> errors = new ArrayList<>();

        if (metadata == null) {
            errors.add("Metadata is missing.");
            return errors;
        }

        if (!isInArchive(metadata.getId(), parent.getContent())) {
            errors.add("Metadata not present in parent book. [" + metadata.getId() + "]");
        }

        if (StringUtils.isBlank(metadata.getDate())) {
            errors.add("Metadata date not set.");
        }
        if (metadata.getYearStart() == -1) {
            errors.add("Metadata start year not set.");
        }
        if (metadata.getYearEnd() - metadata.getYearStart() < 0) {
            errors.add("Date range ends before it begins! " +
                    "Check the description <date notBefore=\"..\" notAfter=\"..\"> element.");
        }
        if (metadata.getYearEnd() == -1) {
            errors.add("Metadata end year not set.");
        }
        if (StringUtils.isBlank(metadata.getCurrentLocation())) {
            errors.add("Metadata current location not set.");
        }
        if (StringUtils.isBlank(metadata.getRepository())) {
            errors.add("Metadata repository not set.");
        }
        if (StringUtils.isBlank(metadata.getShelfmark())) {
            errors.add("Metadata shelfmark not set.");
        }
        if (StringUtils.isBlank(metadata.getOrigin())) {
            errors.add("Metadata origin not set.");
        }
        if (StringUtils.isBlank(metadata.getDimensions())) {
            errors.add("Metadata dimensions not set.");
        }
        if (metadata.getWidth() == -1) {
            errors.add("Metadata width not set.");
        }
        if (metadata.getHeight() == -1) {
            errors.add("Metadata height not set.");
        }
        if (metadata.getNumberOfIllustrations() == -1) {
            errors.add("Metadata number of illustrations not set.");
        }
        if (metadata.getNumberOfPages() == -1) {
            errors.add("Metadata number of pages/folios not set.");
        }
        if (StringUtils.isBlank(metadata.getType())) {
            errors.add("Metadata type not set.");
        }
        if (StringUtils.isBlank(metadata.getCommonName())) {
            errors.add("Metadata common name not set.");
        }
        if (StringUtils.isBlank(metadata.getMaterial())) {
            errors.add("Metadata material not set.");
        }
        if (metadata.getTexts() == null) {
            errors.add("Metadata texts not set.");
        }

        for (BookText text : metadata.getTexts()) {
            if (StringUtils.isBlank(text.getFirstPage())) {
                errors.add("Metadata text first page not set. [" + text + "]");
            }
            if (StringUtils.isBlank(text.getLastPage())) {
                errors.add("Metadata text last page not set. [" + text + "]");
            }
            if (StringUtils.isBlank(text.getTitle())) {
                errors.add("Metadata text title not set. [" + text + "]");
            }
            if (text.getNumberOfIllustrations() == -1) {
                errors.add("Metadata number of illustrations not set. [" + text + "]");
            }
            if (text.getNumberOfPages() == -1) {
                errors.add("Metadata number of pages not set. [" + text + "]");
            }
            if (text.getColumnsPerPage() == -1) {
                errors.add("Metadata columns per page not set. [" + text + "]");
            }
            if (text.getLeavesPerGathering() == -1) {
                errors.add("Metadata leaves per gathering not set. [" + text + "]");
            }
            if (text.getLinesPerColumn() == -1) {
                errors.add("Metadata lines per column not set. [" + text + "]");
            }
        }

        return errors;
    }

    /**
     * Check data consistency of an {@link rosa.archive.model.ImageList} according
     * to the following rules:
     *
     * <ul>
     *     <li>The image list must exist.</li>
     *     <li>Each image in the list must be initialized with non-null field values.</li>
     *     <li>Any image marked as missing must not appear in the Book's list of contents.</li>
     *     <li>Any image marked as not missing must appear in the Book's list of contents.</li>
     * </ul>
     *
     * @param images {@link rosa.archive.model.ImageList}
     * @param parent {@link rosa.archive.model.Book} that contains this image list
     * @return list of errors found while checking data
     */
    private List<String> check(ImageList images, Book parent) {
        List<String> errors = new ArrayList<>();

        if (images == null || images.getImages() == null) {
            errors.add("Image list is missing.");
            return errors;
        }

        for (BookImage image : images.getImages()) {
            // Make sure the width, height, ID fields were set
            if (StringUtils.isBlank(image.getId())) {
                errors.add("Image ID not set. [" + image + "]");
            }
            if (image.getWidth() == -1) {
                errors.add("Image width not set. [" + image + "]");
            }
            if (image.getHeight() == -1) {
                errors.add("Image height not set. [" + image + "]");
            }

            // Relies on the fact that the array is sorted.
            boolean inContent = isInArchive(image.getId(), parent.getContent());
            if (image.isMissing() && inContent) {
                errors.add("Image [" + image.getId() + "] marked as missing is present in archive.");
            } else if (!image.isMissing() && !inContent) {
                errors.add("Image [" + image.getId() + "] marked as present in archive is missing.");
            }
        }

        return errors;
    }

    /**
     * Check the following:
     *
     * <ul>
     *     <li>Crop information must be exist.</li>
     *     <li>Crop information must exist in parent book content.</li>
     *     <li>For each data row, the ID must exist in the parent book content.</li>
     *     <li>For each data row, the values left, right, top, bottom must be between 0 and 1.</li>
     * </ul>
     *
     * @param cropInfo cropping data
     * @param parent parent book
     * @return list of errors found while performing check
     */
    private List<String> check(CropInfo cropInfo, Book parent) {
        List<String> errors = new ArrayList<>();

        if (cropInfo == null) {
            errors.add("Crop information missing.");
            return errors;
        }

        if (!isInArchive(parent.getId() + config.getCROP(), parent.getContent())) {
            errors.add("Crop information missing from parent book content.");
        }

        for (CropData crop : cropInfo) {
            if (StringUtils.isBlank(crop.getId())) {
                errors.add("Crop ID missing. ]" + crop + "]");
            } else if (!isInArchive(crop.getId(), parent.getContent())) {
                errors.add("Crop information not present in parent Book.");
            }

            if (crop.getLeft() < 0.0 || crop.getRight() < 0.0
                    || crop.getTop() < 0.0 || crop.getBottom() < 0.0) {
                errors.add("Crop cannot have negative numbers. [" + crop + "]");
            }
            if (crop.getLeft() > 1.0 || crop.getRight() > 1.0 || crop.getTop() > 1.0 || crop.getBottom() > 1.0) {
                errors.add("Crop cannot have numbers greater than 1.0 [" + crop + "]");
            }
        }

        return errors;
    }

    /**
     * <ul>
     *     <li>The item containing the checksum information must exist in the archive.</li>
     *     <li>For each hash in the list, there must be an ID to identify the item in the
     *         archive associated with the hash.</li>
     *     <li>For each hash in the list, the hashing algorithm used must be identified</li>
     *     <li>For each hash in the list, the hash value must be defined.</li>
     * </ul>
     *
     * @param info item to check
     * @param parent containing Book
     * @return list of errors found during checking
     */
    private List<String> check(ChecksumInfo info, Book parent) {
        List<String> errors = new ArrayList<>();

        if (info == null) {
            errors.add("Checksum information missing.");
            return errors;
        }

        if (!isInArchive(info.getId(), parent.getContent())) {
            errors.add("Checksum information not present in archive.");
        }

        for (String dataId : info.getAllIds()) {
            ChecksumData data = info.getChecksumDataForId(dataId);

            if (StringUtils.isBlank(data.getId())) {
                errors.add("ID missing for checksum: [" + data + "]");
            } else {
                if (!isInArchive(data.getId(), parent.getContent())) {
                    errors.add("ID associated with checksum does not exist in the archive. [" + data + "]");
                }
            }
            if (data.getAlgorithm() == null) {
                errors.add("Hashing algorithm not defined for checksum: [" + data + "]");
            }
            if (StringUtils.isBlank(data.getHash())) {
                errors.add("Hash value missing. [" + data + "]");
            }
            if (!StringUtils.isNumeric(data.getHash())) {
                errors.add("Hash value is non-numeric. " + data + "]");
            }
        }

        return errors;
    }

    /**
     * <ul>
     *     <li>The {@link rosa.archive.model.BookStructure} must exist in the Book object.</li>
     *     <li>The associated item in the archive must exist (Reduced Tagging).</li>
     *     <li>For each page in the structure, there must be an ID, name and front and back sides.</li>
     * </ul>
     *
     * @param structure item to check
     * @param parent parent container
     * @return list of errors found while performing the check
     */
    private List<String> check(BookStructure structure, Book parent) {
        List<String> errors = new ArrayList<>();

        if (structure == null) {
            errors.add("Reduced tagging missing.");
            return errors;
        }

        if (!isInArchive(structure.getId(), parent.getContent())) {
            errors.add("Reduced tagging missing from archive.");
        }

        for (StructurePage page : structure) {
            if (StringUtils.isBlank(page.getId())) {
                errors.add("Page ID missing. [" + page + "]");
            }
            if (StringUtils.isBlank(page.getName())) {
                errors.add("Page name missing. [" + page + "]");
            }
            if (page.getVerso() != null) {
                errors.addAll(check(page.getVerso()));
            }
            if (page.getRecto() != null) {
                errors.addAll(check(page.getRecto()));
            }
        }

        return errors;
    }

    /**
     * <ul>
     *     <li>The side of the page must exist.</li>
     *     <li>The side must have 0 or more columns. Each column must be checked.</li>
     *     <li>The side must have 0 or more items spanning the side.
     *         Each item spanning the side must be checked.</li>
     * </ul>
     *
     * @param side side of a page to check
     * @return list of errors found while checking
     */
    private List<String> check(StructurePageSide side) {
        List<String> errors = new ArrayList<>();

        if (side == null) {
            errors.add("Side of page missing.");
            return errors;
        }

        if (side.columns() != null) {
            for (StructureColumn column : side.columns()) {
                errors.addAll(check(column));
            }
        }
        if (side.spanning() != null) {
            for (Item item : side.spanning()) {
                errors.addAll(check(item));
            }
        }

        return errors;
    }

    /**
     * <ul>
     *     <li>The column must exist.</li>
     *     <li>The column must exist on a page.</li>
     *     <li>The column must have a single letter designation.</li>
     *     <li>Other information describing the column must be present,
     *         or be initialized to a reasonable default.</li>
     * </ul>
     *
     * @param column column of stuff on a page
     * @return list of errors found while checking
     */
    private List<String> check(StructureColumn column) {
        List<String> errors = new ArrayList<>();

        if (column == null) {
            errors.add("Column is missing.");
            return errors;
        }

        if (StringUtils.isBlank(column.getFirstLineTranscribed())) {
            errors.add("First line of column is not transcribed. [" + column + "]");
        }
        if (StringUtils.isBlank(column.getParentSide())) {
            errors.add("Parent side of column missing. [" + column + "]");
        }
        if (column.getColumnLetter() == '\u0000') {
            errors.add("Column letter designation missing. [" + column + "]");
        }
        if (column.getFirstLineCriticalEdition() < 0) {
            errors.add("First line of the critical edition not defined. [" + column + "]");
        }
        if (column.getTotalLines() < 0) {
            errors.add("Total lines of column not defined. [" + column + "]");
        }
        if (column.getItems() != null) {
            for (Item item : column.getItems()) {
                errors.addAll(check(item));
            }
        }

        return errors;
    }

    /**
     * <ul>
     *     <li>The item must exist.</li>
     *     <li>The item must take up 0 or more lines.</li>
     * </ul>
     *
     * @param item item to check
     * @return list of errors found during the check
     */
    private List<String> check(Item item) {
        List<String> errors = new ArrayList<>();

        if (item == null) {
            errors.add("Item is missing.");
            return errors;
        }

        if (item.getLines() < 0) {
            errors.add("Lines is negative. [" + item + "]");
        }

        return errors;
    }

    private List<String> check(IllustrationTagging tagging, Book parent) {
        List<String> errors = new ArrayList<>();

        if (tagging == null) {
            errors.add("Image tagging missing.");
            return errors;
        }

        if (!isInArchive(tagging.getId(), parent.getContent())) {
            errors.add("Image tagging missing from archive.");
        }

        for (Illustration illustration : tagging) {
            if (StringUtils.isBlank(illustration.getId())) {
                errors.add("Illustration ID missing. [" + illustration + "]");
            }
            if (StringUtils.isBlank(illustration.getPage())) {
                errors.add("Illustration page missing. [" + illustration + "]");
            }
            if (illustration.getTitles().length == 0) {
                errors.add("Illustration title missing. [" + illustration + "]");
            } else {
                for (String id : illustration.getTitles()) {
                    if (!StringUtils.isNumeric(id)) {
                        errors.add("Illustration ID is non-numeric. [" + illustration + "]");
                    }
                    // TODO compare with IDs in collection's IllustrationTitles
                }
            }
            if (illustration.getCharacters().length == 0) {
                errors.add("Illustration characters missing. [" + illustration + "]");
            } else {
                for (String id : illustration.getCharacters()) {
                    if (!StringUtils.isNumeric(id)) {
                        errors.add("Illustration character ID is non-numeric.[" + illustration + "]");
                    }
                    // TODO compare with IDs in collections CharacterNames
                }
            }
        }

        return errors;
    }

    private List<String> check(NarrativeTagging tagging, Book parent) {
        List<String> errors = new ArrayList<>();

        if (tagging == null) {
            errors.add("Narrative tagging missing.");
            return errors;
        }

        if (!isInArchive(tagging.getId(), parent.getContent())) {
            errors.add("Narrative tagging not found in archive. [" + tagging + "]");
        }

        for (BookScene scene : tagging) {
            // TODO
        }

        return errors;
    }

}
