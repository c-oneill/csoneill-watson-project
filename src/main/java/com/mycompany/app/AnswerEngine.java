package com.mycompany.app;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.StringJoiner;

/*
 *
 */
public class AnswerEngine {
    private static final Similarity similarity = new BM25Similarity();
    //private static Similarity similarity = new LMJelinekMercerSimilarity(0.1F);
    private final Analyzer analyzer;

    private final boolean stem;
    private final boolean lemmatize;
    private final boolean stopwords;

    private Directory directory;
    private DirectoryReader reader;
    private IndexSearcher searcher;

    /*
     * Construct an answer engine from an input filepath.
     */
    public AnswerEngine(String dirFilePath, boolean stem, boolean lemmatize, boolean stopwords) {
        this.stem = stem;
        this.lemmatize = lemmatize;
        this.stopwords = stopwords;

        analyzer = Indexer.buildCustomAnalyzer(this.stem, this.stopwords);
        try {
            this.directory = FSDirectory.open(Paths.get(dirFilePath));
            this.reader = DirectoryReader.open(this.directory);
            this.searcher = new IndexSearcher(this.reader);
            this.searcher.setSimilarity(this.similarity);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /*
     * Construct an answer engine from a Directory.
     */
    public AnswerEngine(Directory directory, boolean stem, boolean lemmatize, boolean stopwords) {
        this.stem = stem;
        this.lemmatize = lemmatize;
        this.stopwords = stopwords;

        analyzer = Indexer.buildCustomAnalyzer(this.stem, this.stopwords);
        try {
            this.directory = directory;
            this.reader = DirectoryReader.open(this.directory);
            this.searcher = new IndexSearcher(this.reader);
            this.searcher.setSimilarity(this.similarity);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public TopDocs searchIndex(Query q, int n) {
        try {
            TopDocs topDocs = this.searcher.search(q, n);
            return topDocs;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /*
     * Notice the default field to search is content.
     */
    public Query buildQuery(String query) {
        try {
            return new QueryParser("content", analyzer).parse(query);
        } catch (ParseException e) {
            return null;
        }
    }

    /*
     * Overloads build query, so we can add the category as a parameter.
     */
    public Query buildQuery(String content, String categories) {
        //content = removeSpecialChars(content);
        //categories = removeSpecialChars(categories);
        content = handleSpecialChars(content);
        categories = handleSpecialChars(categories);
        StringBuilder sb = new StringBuilder()
                .append(content);
        for (String category : categories.split("\\s+")) {
            sb.append(" categories:").append(category);
        }
        System.out.println(buildQuery(sb.toString())); // debug
        return buildQuery(sb.toString());
    }

    /*
     * Helper tool to remove special characters from a query before going
     * through the lucene query parser.
     * issue: + - && || ! ( ) { } [ ] ^ " ~ * ? : \ /
     */
    public static String removeSpecialChars(String input) {
        return input.replaceAll("[^a-zA-Z0-9\\s+]+", "");
    }

    public static String handleSpecialChars(String input) {
        String[] removeChars = {"+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]",
                "^", "~", "*", "?", ":", "\\", "/"};
        for (String c : removeChars) {
            if (input.contains(c)) {
                input = input.replace(c, "");
            }
        }
        String[] replaceChars = { "\""};
        for (String c : replaceChars) {
            if (input.contains(c)) {
                //input = input.replace(c, "\\" + c);
            }
        }
        return input;
    }

    /*
     * Close the DirectoryReader.
     */
    public void close() throws IOException {
        reader.close();
    }

    public DirectoryReader getReader() {
        return this.reader;
    }

    /*
     * I can't figure out how to store the tokenized and filtered text in
     * the title field, so this is the best workaround I can figure out for
     * now.
     * Takes inputText, re-analyzes converting into a token stream, and builds
     * a string.
     */
    public String reanalyzeTitle(String inputText) throws IOException {
        StringJoiner sj = new StringJoiner(" ");
        TokenStream tokenStream = analyzer.tokenStream("title", inputText);
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while(tokenStream.incrementToken()) {
            sj.add(attr.toString());
        }
        tokenStream.close();
        return sj.toString();
    }

    public static void main(String[] args) {
        try {
            boolean stem = false;
            boolean lemmatize = false;
            boolean stopwords = false;

            String indexFilePath = "testIndex2";
            String inputFile = "wiki-example.txt";

            Indexer indexer = new Indexer(indexFilePath, stem, lemmatize, stopwords);
            indexer.addWikiFile(inputFile);
            indexer.close();

            AnswerEngine engine = new AnswerEngine(indexFilePath, stem, lemmatize, false);
            //Query q1 = buildQuery("Products are commonly specified as meeting a particular British Standard");
            String query = "red hat";
            //String query = "categories:arizona categories:politician";
            Query q1 = engine.buildQuery(query);
            TopDocs result = engine.searchIndex(q1, 5);
            System.out.println("total hits: " + result.totalHits);
            String printLine;
            for (ScoreDoc scoreDoc : result.scoreDocs) {
                printLine = engine.reader.document(scoreDoc.doc).get("title");
                System.out.println(engine.reanalyzeTitle(printLine));
                System.out.println(printLine.substring(2, printLine.length() - 2));
                //System.out.println(engine.searcher.explain(q1, scoreDoc.doc));
            }

            engine.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
