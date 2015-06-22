package rosa.search.core;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.pattern.PatternTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.QueryBuilder;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookDescription;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookScene;
import rosa.archive.model.BookStructure;
import rosa.archive.model.CharacterName;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.NarrativeScene;
import rosa.archive.model.NarrativeSections;
import rosa.archive.model.NarrativeTagging;
import rosa.archive.model.Transcription;
import rosa.archive.model.redtag.Item;
import rosa.archive.model.redtag.Rubric;
import rosa.archive.model.redtag.StructureColumn;
import rosa.archive.model.redtag.StructurePage;
import rosa.archive.model.redtag.StructurePageSide;
import rosa.search.model.QueryOperation;
import rosa.search.model.QueryTerm;
import rosa.search.model.SearchField;
import rosa.search.model.SearchFieldType;
import rosa.search.model.SearchFields;

/**
 * Handle mapping between search fields and Lucene fields, search queries and
 * Lucene queries.
 */
public class LuceneMapper {
    private final Analyzer english_analyzer;
    private final Analyzer french_analyzer;
    private final Analyzer imagename_analyzer;
    private final Analyzer string_analyzer;
    private final Analyzer main_analyzer;

    // TODO old french spelling
    // TODO charname variance using SynonymFilter? (Doesn't really handle case?
    // Can't be used for queries?)

    // Lucene field name -> search field type
    private final Map<String, SearchFieldType> lucene_field_map;

    // Search field name -> search field
    private final Map<String, SearchField> search_field_map;

    public LuceneMapper() {
        this.english_analyzer = new EnglishAnalyzer();
        this.french_analyzer = new FrenchAnalyzer();
        this.string_analyzer = new WhitespaceAnalyzer();

        // Tokenizes on spaces and . while removing excess 0's
        // TODO r/v?
        this.imagename_analyzer = new Analyzer() {
            Pattern pattern = Pattern.compile("\\s+|^0*|\\.0*");

            @Override
            protected TokenStreamComponents createComponents(String arg0) {
                Tokenizer tokenizer = new PatternTokenizer(pattern, -1);
                TokenStream filter = new LowerCaseFilter(tokenizer);

                return new TokenStreamComponents(tokenizer, filter);
            }
        };

        this.lucene_field_map = new HashMap<>();
        this.search_field_map = new HashMap<>();

        Map<String, Analyzer> analyzer_map = new HashMap<>();

        for (SearchField sf: SearchFields.values()) {
            search_field_map.put(sf.getFieldName(), sf);

            for (SearchFieldType type: sf.getFieldTypes()) {
                String lucene_field = getLuceneField(sf, type);

                lucene_field_map.put(lucene_field, type);
                analyzer_map.put(lucene_field, get_analyzer(type));
            }
        }

        this.main_analyzer = new PerFieldAnalyzerWrapper(string_analyzer,
                analyzer_map);
    }

    public String getLuceneField(SearchField sf, SearchFieldType type) {
        return sf.getFieldName() + "." + type.name();
    }

    private Analyzer get_analyzer(SearchFieldType type) {
        switch (type) {
        case ENGLISH:
            return english_analyzer;
        case FRENCH:
            return french_analyzer;
        case IMAGE_NAME:
            return imagename_analyzer;
        case OLD_FRENCH:
            return french_analyzer;
        case STRING:
            return string_analyzer;
        default:
            return null;
        }
    }

    public Analyzer getAnalyzer() {
        return main_analyzer;
    }

    public Query createLuceneQuery(rosa.search.model.Query query) {
        if (query.isOperation()) {
            BooleanQuery result = new BooleanQuery();
            Occur occur = query.getOperation() == QueryOperation.AND ? Occur.MUST
                    : Occur.SHOULD;

            for (rosa.search.model.Query kid: query.children()) {
                result.add(createLuceneQuery(kid), occur);
            }

            return result;
        } else {
            return create_lucene_query(query.getTerm());
        }
    }

    // TODO old french complications etc.

    private Query create_lucene_query(QueryTerm term) {
        SearchField sf = search_field_map.get(term.getField());

        if (sf == null) {
            // TODO
            return null;
        }

        if (sf.getFieldTypes().length == 1) {
            return create_lucene_query(sf, sf.getFieldTypes()[0],
                    term.getValue());
        } else {
            BooleanQuery query = new BooleanQuery();

            for (SearchFieldType type: sf.getFieldTypes()) {
                query.add(create_lucene_query(sf, type, term.getValue()),
                        Occur.SHOULD);
            }

            return query;
        }
    }

