package com.mycompany.app;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.AnnotationPipeline;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.util.CoreMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/*
 * A class to build and save the index.
 *
 */
public class Indexer {

    private static Similarity similarity = new BM25Similarity();
    //private static Similarity similarity = new LMJelinekMercerSimilarity(0.1F);
    private Analyzer analyzer;

    private String indexDirPath;
    private IndexWriter indexWriter;
    private IndexWriterConfig config;
    private Directory directory;

    private final boolean stem;
    private final boolean lemmatize;
    private final boolean stopwords;

    private AnnotationPipeline pipeline;

    private ClassLoader classLoader = getClass().getClassLoader();

    /*
     * Pass the name and path of the new index. Path is relative to the
     * working directory.
     */
    public Indexer(String indexDirPath, boolean stem, boolean lemmatize, boolean stopwords) {
        this.stem = stem;
        this.lemmatize = lemmatize;
        this.stopwords = stopwords;

        this.analyzer = buildCustomAnalyzer(this.stem, this.stopwords);
        StanfordLemmatizer();
        this.indexDirPath = indexDirPath;
        try {
            this.directory = FSDirectory.open(Paths.get(indexDirPath));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        this.config = new IndexWriterConfig(analyzer);
        this.config.setSimilarity(this.similarity);
        this.config.setRAMBufferSizeMB(128); // default is 16?

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

    /*
     * Takes the processed file that contains many wikipedia articles. Breaks
     * down each file and adds each article as a separate document into the
     * index. Text is first turned into lemmas by CoreNLP before being passed
     * through the lucene whitespace analyzer.
     *
     * Put files in the resources folder.
     */
    public void addWikiFile(File file) {
        // break down with function mentioned above

        // will have issues is file path includes spaces
        try (Scanner scanner = new Scanner(file)) {
            String articleContent;
            String title = "NO TITLE ASSIGNED";
            String categories = "";

            String line;
            StringBuilder sb = new StringBuilder();

            while(scanner.hasNextLine()) {
                line = scanner.nextLine();

                // if it's a title
                if (line.length() > 2
                        && line.charAt(0) == '['
                        && line.charAt(1) == '['
                        && !line.startsWith("[[File:")) {
                    // add the last article as a doc to the index
                    // fixme: what if there is filler text to start the file?
                    articleContent = sb.toString();

                    if (lemmatize) {
                        List<String> articleLemmas = lemmatize(articleContent);
                        if (articleLemmas != null)
                            addDoc(title, String.join(" ", articleLemmas), categories);
                    } else {
                        addDoc(title, articleContent, categories);
                    }

                    // start processing the next article
                    title = line;
                    sb = new StringBuilder();

                    // if it's a categories line
                } else if (line.startsWith("CATEGORIES:")) {
                    if (lemmatize) {
                        List<String> catLemmas = lemmatize(line);
                        categories = String.join(" ", catLemmas.subList(1, catLemmas.size()));
                    } else {
                        categories = line.substring(11);
                    }

                    // it's just a content line
                } else {
                    sb.append(line);
                }
            }
        } catch (IOException e) {
            // handle FileNotFoundException for File()
            // handle IOException from addDoc()
            e.printStackTrace();
        }
    }

    /*
     * Pulled from: https://stackoverflow.com/questions/1578062/lemmatization-java
     */
    private void StanfordLemmatizer() {
        // Create StanfordCoreNLP object properties, with POS tagging
        // (required for lemmatization), and lemmatization
        Properties props;
        props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma");

        /*
         * This is a pipeline that takes in a string and returns various analyzed linguistic forms.
         * The String is tokenized via a tokenizer (such as PTBTokenizerAnnotator),
         * and then other sequence model style annotation can be used to add things like lemmas,
         * POS tags, and named entities. These are returned as a list of CoreLabels.
         * Other analysis components build and store parse trees, dependency graphs, etc.
         *
         * This class is designed to apply multiple Annotators to an Annotation.
         * The idea is that you first build up the pipeline by adding Annotators,
         * and then you take the objects you wish to annotate and pass them in and
         * get in return a fully annotated object.
         *
         *  StanfordCoreNLP loads a lot of models, so you probably
         *  only want to do this once per execution
         */
        this.pipeline = new StanfordCoreNLP(props);
    }

    /*
     * Pulled from: https://stackoverflow.com/questions/1578062/lemmatization-java
     */
    public List<String> lemmatize(String documentText)
    {
        List<String> lemmas = new LinkedList<String>();
        // Create an empty Annotation just with the given text
        Annotation document = new Annotation(documentText);
        // run all Annotators on this text
        this.pipeline.annotate(document);
        // Iterate over all of the sentences found
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for(CoreMap sentence: sentences) {
            // Iterate over all tokens in a sentence
            for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // Retrieve and add the lemma for each word into the
                // list of lemmas
                lemmas.add(token.get(CoreAnnotations.LemmaAnnotation.class));
            }
        }
        return lemmas;
    }

    public void addWikiFile(String filePath) {
        File file  = new File(classLoader.getResource(filePath).getFile());
        addWikiFile(file);
    }

    /*
     * Convert a string into a list of lemmas. If the input string is empty
     * or only spaces, returns null.
     */
    private List<String> getLemmas(String input) {
        try {
            if (emptySentence(input))
                return null;
            Sentence s = new Sentence(input);
            return s.lemmas();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            System.out.println("input: " + input);
            return null;
        }
    }

    private boolean emptySentence(String input) {
        for (int i = 0; i < input.length(); i++) {
            if (!Character.isWhitespace(input.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void addDoc(String title, String content, String categories) throws IOException {
        Document doc = new Document();
        doc.add(new StringField("title", title, Field.Store.YES));
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
     * Builds a custom analyzer. Splits into tokens on non-letters. Removes
     * stop words. Sets lowercase.
     */
    public static Analyzer buildCustomAnalyzer(boolean stem, boolean stopwords) {
        CustomAnalyzer.Builder a = null;
        try {
            a = CustomAnalyzer.builder()
                    .withTokenizer("standard") // combines LetterTokenizer and lowercase filter
                    //.addTokenFilter(RemoveSpecialFilterFactory.class, new HashMap<>())
                    .addTokenFilter("lowercase");
            if (!stopwords)
                a.addTokenFilter("stop");
            if (stem)
                a.addTokenFilter("porterStem");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return a.build();
    }

    public static void main(String[] args ) {
        Indexer indexer = new Indexer("testIndex2", false, false, false);
        indexer.addWikiFile("wiki-example.txt");
        try {
            indexer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
