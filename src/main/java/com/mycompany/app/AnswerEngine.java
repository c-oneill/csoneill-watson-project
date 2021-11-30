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
    private static final Analyzer analyzer = Indexer.buildCustomAnalyzer();

    private Directory directory;
    private DirectoryReader reader;
    private IndexSearcher searcher;

    /*
     * Construct an answer engine from an input filepath.
     */
    public AnswerEngine(String dirFilePath) {
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
    public AnswerEngine(Directory directory) {
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

    private TopDocs searchIndex(Query q, int n) {
        //if (this.searcher == null) return null;
        //List<ResultClass> resultList = new ArrayList<>();
        try {
            TopDocs topDocs = this.searcher.search(q, n);
//            // build the results list
//            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
//                ResultClass result = new ResultClass();
//                result.DocName = searcher.doc(scoreDoc.doc);
//                result.docScore = scoreDoc.score;
//                resultList.add(result);
//            }
            return topDocs;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


//    private Document getDocById(String docID) {
//        reader.document(docID)
//    }

    private static Query buildQuery(String query) {
        try {
            return new QueryParser("content", analyzer).parse(query);
        } catch (ParseException e) {
            return null;
        }
    }

    /*
     * Close the DirectoryReader.
     */
    public void close() throws IOException {
        reader.close();
    }

    /*
     * I can't figure out how to store the tokenized and filtered text in
     * the title field, so this is the best workaround I can figure out for
     * now.
     * Takes inputText, re-analyzes converting into a token stream, and builds
     * a string.
     */
    private static String reanalyzeTitle(String inputText) throws IOException {
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
            String indexFilePath = "testIndex";
            String inputFile = "wiki-example.txt";

            Indexer indexer = new Indexer(indexFilePath);
            indexer.addWikiFile(inputFile);
            indexer.close();

            AnswerEngine engine = new AnswerEngine(indexFilePath);
            //Query q1 = buildQuery("Products are commonly specified as meeting a particular British Standard");
            Query q1 = buildQuery("arizona politician");
            TopDocs result = engine.searchIndex(q1, 5);
            System.out.println("total hits: " + result.totalHits);
            for (ScoreDoc scoreDoc : result.scoreDocs) {
                //System.out.println(engine.reader.document(scoreDoc.doc));
                //System.out.println(engine.reader.document(scoreDoc.doc).getFields().get(1).fieldType().tokenized());
                System.out.println(reanalyzeTitle(engine.reader.document(scoreDoc.doc).get("title")));
            }

            engine.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
