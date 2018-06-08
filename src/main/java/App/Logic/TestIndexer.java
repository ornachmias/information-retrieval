package App.Logic;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TestIndexer {
    Directory corpus = new RAMDirectory();

    String analyzer = "vsm";
    boolean stopwords = false;
    boolean stemmer = true;

    public static void main(String[] args) throws IOException {
        List<String> docs = new ArrayList<String>();
        docs.add("This is a document");
        docs.add("Search for this document, it's longer and has more document.");
        docs.add("This one is longer still but it only has the one document contained within it's bounds.");

        TestIndexer indexer = new TestIndexer();
        IndexWriterConfig cf = indexer.index(docs);

        //Testing standard search (for "document")
        List<String> results = indexer.search("document", cf);
        System.out.println("Using Similarity: " + cf.getSimilarity().toString());
        for (String result : results) {
            System.out.println(result);
        }

        //Testing stopword search
//        results = indexer.search("this", cf);
//        for (String result : results) {
//            System.out.println(result);
//        }
    }

    public IndexWriterConfig index(List<String> docs) throws IOException
    {
        Analyzer analyz;
        IndexWriterConfig config;

        if (analyzer.equals("vsm") && stopwords && stemmer)
        {
            //VSM cosine similarity with TFIDF + stopwords + stemmer
            CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet();
            analyz = new EnglishAnalyzer(stopWords);
            config = new IndexWriterConfig(analyz);
            config.setSimilarity(new ClassicSimilarity());
        }
        else if (analyzer.equals("vsm") && !stopwords && stemmer)
        {
            //VSM cosine similarity with TFIDF - stopwords + stemmer
            analyz = new EnglishAnalyzer(CharArraySet.EMPTY_SET);
            config = new IndexWriterConfig(analyz);
            config.setSimilarity(new ClassicSimilarity());
        }
        else if (analyzer.equals("vsm") && stopwords && !stemmer)
        {
            //VSM cosine similarity with TFIDF - stopwords - stemmer
            CharArraySet stopWords = StandardAnalyzer.STOP_WORDS_SET;
            analyz = new StandardAnalyzer(stopWords);
            config = new IndexWriterConfig(analyz);
            config.setSimilarity(new ClassicSimilarity());
        }
        else if (analyzer.equals("bm25") && stopwords && stemmer)
        {
            //Analyzer + stopwords + stemmer
            CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet();
            analyz = new EnglishAnalyzer(stopWords);
            config = new IndexWriterConfig(analyz);
            //BM25 ranking method
            config.setSimilarity(new BM25Similarity());
        }
        else if (analyzer.equals("bm25") && !stopwords && stemmer)
        {
            //Analyzer - stopwords + stemmer
            analyz = new EnglishAnalyzer(CharArraySet.EMPTY_SET);
            config = new IndexWriterConfig(analyz);
            //BM25 ranking method
            config.setSimilarity(new BM25Similarity());
        }
        else if (analyzer.equals("bm25") && stopwords && !stemmer)
        {
            //Analyzer + stopwords - stemmer
            CharArraySet stopWords = StandardAnalyzer.STOP_WORDS_SET;
            analyz = new StandardAnalyzer(stopWords);
            config = new IndexWriterConfig(analyz);
            //BM25 ranking method
            config.setSimilarity(new BM25Similarity());
        }
        else
        {
            //some default
            analyz = new StandardAnalyzer();
            config = new IndexWriterConfig(analyz);
            config.setSimilarity(new ClassicSimilarity());
        }


        IndexWriter w = new IndexWriter(corpus, config);

        int doccount = 0;
        for (String doc1 : docs) {
            Document doc = new Document();
            doc.add(new TextField("title", "doc" + doccount, Field.Store.YES));
            doc.add(new TextField("text", doc1, Field.Store.YES));
            w.addDocument(doc);
            doccount++;
        }

        w.close();

        return config;
    }

    public List<String> search(String searchQuery, IndexWriterConfig cf) throws IOException {
        QueryParser qp = new QueryParser("text", cf.getAnalyzer());
        Query stemmedQuery = null;
        try {
            stemmedQuery = qp.parse(searchQuery);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        IndexReader reader = DirectoryReader.open(corpus);
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(cf.getSimilarity());

        TopDocs docs = searcher.search(stemmedQuery, 10);
        ScoreDoc[] scored = docs.scoreDocs;

        List<String> results = new LinkedList<String>();
        for (ScoreDoc aDoc : scored) {
            Document d = searcher.doc(aDoc.doc);
            results.add("+ " + d.get("title") + " | text: " + d.get("text") + " | score: " + aDoc.score);
        }
        return results;
    }
}