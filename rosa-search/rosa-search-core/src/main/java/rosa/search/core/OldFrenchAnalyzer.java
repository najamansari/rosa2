package rosa.search.core;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.fr.FrenchLightStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.ElisionFilter;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.analysis.util.WordlistLoader;
import org.apache.lucene.util.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Taken from Lucene's FrenchAnalyzer
 *
 * An extension of Lucene's French analyzer that passes the tokens through
 * an extra filter that will normalize the differences in spelling in Old
 * French.
 *
 * {@link org.apache.lucene.analysis.fr.FrenchAnalyzer}
 */
public class OldFrenchAnalyzer extends StopwordAnalyzerBase {

    /** File containing default French stopwords. */
    public final static String DEFAULT_STOPWORD_FILE = "french_stop.txt";

    /** Default set of articles for ElisionFilter */
    public static final CharArraySet DEFAULT_ARTICLES = CharArraySet.unmodifiableSet(
            new CharArraySet(
                    Arrays.asList(
                            "l", "m", "t", "qu", "n", "s", "j", "d", "c", "jusqu", "quoiqu", "lorsqu", "puisqu"
                    ), true)
    );

    /**
     * Contains words that should be indexed but not stemmed.
     */
    private final CharArraySet excltable;
    private final Map<String, Set<String>> spellingEquivalenceTable;
    private final Map<String, Set<String>> nameVariantsTable;

    /**
     * Returns an unmodifiable instance of the default stop-words set.
     * @return an unmodifiable instance of the default stop-words set.
     */
    public static CharArraySet getDefaultStopSet(){
        return OldFrenchAnalyzer.DefaultSetHolder.DEFAULT_STOP_SET;
    }

    private static class DefaultSetHolder {
        static final CharArraySet DEFAULT_STOP_SET;
        static {
            try {
                DEFAULT_STOP_SET = WordlistLoader.getSnowballWordSet(IOUtils.getDecodingReader(SnowballFilter.class,
                        DEFAULT_STOPWORD_FILE, StandardCharsets.UTF_8));
            } catch (IOException ex) {
                // default set should always be present as it is part of the distribution (JAR)
                throw new RuntimeException("Unable to load default stopword set");
            }
        }
    }

    /**
     * Builds an analyzer with the default stop words ({@link #getDefaultStopSet}).
     */
    public OldFrenchAnalyzer() {
        this(OldFrenchAnalyzer.DefaultSetHolder.DEFAULT_STOP_SET);
    }

    /**
     * Builds an analyzer with the given stop words
     *
     * @param stopwords
     *          a stopword set
     */
    public OldFrenchAnalyzer(CharArraySet stopwords){
        this(stopwords, CharArraySet.EMPTY_SET);
    }

    /**
     * Builds an analyzer with the given stop words
     *
     * @param stopwords
     *          a stopword set
     * @param stemExclusionSet
     *          a stemming exclusion set
     */
    public OldFrenchAnalyzer(CharArraySet stopwords, CharArraySet stemExclusionSet) {
        this(stopwords, stemExclusionSet, Collections.emptyMap());

    }

    /**
     * Builds and analyzer with the given parameters.
     *
     * @param stopwords
     *          a stop word set
     * @param stemExclusionSet
     *          a stemming exclusion set
     * @param spellingEquivalenceTable
     *          a table of equivalent spelling relations
     */
    public OldFrenchAnalyzer(CharArraySet stopwords, CharArraySet stemExclusionSet,
                             Map<String, Set<String>> spellingEquivalenceTable) {
        // NOTE: using Collections.emptyMap() gives an unmodifiable empty map
        this(stopwords, stemExclusionSet, spellingEquivalenceTable, Collections.emptyMap());
    }

    /**
     * Builds and analyzer with the given parameters.
     *
     * @param stopwords
     *          a stop word set
     * @param stemExclusionSet
     *          a stemming exclusion set
     * @param spellingEquivalenceTable
     *          a table of equivalent spelling relations
     * @param nameVariantsTable
     *          a table of name variants
     */
    public OldFrenchAnalyzer(CharArraySet stopwords, CharArraySet stemExclusionSet,
                            Map<String, Set<String>> spellingEquivalenceTable,
                            Map<String, Set<String>> nameVariantsTable) {
        super(stopwords);
        this.excltable = CharArraySet.unmodifiableSet(CharArraySet.copy(stemExclusionSet));
        this.spellingEquivalenceTable = spellingEquivalenceTable == null ? new HashMap<>() : spellingEquivalenceTable;
        this.nameVariantsTable = nameVariantsTable == null ? new HashMap<>() : nameVariantsTable;
    }

    /**
     * Creates
     * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
     * used to tokenize all the text in the provided {@link Reader}.
     *
     * Somewhat simplified from stock Lucene FrenchAnalyzer implementation, since
     * this particular implementation does not need to worry about Lucene version.
     *
     * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
     *         built from a {@link StandardTokenizer} filtered with
     *         {@link StandardFilter}, {@link ElisionFilter},
     *         {@link LowerCaseFilter}, {@link StopFilter},
     *         {@link SetKeywordMarkerFilter} if a stem exclusion set is
     *         provided, and {@link FrenchLightStemFilter}
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer source = new StandardTokenizer();
        TokenStream result = new StandardFilter(source);
        if (!nameVariantsTable.isEmpty())
            result = new SpellingVariationTokenFilter(result, nameVariantsTable);
        result = new ElisionFilter(result, DEFAULT_ARTICLES);
        result = new LowerCaseFilter(result);
        result = new StopFilter(result, stopwords);
        if(!excltable.isEmpty())
            result = new SetKeywordMarkerFilter(result, excltable);
        result = new FrenchLightStemFilter(result);
        if (!spellingEquivalenceTable.isEmpty())
            result = new SpellingVariationTokenFilter(result, spellingEquivalenceTable);
        return new TokenStreamComponents(source, result);
    }

}