    private Query create_lucene_query(SearchField sf, SearchFieldType type,
            String query) {
        String lucene_field = getLuceneField(sf, type);
        QueryBuilder builder = new QueryBuilder(main_analyzer);

        switch (type) {
        case ENGLISH:
            return builder.createPhraseQuery(lucene_field, query);
        case FRENCH:
            return builder.createPhraseQuery(lucene_field, query);
        case IMAGE_NAME:
            return builder.createPhraseQuery(lucene_field, query);
        case OLD_FRENCH:
            return builder.createPhraseQuery(lucene_field, query);
        case STRING:
            return builder.createPhraseQuery(lucene_field, query);
        default:
            return null;
        }
    }

    private void add_field(Document doc, SearchField sf, String value) {
        for (SearchFieldType type: sf.getFieldTypes()) {
            doc.add(create_field(getLuceneField(sf, type), type, value));
        }
    }

    private void add_field(Document doc, SearchField sf, SearchFieldType type,
            String value) {
        doc.add(create_field(getLuceneField(sf, type), type, value));
    }

    private IndexableField create_field(String name, SearchFieldType type,
            String value) {
        switch (type) {
        case ENGLISH:
            return new TextField(name, value, Store.YES);
        case FRENCH:
            return new TextField(name, value, Store.YES);
        case IMAGE_NAME:
            return new TextField(name, value, Store.YES);
        case OLD_FRENCH:
            return new TextField(name, value, Store.YES);
        case STRING:
            return new StringField(name, value, Store.YES);
        default:
            return null;
        }
    }

    public List<Document> createDocuments(BookCollection col, Book book)
            throws IOException {
        List<Document> result = new ArrayList<>();

        // Create document for book

        {
            Document doc = new Document();

            index(doc, col, book);

            result.add(doc);
        }

        // Create document for each image

        ImageList images = book.getImages();

        for (BookImage image: images.getImages()) {
            Document doc = new Document();

            index(doc, col, book, image);
            result.add(doc);
        }

        return result;
    }

    private static String xml_to_text(InputSource src) throws SAXException,
            IOException {
        XMLReader r = XMLReaderFactory.createXMLReader();
        final StringBuilder result = new StringBuilder();

        r.setContentHandler(new DefaultHandler() {
            public void characters(char[] text, int offset, int len)
                    throws SAXException {
                result.append(text, offset, len);
            }

        });

        r.parse(src);

        return result.toString();
    }

    private void index(Document doc, BookCollection col, Book book)
            throws IOException {
        add_field(doc, SearchFields.ID,
                SearchUtil.createId(col.getId(), book.getId()));
        add_field(doc, SearchFields.COLLECTION_ID, col.getId());
        add_field(doc, SearchFields.BOOK_ID, book.getId());

        try {
            for (String lc: col.getAllSupportedLanguages()) {

                BookDescription desc = book.getBookDescription(lc);

                if (desc != null) {
                    SearchFieldType type = SearchFieldType.ENGLISH;

                    // TODO Need Constants for LCs
                    if (lc.equals("fr")) {
                        type = SearchFieldType.FRENCH;
                    }

                    add_field(
                            doc,
                            SearchFields.DESCRIPTION_TEXT,
                            type,
                            xml_to_text(new InputSource(new StringReader(desc
                                    .getXML()))));
                }
            }
        } catch (SAXException e) {
            throw new IOException("Failure to parse xml description of "
                    + book.getId(), e);
        }
    }

