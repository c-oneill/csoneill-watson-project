package com.mycompany.app;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.File;
import java.io.IOException;

/*
 * Main platform for Indexing and using the Answer Engine.
 *
 * usage: AnswerPlatform -q queryFilePath [-w writeIndexFilePath | -o openIndexFilePath]
 * Pulls queries from queryFilePath. If the -w, then write the index. If -o,
 * then open the index.
 *
 * Wiki data file folder should be expanded in the resources folder
 *
 */
public class AnswerPlatform {

    private AnswerEngine engine;
    private String queryFilePath;
    private boolean open; // true if open, false if write

    public AnswerPlatform(String indexFilePath, String queryFilePath, String dataFolder, boolean open) {
        this.queryFilePath = queryFilePath;
        try {
            if (!open) { // writing a new index
                Indexer indexer = new Indexer(indexFilePath);

                ClassLoader classLoader = getClass().getClassLoader();
                File dataFolderFile = new File(classLoader.getResource(dataFolder).getFile());
                File[] dataFileList = dataFolderFile.listFiles();
                for (File dataFile : dataFileList) {
                    indexer.addWikiFile(dataFile);
                }
                indexer.close();
            }
            this.engine = new AnswerEngine(indexFilePath);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public TopDocs searchQuery(String Query, int hits) {
        Query q1 = AnswerEngine.buildQuery("categories:arizona categories:politician");
        return engine.searchIndex(q1, hits);
    }

    public void printResults(TopDocs result) {
        try {
            System.out.println("total hits: " + result.totalHits);
            for (ScoreDoc scoreDoc : result.scoreDocs) {
                System.out.println(AnswerEngine.reanalyzeTitle(engine.getReader().document(scoreDoc.doc).get("title")));
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        // todo: code to loop through and process args
        String indexFilePath = "testIndex";
        String queryFilePath = "questions.txt";
        String dataFolder =  "wiki-subset-20140602";

        String query = "categories:arizona categories:politician";
        AnswerPlatform platform = new AnswerPlatform(indexFilePath, queryFilePath, dataFolder,false);
        TopDocs result = platform.searchQuery(query, 10);
        platform.printResults(result);
    }
}
