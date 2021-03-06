# csoneill-watson-project

Usage:
`maven clean package`
`java -jar target/csoneill-watson-project-1.0-SNAPSHOT-jar-with-dependencies.jar <INDEX> <STEM> <LEMMATIZE> <STOPWORDS> <SIMILARITY>`

alternatively use `target/csoneill-watson-project-1.0-SNAPSHOT-jar-with-dependencies.jar` without dependencies included.

The are several indices to choose from based on combinations of parameters: lemmatization, stemming, stopwords, similarity measure. The options are listed below with the command to run.

Indices:

1. PlainIndex

    no stemming, no lemmatization, stopwords kept, BM25 cosine similarity

    `java -jar target/csoneill-watson-project-1.0-SNAPSHOT-jar-with-dependencies.jar PlainIndex false false true BM25`

2. PlainNoStopIndex

    no stemming, no lemmatization, stopwords removed, BM25 cosine similarity

    `java -jar target/csoneill-watson-project-1.0-SNAPSHOT-jar-with-dependencies.jar PlainNoStopIndex false false false BM25`

3. StemIndex

    stemming, no lemmatization, stopwords kept, BM25 cosine similarity

    `java -jar target/csoneill-watson-project-1.0-SNAPSHOT-jar-with-dependencies.jar StemIndex true false true BM25`

4. StemNoStopIndex

    stemming, no lemmatization, stopwords removed, BM25 cosine similarity

    `java -jar target/csoneill-watson-project-1.0-SNAPSHOT-jar-with-dependencies.jar StemNoStopIndex true false false BM25`

5. JMStemIndex (Best Performance)

    stemming, no lemmatization, stopwords kept, Jelinek-Mercer language model (lambda = 0.1)

    `java -jar target/csoneill-watson-project-1.0-SNAPSHOT-jar-with-dependencies.jar JMStemIndex true false true JMLM`

6. TFIDFStemIndex

    stemming, no lemmatization, stopwords kept, TF-IDF similarity

    `java -jar target/csoneill-watson-project-1.0-SNAPSHOT-jar-with-dependencies.jar JMStemIndex true false true TFIDF`  

7. LemmaIndex

    no stemming, lemmatization, stopwords kept, BM25 cosine similarity

    `java -jar target/csoneill-watson-project-1.0-SNAPSHOT-jar-with-dependencies.jar LemmaIndex false true true BM25`

8. LemmaNoStopIndex

    no stemming, lemmatization, stopwords removed, BM25 cosine similarity

    `java -jar target/csoneill-watson-project-1.0-SNAPSHOT-jar-with-dependencies.jar LemmaNoStopIndex false true false BM25`



Classes:

Indexer
    
    Builds and writes the index to disk.

AnswerEngine

    Main logic behind search, query building, and CustomAnalyzer.

AnswerPlatform

    Brings Indexer and AnswerEngine together in one platform.

AnswerPlatformRun

    Parses and runs the provided Jeopardy clues on an Answer Platform. Calculates and prints the performance measurements.

RemoveSpecialFilter, RemoveSpecialFilterFactory

    Unused. Provides template for a custom token removal filter.






