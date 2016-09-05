package actionClasses;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import tools.ProcessResult;
import tools.TermProcessor;
import visualRecognition.AnalyzeImage;

public class UploadFile {
	static File imgFile;

	static String jsonObj;
	
	public String getJsonObj() {
		return jsonObj;
	}

	public void setJsonObj(String jsonObj) {
		this.jsonObj = jsonObj;
	}

	public static void main(String args[]) {

		imgFile = new File("C://tmp//eiffel.jpg");
		Map<String, Double> tags;
		List<Entry<String, Double>> tagsList = new ArrayList<Entry<String, Double>>();
		try {
			tags = AnalyzeImage.analyze(imgFile);
			Iterator itr = tags.keySet().iterator();
			while (itr.hasNext()) {
				String key = (String) itr.next();

				tagsList.add(new SimpleEntry<String, Double>(key, tags.get(key)));
			}
			System.out.println(tagsList.toString());
			System.out.println("Done processing");
			ProcessResult result = TermProcessor.processTerms(tagsList);
			System.out.println(result.getLikeRatio());
			System.out.println("Created tags");
			Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
			String json = gson.toJson(result);
			jsonObj = json;
			System.out.println("Converted into json");
			System.out.println(json);
		} catch (IOException | GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String pageLoad()
	{
		return "success";
	}
	
	public File getImgFile() {
		return imgFile;
	}

	public void setImgFile(File imgFile) {
		this.imgFile = imgFile;
	}

}
