package visualRecognition;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AnalyzeImage {

	public static Map<String, Double> analyze(File mediaFile) throws IOException, GeneralSecurityException {
		Map<String, Double> tags = new HashMap<String, Double>();
		/*
		WatsonAnalyze analyzer = new WatsonAnalyze();
		Map<String, Double> watsonClassification = analyzer.watsonAnalyze(mediaFile);
		*/
		GoogleAnalyzer analyze = new GoogleAnalyzer();
		Map<String, Double> googleClassification = analyze.visionAnalyzer(mediaFile);
		/*
		Iterator<String> itr1 = watsonClassification.keySet().iterator();
		while (itr1.hasNext()) {
			String key = itr1.next();
			tags.put(key, watsonClassification.get(key));
		}
	*/
		Iterator<String> itr2 = googleClassification.keySet().iterator();
		while (itr2.hasNext()) {
			String key = itr2.next();
			tags.put(key, googleClassification.get(key));
		}

		
		return tags;
	}
}
