package com.mycompany.app;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;

import java.io.File;
import java.io.IOException;

/*
 * Main platform for Indexing and using the Answer Engine.
 *
 * Wiki data file folder should be expanded in the resources folder
 *
 */
public class AnswerPlatform {

    private AnswerEngine engine;

    public AnswerPlatform(String indexFilePath, String dataFolder, boolean write, boolean stem,
                          boolean lemmatize, boolean stopwords, Similarity s) {
        try {
            if (write) { // writing a new index
                Indexer indexer = new Indexer(indexFilePath, stem, lemmatize, stopwords, s);

                File dataFolderFile = new File("src/main/resources/" + dataFolder);
                File[] dataFileList = dataFolderFile.listFiles();
                assert dataFileList != null;
                for (File dataFile : dataFileList) {
                    indexer.addWikiFile(dataFile);
                }
                indexer.close();
            }
            this.engine = new AnswerEngine(indexFilePath, stem, lemmatize, stopwords, s);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public Query buildQuery(String content, String categories) {
        return engine.buildQuery(content, categories);
    }

    public TopDocs searchQuery(String query, int hits) {
        Query q1 = engine.buildQuery(query);
        return engine.searchIndex(q1, hits);
    }

    public TopDocs searchQuery(String content, String categories, int hits) {
        Query q1 = engine.buildQuery(content, categories);
        return engine.searchIndex(q1, hits);
    }

    public void printResults(TopDocs result) {
        try {
            System.out.println("total hits: " + result.totalHits);
            String printLine;
            for (ScoreDoc scoreDoc : result.scoreDocs) {
                printLine = engine.getReader().document(scoreDoc.doc).get("title");
                System.out.println(printLine.substring(2, printLine.length() - 2));
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public String getFirstResultTitle(ScoreDoc scoreDoc) {
        try {
            return engine.getReader().document(scoreDoc.doc).get("title");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /*
     * Returns the rank of the correct result from the order TopDocs passed.
     * Limited by how many hits are returned by the index search.
     */
    public int getRankCorrectResult(TopDocs topDocs, String expecting, int totalHits) {
        try {
            String result;
            int rank = 1;
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                result = engine.getReader().document(scoreDoc.doc).get("title");
                if (matchAnswers(expecting, result)) {
                    return rank;
                }
                rank++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return totalHits;
    }

    /*
     * Returns true if the strings match, false otherwise. case-insensitive.
     * The expected answer may include '|', indicating it may match either
     * option.
     */
    public static boolean matchAnswers(String expecting, String result) {
        String[] possibleAnswers = expecting.split("\\|");
        for (String ans : possibleAnswers) {
            if (ans.trim().toLowerCase().equals(result.substring(2, result.length() - 2).toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        String indexFilePath = "WikiIndex";
        String queryFilePath = "questions.txt";
        String dataFolder =  "wiki-subset-20140602";

        AnswerPlatform platform = new AnswerPlatform(indexFilePath, dataFolder,false, false, false,
                false, new BM25Similarity());
        //String query = "categories:arizona categories:politician";
        //String query = "The dominant paper in our nation's capital, it's among the top 10 U.S. papers in circulation";
        //String content = "Daniel Hertzberg & James B. Stewart of this paper shared a 1988 Pulitzer for their stories about insider trading";
        //String categories = "NEWSPAPERS";
        //String content = "Song that says, \"you make me smile with my heart; your looks are laughable, unphotographable\"";
        //String categories = "BROADWAY LYRICS";
        String content = "In 2009: Joker on film";
        String categories = "GOLDEN GLOBE WINNERS";
        Query q = platform.buildQuery(content, categories);
        System.out.println(q);
        TopDocs result = platform.searchQuery(content, categories, 20);
        //TopDocs result = platform.searchQuery(query, 20);
        platform.printResults(result);
    }
}
