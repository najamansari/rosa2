package rosa.website.core.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.website.model.csv.BookDataCSV;
import rosa.website.model.csv.CollectionCSV;
import rosa.website.model.csv.IllustrationTitleCSV;

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
 * <servlet>
 *     <servlet-name>archiveService</servlet-name>
 *     <servlet-class>rosa.website.core.server.ArchiveDataServiceImpl</servlet-class>
 * </servlet>
 *
 * <servlet-mapping>
 *     <servlet-name>archiveService</servlet-name>
 *     <url-pattern>/data</url-pattern>
 * </servlet-mapping>
 * </code>
 */
@RemoteServiceRelativePath("data")
public interface ArchiveDataService extends RemoteService {
    /**
     * Load data about a collection in the archive in CSV format.
     *
     * @param collection book collection in the archive
     * @return collection data
     * @throws IOException if the collection does not exist or is not available
     */
    CollectionCSV loadCollectionData(String collection, String lang) throws IOException;

    /**
     * Load data about the books held within a collection in the archive.
     *
     * @param collection collection in the archive
     * @return book data
     * @throws IOException if the collection or any books within are not available
     */
    BookDataCSV loadCollectionBookData(String collection) throws IOException;

    /**
     * Get data about illustrations in the collection with respect to book in
     * the collection.
     *
     * @param collection collection in the archive
     * @return illustration titles
     * @throws IOException if the collection or any books are unavailable
     */
    IllustrationTitleCSV loadIllustrationTitles(String collection) throws IOException;

    /**
     * Load a BookCollection object from the archive.
     *
     * @param collection name of the book collection
     * @return the book collection
     * @throws IOException if the collection is unavailable
     */
    BookCollection loadBookCollection(String collection) throws IOException;

    /**
     * Load a Book object from a particular collection.
     *
     * @param collection name of the collection
     * @param book name of the book
     * @return the book
     * @throws IOException if the collection or book are not available
     */
    Book loadBook(String collection, String book) throws IOException;
}
