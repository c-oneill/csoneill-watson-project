package com.mycompany.app;

import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/*
 * Testing Jeopardy questions
 */
public class AnswerPlatformRun {

    public static final String QUERIES = "questions.txt"; // in the resources folder
    public static String INDEX = "PlainIndex"; // in the resources folder
    public static final String DATA = "wiki-subset-20140602"; // in the resources folder
    public static boolean WRITE = false; // do NOT accidentally overwrite the index
    public static boolean STEM = false;
    public static boolean LEMMATIZE = false;
    public static boolean STOPWORDS = true; // true if we keep stop words
    public static Similarity SIMILARITY = new LMJelinekMercerSimilarity(0.1F);
    //public static Similarity SIMILARITY = new BM25Similarity();
    //public static Similarity SIMILARITY = new ClassicSimilarity();

    private AnswerPlatform platform;

    /*
     * see README for usage. If not arguments are passed, default run is:
     *  stemming, no lemmatization, stopwords kept, Jelinek-Mercer language model (lambda = 0.1)
     */
    public static void main(String[] args) {
        AnswerPlatformRun platformRun = new AnswerPlatformRun();
        platformRun.initialize(args);
        platformRun.test_onAllQueries();
    }

    public void initialize(String[] args) {
        if (args.length == 5) {
            INDEX = args[0];
            STEM = Boolean.parseBoolean(args[1]);
            LEMMATIZE = Boolean.parseBoolean(args[2]);
            STOPWORDS = Boolean.parseBoolean(args[3]);
            if (args[4].equals("BM25")) {
                SIMILARITY = new BM25Similarity();
            } else if (args[4].equals("TFIDF")) {
                SIMILARITY = new ClassicSimilarity();
            }
        }
        platform = new AnswerPlatform(INDEX, DATA, WRITE, STEM, LEMMATIZE, STOPWORDS, SIMILARITY);
    }

    public void test_onAllQueries() {
        File queryFile = new File("src/main/resources/" + QUERIES);

        int queryCount = 0;
        int correctCount = 0;
        double inverseRankSum = 0;

        try (Scanner scanner = new Scanner(queryFile)) {
            String categories = "";
            String content = "";
            String answer = "";
            TopDocs results;
            int place = 0;
            String nextLine;

            double rank;
            int totalReturned = 10000;

            while (scanner.hasNextLine()) {
                nextLine = scanner.nextLine();
                if (place == 0)
                    categories = nextLine;
                else if (place == 1)
                    content = nextLine;
                else if (place == 2)
                    answer = nextLine;
                else if (place == 3) {
                    // skipping the blank line

                    results = platform.searchQuery(content, categories, totalReturned);
                    System.out.println("Question: " + content);
                    //System.out.println("My results:");
                    platform.printResults(results, 5);
                    System.out.println("Answer: " + answer);
                    rank = platform.getRankCorrectResult(results, answer, totalReturned);
                    if (rank == 1)
                        correctCount++;
                    inverseRankSum += 1 / rank;
                    System.out.println("Rank correct result: " + rank);

                    place = -1;
                    queryCount++;
                    System.out.println();
                }
                place++;
            }
            System.out.println("total queries: " + queryCount);
            System.out.println("total correct: " + correctCount);
            System.out.println("MRR: " + inverseRankSum / queryCount);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
    }
}
