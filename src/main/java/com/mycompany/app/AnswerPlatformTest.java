package com.mycompany.app;

import org.apache.lucene.search.TopDocs;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/*
 * Testing Jeopardy questions
 */
public class AnswerPlatformTest {

    public static final String QUERIES = "questions.txt"; // in the resources folder
    public static String INDEX = "PlainIndex"; // in the resources folder
    public static final String DATA = "wiki-subset-20140602"; // in the resources folder
    public static boolean WRITE = false; // do NOT accidentally overwrite the index
    public static boolean STEM = false;
    public static boolean LEMMATIZE = false;
    public static boolean STOPWORDS = true; // true if we keep stop words

    private AnswerPlatform platform;

    /*
     * run as:
     * java -jar ___ INDEX STEM LEMMATIZE STOPWORDS
     */
    public void main(String[] args) {
        INDEX = args[0];
        WRITE = Boolean.parseBoolean(args[1]);
        STEM = Boolean.parseBoolean(args[2]);
        LEMMATIZE = Boolean.parseBoolean(args[3]);
        STOPWORDS = Boolean.parseBoolean(args[4]);

        platform = new AnswerPlatform(INDEX, DATA, WRITE, STEM, LEMMATIZE, STOPWORDS);
        test_onAllQueries();
    }

    public void test_onAllQueries() {
        ClassLoader classLoader = getClass().getClassLoader();
        File queryFile = new File(classLoader.getResource(QUERIES).getFile());

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
                    //platform.printResults(results);
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
