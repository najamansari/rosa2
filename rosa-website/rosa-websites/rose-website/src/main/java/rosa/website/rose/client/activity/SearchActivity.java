package rosa.website.rose.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.RangeChangeEvent.Handler;
import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.QueryTerm;
import rosa.search.model.SearchMatch;
import rosa.search.model.SearchOptions;
import rosa.search.model.SearchResult;
import rosa.website.core.client.ArchiveDataServiceAsync;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.FSIUtil;
import rosa.website.core.client.Labels;
import rosa.website.core.client.event.SidebarItemSelectedEvent;
import rosa.website.core.client.place.AdvancedSearchPlace;
import rosa.website.core.client.view.AdvancedSearchView;
import rosa.website.core.client.widget.LoadingPanel;
import rosa.website.model.csv.BookDataCSV;
import rosa.website.model.csv.BookDataCSV.Column;
import rosa.website.model.csv.CSVData;
import rosa.website.model.csv.CSVRow;
import rosa.website.model.csv.CSVType;
import rosa.website.model.select.BookInfo;
import rosa.website.rose.client.WebsiteConfig;
import rosa.website.search.client.QueryUtil;
import rosa.website.search.client.RosaQueryUtil;
import rosa.website.core.client.RosaSearchServiceAsync;
import rosa.website.search.client.model.SearchCategory;
import rosa.website.search.client.model.SearchMatchModel;
import rosa.website.search.client.model.SearchResultModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SearchActivity implements Activity {
    private static final Logger LOG = Logger.getLogger(SearchActivity.class.toString());
    private static final QueryUtil QUERY_UTIL = new RosaQueryUtil();
    private static final int MATCH_COUNT = 20;// Number of matches to return from search service. Can be used with paging.
    private static final int THUMB_WIDTH = 64;
    private static final int THUMB_HEIGHT = 64;

    private final AdvancedSearchPlace place;
    private final AdvancedSearchView view;
    private final ArchiveDataServiceAsync archiveDataService;
    private final RosaSearchServiceAsync searchService;

    private String resumeToken = null;     // For use in paging
    private BookDataCSV bookDataCSV;

    private HandlerRegistration rangeChangeHandler;
    private HandlerRegistration searchButtonHandler;

    /**
     * @param place initial search state
     * @param clientFactory .
     */
    public SearchActivity(AdvancedSearchPlace place, ClientFactory clientFactory) {
        this.place = place;
        this.view = clientFactory.advancedSearchView();
        this.archiveDataService = clientFactory.archiveDataService();
        this.searchService = clientFactory.searchService();
    }

    private void finishActivity() {
        if (rangeChangeHandler != null) {
            rangeChangeHandler.removeHandler();
        }
        if (searchButtonHandler != null) {
            searchButtonHandler.removeHandler();
        }

        view.clear();
        LoadingPanel.INSTANCE.hide();
    }

    @Override
    public String mayStop() {
        return null;
    }

    @Override
    public void onCancel() {
        finishActivity();
    }

    @Override
    public void onStop() {
        finishActivity();
    }

    @Override
    public void start(AcceptsOneWidget panel, EventBus eventBus) {
        LoadingPanel.INSTANCE.show();
        panel.setWidget(view);

        eventBus.fireEvent(new SidebarItemSelectedEvent("NULL"));

        view.clear();
        initView();

        String collection = WebsiteConfig.INSTANCE.collection();
        String lang = LocaleInfo.getCurrentLocale().getLocaleName();

        archiveDataService.loadCSVData(collection, lang, CSVType.COLLECTION_BOOKS, new AsyncCallback<CSVData>() {
            @Override
            public void onFailure(Throwable caught) {
                LOG.log(Level.SEVERE, "Failed to get book data.", caught);
            }

            @Override
            public void onSuccess(CSVData result) {
                if (result instanceof BookDataCSV) {
                    setSearchModel((BookDataCSV) result);
                } else {
                    LOG.log(Level.SEVERE, "Cannot initialize search widget, bad data returned from server.");
                }
                LoadingPanel.INSTANCE.hide();
            }
        });
    }

    private void initView() {
        view.setAddFieldButtonText(Labels.INSTANCE.addSearchField());
        view.setSearchButtonText(Labels.INSTANCE.search());
        view.setRemoveButtonText(Labels.INSTANCE.removeSearchField());
        view.setClearBooksButtonText(Labels.INSTANCE.clearTextBox());
        view.setSearchFieldLabels(getCategoryLabels());

        view.setAvailableSearchFields(SearchCategory.values());
        view.setAvailableSearchOperations(QueryOperation.values());

        searchButtonHandler = view.addSearchButtonClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                String query = view.getSearchQuery();       // This query string will have to be URL encoded first
                if (query != null && !query.isEmpty()) {
                    History.newItem("search;" + view.getSearchQuery(), false);
                    // Do search
                    performSearch(query);
                }
            }
        });
    }

    private void setSearchModel(BookDataCSV data) {
        this.bookDataCSV = data;

        List<BookInfo> books = new ArrayList<>();
        books.add(new BookInfo("Restrict by book:", null));
        for (CSVRow row : data) {
            books.add(new BookInfo(row.getValue(Column.COMMON_NAME), row.getValue(Column.ID)));
        }
        view.addBooksToRestrictionList(books.toArray(new BookInfo[books.size()]));

        setData(books);
    }

    private void setData(List<BookInfo> books) {
        if (place == null || place.getSearchToken() == null || place.getSearchToken().isEmpty()) {
            view.addQueryField();
            view.addQueryField();
            view.addQueryField();

            return;
        }
        final int OPERATION_AND = QueryOperation.AND.ordinal();
        final int CATEGORY_ALL = SearchCategory.ALL.ordinal();

        List<QueryTerm> terms = QUERY_UTIL.queryParts(place.getSearchToken());
        for (QueryTerm term : terms) {
            SearchCategory category = SearchCategory.category(term.getField());
            view.addQueryField(term.getValue(), OPERATION_AND, category == null ? CATEGORY_ALL : category.ordinal());
        }

        String[] restrictedBooks = QUERY_UTIL.bookRestrictionList(place.getSearchToken());
        for (String book : restrictedBooks) {
            BookInfo bookInfo = getBook(book, books);
            if (bookInfo != null) {
                view.setBooksAsRestricted(bookInfo);
            }
        }

        LOG.info("Setting initial data. " + place.toString());
        performSearch(place.getSearchToken());
    }

    private BookInfo getBook(String book, List<BookInfo> books) {
        for (BookInfo b : books) {
            if (b.id.equals(book) || b.title.equals(book)) {

                return b;
            }
        }

        return null;
    }

    private void performSearch(final String searchToken) {
        if (rangeChangeHandler != null) {
            rangeChangeHandler.removeHandler();
        }

        final Query query = QUERY_UTIL.toQuery(searchToken, WebsiteConfig.INSTANCE.collection());
        resumeToken = null;

        if (query == null) {
            return;
        }

        /*
            Search is done inside this RangeChangeHandler
        */
        rangeChangeHandler = view.addRangeChangeHandler(new Handler() {
            @Override
            public void onRangeChange(RangeChangeEvent event) {
                // Results range (start, length)
                final int start = event.getNewRange().getStart();
                int length = event.getNewRange().getLength();

                // Use resume token only if progressing forward in search results
                SearchOptions options;
                if (QUERY_UTIL.offset(searchToken) < start) {
                    options = new SearchOptions(start, (length == 0 ? MATCH_COUNT : length), resumeToken);
                } else {
                    options = new SearchOptions(start, (length == 0 ? MATCH_COUNT : length), null);
                }

                searchService.search(query, options, new AsyncCallback<SearchResult>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        LOG.log(Level.SEVERE, "Search failed.", caught);
                    }

                    @Override
                    public void onSuccess(SearchResult result) {
                        // Update history token, but do not navigate away
                        if (QUERY_UTIL.offset(searchToken) != start) {
                            History.newItem("search;" + QUERY_UTIL.changeOffset(searchToken, start), false);
                        }

                        SearchResultModel model = adaptSearchResults(result);
                        resumeToken = model.getResumeToken();
                        view.setRowCount((int) model.getTotal());  // NOTE: casting long to int can result in data loss
                        view.setRowData(start, model.getMatchList());
                    }
                });
            }
        });

        LOG.info("Performing search. [" + searchToken + "]  {" + this + "}");
        // Setting visible range will trigger a RangeChangeEvent, picked up by the newly defined
        // RangeChangeHandler. This performs the initial search.
        view.setVisibleRange(QUERY_UTIL.offset(searchToken), MATCH_COUNT);
    }

    /**
     * @param result search result
     * @return Search results adapted to a form that can be displayed
     */
    private SearchResultModel adaptSearchResults(SearchResult result) {
        SearchResultModel model = new SearchResultModel(result);

        for (SearchMatch match : result.getMatches()) {
            String pageId = QUERY_UTIL.getPageID(match);
            String bookId = QUERY_UTIL.getBookID(match);

            String fsiUrl;
            String targetUrl = GWT.getHostPageBaseURL();

            if (pageId == null || pageId.isEmpty()) {
                targetUrl += "#book;" + bookId;
                fsiUrl = null;
            } else {
                targetUrl += "#read;" + pageId;
                fsiUrl = FSIUtil.getFSIImageUrl(
                        WebsiteConfig.INSTANCE.fsiShare(),
                        bookId,
                        pageId,
                        THUMB_WIDTH,
                        THUMB_HEIGHT,
                        WebsiteConfig.INSTANCE.fsiUrl()
                );
            }

            model.addSearchMatch(new SearchMatchModel(match, fsiUrl, targetUrl, getDisplayName(bookId, pageId)));
        }

        return model;
    }

    /**
     * Get the name of a book as it should be displayed in the search results.
     *
     * @param bookId book
     * @param pageId page
     * @return .
     */
    private String getDisplayName(String bookId, String pageId) {
        if (pageId != null) {
            CSVRow row = bookDataCSV.getRow(bookId);

            if (row != null) {
                return pageId + ": "
                        + row.getValue(Column.REPO) + " "
                        + row.getValue(Column.SHELFMARK);
            }
        }

        return bookId;
    }

    private Map<SearchCategory, String> getCategoryLabels() {
        Map<SearchCategory, String> labels = new HashMap<>();

        for (SearchCategory cat : SearchCategory.values()) {
            String label = null;
            switch (cat) {
                case POETRY:
                    label = Labels.INSTANCE.linesOfVerse();
                    break;
                case RUBRIC:
                    label = Labels.INSTANCE.rubric();
                    break;
                case ILLUSTRATION_TITLE:
                    label = Labels.INSTANCE.illustrationTitles();
                    break;
                case LECOY:
                    label = Labels.INSTANCE.lecoy();
                    break;
                case NOTE:
                    label = Labels.INSTANCE.criticalNote();
                    break;
                case ILLUSTRATION_CHAR:
                    label = Labels.INSTANCE.illustrationChar();
                    break;
                case ILLUSTRATION_KEYWORDS:
                    label = Labels.INSTANCE.illustrationKeywords();
                    break;
                case DESCRIPTION:
                    label = Labels.INSTANCE.bookDescription();
                    break;
                case IMAGE:
                    label = Labels.INSTANCE.imageName();
                    break;
                case NARRATIVE_SECTION:
                    label = Labels.INSTANCE.narrativeSections();
                    break;
                case ALL:
                    label = Labels.INSTANCE.allFields();
                    break;
                default:
                    break;
            }

            if (label != null && !label.isEmpty()) {
                labels.put(cat, label);
            }
        }

        return labels;
    }
}
