package App.Logic;

import App.FileDataAccess;
import App.Model.Measurement;
import App.Modules.IndexModule;
import App.Modules.SearchModule;
import App.Modules.TestingModule;
import App.ParameterFileParser;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.RAMDirectory;

import java.util.*;

public class AssignmentLogic {

    private FileDataAccess _fileDataAccess;
    private ParameterFileParser _parameterFileParser;

    public AssignmentLogic() {
        _fileDataAccess = new FileDataAccess();
        _parameterFileParser = new ParameterFileParser(_fileDataAccess);
    }

    public AssignmentLogic(FileDataAccess fileDataAccess, ParameterFileParser parameterFileParser) {
        _fileDataAccess = fileDataAccess;
        _parameterFileParser = parameterFileParser;
    }

    public Map<String, List<String>> run(String parametersFileName) throws Exception {
        // Get all relevant parameters
        _parameterFileParser.LoadContent(parametersFileName);

        // Parse the documents
        Map<String, String> docs =
                _fileDataAccess.parseDocsFile(_parameterFileParser.getDocFiles());

        // Decide on Similarity equation for both index and search
        Similarity similarity = new ClassicSimilarity();

        // We first index the documents without taking stop words into account
        // so we could complete the assignment with stop words based on the documents
        RAMDirectory index = new RAMDirectory();
        IndexModule indexModule = new IndexModule(index, CharArraySet.EMPTY_SET, similarity);
        indexModule.indexDocs(docs);

        SearchModule searchModule = new SearchModule(index, similarity);
        List<String> stopWords = searchModule.getTopWords(20);

        // Reset objects and re-index using new stop words
        index = new RAMDirectory();
        indexModule = new IndexModule(index, CharArraySet.copy(new HashSet<>(stopWords)), similarity);
        searchModule = new SearchModule(index, similarity);
        indexModule.indexDocs(docs);

        // Parse queries
        Map<String, String> queries =
                _fileDataAccess.parseQueriesFile(_parameterFileParser.getQueryFile());

        Map<String, List<String>> results = new HashMap<>();

        for (String id : queries.keySet()) {
            String query = queries.get(id);
            // We set the size of the page as the total number of docs to avoid
            // paging over the result. Not optimal, but practically it's ok.
            List<String> queryResults = searchModule.queryDocs(query, docs.size());
            results.put(id, queryResults);
        }

        _fileDataAccess.writeResults(_parameterFileParser.getOutputFile(), results);

        String truthFilePath = _parameterFileParser.getTruthFile();
        if (truthFilePath != null) {
            Map truthContent = _fileDataAccess.parseTruthFile(truthFilePath);
            TestingModule tester = new TestingModule(truthContent, results);
            Measurement measurement = tester.TestQueries();
            System.out.println(measurement.GetAverageF());
        }

        return results;
    }
}
