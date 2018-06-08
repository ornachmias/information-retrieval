package App.Logic;

import App.Model.IRetrivalAlgorithm;
import App.Model.RetrievalAlgorithmType;
import App.Model.Threshold.BasicThreashold;
import App.AlgImpl.RetrivalAlgorithmFactory;
import App.Modules.SearchModule;

import java.util.*;

public class RunConstantThresholds extends AssignmentLogic {

    public Map<String, List<String>> run(String parametersFileName) throws Exception {
        // Get all relevant parameters
        _parameterFileParser.LoadContent(parametersFileName);
        RetrievalAlgorithmType alg_type = _parameterFileParser.getRetrievalAlgorithm();
        IRetrivalAlgorithm alg = RetrivalAlgorithmFactory.GetAlg(alg_type);

        // Parse the documents
        Map<String, String> docs = _fileDataAccess.parseDocsFile(_parameterFileParser.getDocFiles());

        List<String> stopWords = GetStopWards(docs);
        SearchModule searchModule = IndexDocs(docs, alg, stopWords);

        // Parse queries
        Map<String, String> queries = _fileDataAccess.parseQueriesFile(_parameterFileParser.getQueryFile());

        float threshold = (float) 0.1;
        Map<String, List<String>> results = new HashMap<>();
        while (threshold < 2) {
            alg.setThreshold(new BasicThreashold(threshold));
            results = GetResults(docs, queries, searchModule);
            _fileDataAccess.writeResults(_parameterFileParser.getOutputFile(), results);
            MeasureResults(results);
            threshold += 0.01;
        }

        return results;
    }
}