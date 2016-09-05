package visualRecognition;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.json.*;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassification;

public class WatsonAnalyze {
	private final VisualRecognition service;

	public WatsonAnalyze() {
		service = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);
		service.setApiKey("2a053641dd0c00fcc1cca32b6b3195e188d02e16");

	}

	public Map<String, Double> watsonAnalyze(File imageFile) {
		ClassifyImagesOptions options = new ClassifyImagesOptions.Builder().images(imageFile).build();
		VisualClassification result = service.classify(options).execute();
		Map<String, Double> map = new HashMap<String, Double>();
		if (result != null) {
			JSONObject obj = new JSONObject(result);
			JSONArray images = obj.getJSONArray("images");
			JSONArray classifiers = images.getJSONObject(0).getJSONArray("classifiers");
			JSONArray classes = classifiers.getJSONObject(0).getJSONArray("classes");
			for (int i = 0; i < classes.length(); i++) {
				Double score = Double.valueOf(classes.getJSONObject(i).getDouble("score"));
				map.put(classes.getJSONObject(i).getString("\"class\""), score);
			}
		}
		return map;
	}
}
