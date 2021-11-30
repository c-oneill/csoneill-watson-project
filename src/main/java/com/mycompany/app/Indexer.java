package com.mycompany.app;

import edu.stanford.nlp.simple.Sentence;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/*
 * A class to build and save the index.
 *
 * todo notes:
 *  add a special field for topic titles in articles?
 */
public class Indexer {

    private static Similarity similarity = new BM25Similarity();
    private static Analyzer analyzer = buildCustomAnalyzer();

    private String indexDirPath;
    private IndexWriter indexWriter;
    private IndexWriterConfig config;
    private Directory directory;

    private ClassLoader classLoader = getClass().getClassLoader();

    /*
     * Pass the name and path of the new index. Path is relative to the
     * working directory.
     */
    public Indexer(String indexDirPath) {
        this.indexDirPath = indexDirPath;
        try {
            this.directory = FSDirectory.open(Paths.get(indexDirPath));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        this.config = new IndexWriterConfig(analyzer);
        this.config.setSimilarity(this.similarity);

        try {
            this.indexWriter = new IndexWriter(directory, config);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /*
     * Build index from list of paths to aggregated wikipedia article files.
     * Index writer is automatically closed.
     */
    public void buildIndex(List<String> filePaths) {
        for (String filePath : filePaths) {
            addWikiFile(filePath);
        }
        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // todo
    // write a fn that breaks up text with the analyzer, then makes lemmas with
    // CoreNLP, and then re-forms the text by joining with spaces. this function will
    // be used by the query parser too

    /*
     * Takes the processed file that contains many wikipedia articles. Breaks
     * down each file and adds each article as a separate document into the
     * index. Text is first turned into lemmas by CoreNLP before being passed
     * through the lucene whitespace analyzer.
     *
     * Put files in the resources folder.
     */
    public void addWikiFile(String filePath) {
        // break down with function mentioned above

        // will have issues is file path includes spaces
        try (Scanner scanner = new Scanner(new File(classLoader.getResource(filePath).getFile()))) {
            String articleContent;
            String title = "NO TITLE ASSIGNED";
            String categories = "";

            String line;
            List<String> lineLemmas;
            StringBuilder sb = new StringBuilder();

            while(scanner.hasNextLine()) {
                line = scanner.nextLine();
                lineLemmas = getLemmas(line);

                if (lineLemmas == null) {
                    // continue

                // if it's a title
                } else if (lineLemmas.size() > 2
                        && lineLemmas.get(0).equals("[")
                        && lineLemmas.get(1).equals("[")) {
                    // add the last article as a doc to the index
                    // fixme: what if there is filler text to start the file?
                    articleContent = sb.toString();
                    addDoc(title, articleContent, categories);

                    // start processing the next article
                    title = String.join(" ", lineLemmas);
                    sb = new StringBuilder();

                // if it's a categories line
                } else if (lineLemmas.size() > 0 && lineLemmas.get(0).equals("category")) {
                    categories = String.join(" ", lineLemmas.subList(1, lineLemmas.size()));

                // it's just a content line
                } else {
                    sb.append(String.join(" ", lineLemmas));
                }
            }
        } catch (IOException e) {
            // handle FileNotFoundException for File()
            // handle IOException from addDoc()
            e.printStackTrace();
        }
    }

    /*
     * Convert a string into a list of lemmas. If the input string is empty
     * or only spaces, returns null.
     */
    private List<String> getLemmas(String input) {
        if (input.trim().isEmpty())
            return null;
        Sentence s = new Sentence(input);
        return s.lemmas();
    }

    private void addDoc(String title, String content, String categories) throws IOException {
        Document doc = new Document();
        doc.add(new StringField("title", title, Field.Store.YES)); // fixme: text or string field?
        doc.add(new TextField("content", content, Field.Store.NO));
        doc.add(new TextField("categories", categories, Field.Store.NO));
        indexWriter.addDocument(doc);
    }

    public void close() throws IOException {
        indexWriter.close();
    }

    public String getIndexDirPath() {
        return indexDirPath;
    }

    public Directory getDirectory() {
        return directory;
    }

    public static Similarity getSimilarity() {
        return similarity;
    }

    /*
     * Builds a custom analyzer. Splits into tokens on whitespace. Removes
     * stop words. Sets lowercase.
     */
    public static Analyzer buildCustomAnalyzer() {
        Analyzer a = null;
        try {
            a = CustomAnalyzer.builder()
                    .withTokenizer("whitespace")
                    .addTokenFilter(RemoveSpecialFilterFactory.class, new HashMap<>())
                    .addTokenFilter("lowercase")
                    .addTokenFilter("stop")
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return a;
    }

    public static void main(String[] args ) {
        // todo: some tests to see how the coreMLP lemmas look on the wiki data example
        Indexer indexer = new Indexer("testIndex");
        indexer.addWikiFile("wiki-example.txt");
        try {
            indexer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //System.out.println(TokenizerFactory.availableTokenizers());
    }
}