    private void index(Document doc, BookCollection col, Book book,
            BookImage image) {
        add_field(doc, SearchFields.ID,
                SearchUtil.createId(col.getId(), book.getId(), image.getId()));
        add_field(doc, SearchFields.COLLECTION_ID, col.getId());
        add_field(doc, SearchFields.BOOK_ID, book.getId());
        add_field(doc, SearchFields.IMAGE_NAME, image.getName());

        IllustrationTagging imgtag = book.getIllustrationTagging();

        IllustrationTitles titles = col.getIllustrationTitles();
        CharacterNames char_names = col.getCharacterNames();

        if (imgtag != null) {
            StringBuilder char_field = new StringBuilder();
            StringBuilder title_field = new StringBuilder();
            StringBuilder keyword_field = new StringBuilder();

            for (int index: imgtag.findImageIndices(book, image.getId())) {
                Illustration illus = imgtag.getIllustrationData(index);

                for (String char_id: illus.getCharacters()) {
                    CharacterName char_name = char_names
                            .getCharacterName(char_id);

                    if (char_name == null) {
                        char_field.append(char_id);
                        char_field.append(", ");
                    } else {
                        for (String name: char_name.getAllNames()) {
                            char_field.append(name);
                            char_field.append(", ");
                        }
                    }
                }

                for (String title_id: illus.getTitles()) {
                    String title = titles.getTitleById(title_id);

                    if (title_id == null) {
                        title_field.append(title_id);
                        title_field.append(", ");
                    } else {
                        title_field.append(title);
                        title_field.append(", ");
                    }
                }

                keyword_field.append(illus.getTextualElement());
                keyword_field.append(", ");
                keyword_field.append(illus.getArchitecture());
                keyword_field.append(", ");
                keyword_field.append(illus.getCostume());
                keyword_field.append(", ");
                keyword_field.append(illus.getOther());
                keyword_field.append(", ");
                keyword_field.append(illus.getObject());
                keyword_field.append(", ");
                keyword_field.append(illus.getLandscape());
                keyword_field.append(", ");
            }

            if (char_field.length() > 0) {
                add_field(doc, SearchFields.ILLUSTRATION_CHAR,
                        char_field.toString());
            }

            if (title_field.length() > 0) {
                add_field(doc, SearchFields.ILLUSTRATION_TITLE,
                        title_field.toString());
            }

            if (keyword_field.length() > 0) {
                add_field(doc, SearchFields.ILLUSTRATION_KEYWORD,
                        keyword_field.toString());
            }
        }

        // Index narrative scenes which start in image

        NarrativeTagging nartag = book.getAutomaticNarrativeTagging();

        if (nartag == null) {
            nartag = book.getManualNarrativeTagging();
        }

        NarrativeSections narsecs = col.getNarrativeSections();

        if (nartag != null) {
            StringBuilder trans_field = new StringBuilder();
            StringBuilder sectionids_field = new StringBuilder();
            StringBuilder sectiondesc_field = new StringBuilder();

            for (BookScene scene: nartag.getScenes()) {
                if (book.guessImageName(scene.getStartPage()).equals(
                        image.getId())) {

                    if (scene.getStartTranscription() != null) {
                        trans_field
                                .append(scene.getStartTranscription() + ", ");
                    }

                    sectionids_field.append(scene.getId() + " ");

                    int secindex = narsecs.findIndexOfSceneById(scene.getId());

                    if (secindex != -1) {
                        NarrativeScene ns = narsecs.asScenes().get(secindex);
                        sectiondesc_field.append(ns.getDescription() + ", ");
                    }
                }
            }

            if (trans_field.length() > 0) {
                add_field(doc, SearchFields.TRANSCRIPTION_TEXT,
                        trans_field.toString());
            }

            if (sectionids_field.length() > 0) {
                add_field(doc, SearchFields.NARRATIVE_SECTION_ID,
                        sectionids_field.toString());
            }

            if (sectiondesc_field.length() > 0) {
                add_field(doc, SearchFields.NARRATIVE_SECTION_DESCRIPTION,
                        sectionids_field.toString());
            }
        }

        // Index rubrics from reduced tagging

        StructurePageSide side = findReducedTaggingSide(book, image.getId());

        if (side != null) {
            StringBuffer rubrics = new StringBuffer();

            for (StructureColumn c: side.columns()) {
                for (Item item: c.getItems()) {
                    if (item instanceof Rubric) {
                        String rubric = ((Rubric) item).getText();

                        // Remove / used to indicate abbreviations
                        rubric = rubric.replaceAll("/", "");

                        rubrics.append(rubric + ", ");
                    }
                }
            }

            if (rubrics.length() > 0) {
                add_field(doc, SearchFields.TRANSCRIPTION_RUBRIC,
                        rubrics.toString());
            }
        }

        Transcription trans = book.getTranscription();
        
        if (trans != null) {
            // TODO
            
            
        }
    }

    // Find the page side from the reduced tagging corresponding to an image id
    private StructurePageSide findReducedTaggingSide(Book book, String image_id) {
        BookStructure struct = book.getBookStructure();

        if (struct == null) {
            return null;
        }

        for (StructurePage page: struct.pages()) {
            String test = book.guessImageName(page.getId() + "r");

            if (test != null && image_id.equals(test)) {
                return page.getRecto();
            }

            test = book.guessImageName(page.getId() + "v");

            if (test != null && image_id.equals(test)) {
                return page.getVerso();
            }
        }

        return null;
    }

    public String getSearchFieldNameFromLuceneField(String lucene_field) {
        int i = lucene_field.lastIndexOf('.');
        
        if (i == -1) {
            return lucene_field;
        }
        
        return lucene_field.substring(0, i);
    }
}
