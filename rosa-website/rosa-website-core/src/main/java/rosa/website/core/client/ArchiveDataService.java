package rosa.website.core.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import rosa.archive.model.ImageList;
import rosa.website.core.shared.RosaConfigurationException;
import rosa.website.model.view.BookDescriptionViewModel;
import rosa.website.model.view.FSIViewerModel;
import rosa.website.model.csv.BookDataCSV;
import rosa.website.model.csv.CSVData;
import rosa.website.model.csv.CollectionCSV;
import rosa.website.model.csv.CSVType;
import rosa.website.model.csv.IllustrationTitleCSV;
import rosa.website.model.select.BookSelectList;
import rosa.website.model.select.SelectCategory;

import java.io.IOException;

/**
 * An RPC service for loading data from the archive.
 *
 * In order to use this service in a GWT client, an entry for
 * the service's servlet must be added to the apps web.xml. When this is updated,
 * a client can use the Async interface with callbacks.
 *
 * EX:
 * <code>
 * &lt;servlet&gt;
 *     &lt;servlet-name&gt;archiveService&lt;/servlet-name&gt;
 *     &lt;servlet-class&gt;rosa.website.core.server.ArchiveDataServiceImpl&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 *
 * &lt;servlet-mapping&gt;
 *     &lt;servlet-name&gt;archiveService&lt;/servlet-name&gt;
 *     &lt;url-pattern&gt;/data&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;
 * </code>
 */
@RemoteServiceRelativePath("data")
public interface ArchiveDataService extends RemoteService {
    /**
     * @param collection book collection name
     * @param lang desired language
     * @param type type of CSV to return
     * @return CSV data
     * @throws IOException if archive is not available
     * @throws RosaConfigurationException if archive has been misconfigured
     */
    CSVData loadCSVData(String collection, String lang, CSVType type) throws IOException, RosaConfigurationException;

    /**
     * Load data about a collection in the archive in CSV format. Data is read from
     * each book's description/metadata file to populate each column. The columns
     * are sorted by book (common) name.
     *
     * @param collection book collection in the archive
     * @param lang language code
     * @return collection data
     * @throws IOException if the collection does not exist or is not available
     * @throws RosaConfigurationException if archive has been misconfigured
     */
    CollectionCSV loadCollectionData(String collection, String lang) throws IOException, RosaConfigurationException;

    /**
     * Load data about the books held within a collection in the archive.
     *
     * @param collection collection in the archive
     * @param lang language code
     * @return book data
     * @throws IOException if the collection or any books within are not available
     * @throws RosaConfigurationException if archive has been misconfigured
     */
    BookDataCSV loadCollectionBookData(String collection, String lang) throws IOException, RosaConfigurationException;

    /**
     * Load data about the books held within a collection in the archive, useful
     * for grouping them into categories for selection.
     *
     * @param collection collection in the archive
     * @param category selection category
     * @param lang language code
     * @return book selection data
     * @throws IOException if the collection or any books within are not available
     * @throws RosaConfigurationException if archive has been misconfigured
     */
    BookSelectList loadBookSelectionData(String collection, SelectCategory category, String lang) throws IOException, RosaConfigurationException;

    /**
     * Get data about illustrations in the collection with respect to book in
     * the collection. Data is adapted from the collection's illustration_titles
     * data and book data. The columns are sorted by page.
     *
     * @param collection collection in the archive
     * @return illustration titles
     * @throws IOException if the collection or any books are unavailable
     * @throws RosaConfigurationException if archive has been misconfigured
     */
    IllustrationTitleCSV loadIllustrationTitles(String collection) throws IOException, RosaConfigurationException;

    /**
     * Get the permission statement regarding the use of a book.
     *
     * @param collection name of the collection
     * @param book name of the book
     * @param lang language code
     * @return permission statement for use of the book
     * @throws IOException if archive is not available or book/collection do not exist
     * @throws RosaConfigurationException if archive has been misconfigured
     */
    String loadPermissionStatement(String collection, String book, String lang) throws IOException, RosaConfigurationException;

    /**
     * Get the CSV image list of a book as a String.
     *
     * @param collection name of collection
     * @param book name of book
     * @return image list as a String
     * @throws IOException if archive is not available
     * @throws RosaConfigurationException if archive has been misconfigured
     */
    String loadImageListAsString(String collection, String book) throws IOException, RosaConfigurationException;

    /**
     * Get the CSV image list of a book.
     *
     * @param collection name of collection
     * @param book name of book
     * @return image list as a String
     * @throws IOException .
     * @throws RosaConfigurationException if archive has been misconfigured
     */
    ImageList loadImageList(String collection, String book) throws IOException, RosaConfigurationException;

    /**
     * Get the data model for the FSI flash viewer for a book.
     *
     * @param collection name of collection
     * @param book name of book
     * @param language language to return data
     * @return model object for the FSI flash viewer
     * @throws IOException .
     * @throws RosaConfigurationException if archive has been misconfigured
     */
    FSIViewerModel loadFSIViewerModel(String collection, String book, String language) throws IOException, RosaConfigurationException;

    /**
     * Get the data model for the book description view for a book.
     *
     * @param collection name of collection
     * @param book name of book
     * @param language language to return the data
     * @return data model object
     * @throws IOException .
     * @throws RosaConfigurationException if archive has been misconfigured
     */
    BookDescriptionViewModel loadBookDescriptionModel(String collection, String book, String language) throws IOException, RosaConfigurationException;
}
