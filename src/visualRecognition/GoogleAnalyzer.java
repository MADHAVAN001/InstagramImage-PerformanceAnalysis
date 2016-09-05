package visualRecognition;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionScopes;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.common.collect.ImmutableList;

public class GoogleAnalyzer {
	static final String APPLICATION_NAME = "FbHackathon";
	private static final int MAX_RESULTS = 4;
	private static Vision vision;

	public Map<String, Double> visionAnalyzer(File mediaFile) throws IOException, GeneralSecurityException {
		vision = getVisionService();
		Map<String, Double> map = new HashMap<String, Double>();

		// Identify landmarks in the picture
		List<EntityAnnotation> landmarks = identifyLandmark(MAX_RESULTS, mediaFile);
		EntityAnnotation bestann = null;
		if (landmarks != null)
			for (EntityAnnotation annotation : landmarks) {

				if (bestann == null || (bestann != null && annotation.getScore() > bestann.getScore()))
					bestann = annotation;

				map.put(annotation.getDescription(), new Double(annotation.getScore()));

			}
		if (bestann != null) {
			JSONArray obj = new JSONArray(bestann.getLocations().toString());
			JSONObject latlng = obj.getJSONObject(0).getJSONObject("latLng");
			GeoCoding geocode = new GeoCoding(latlng.getDouble("latitude"), latlng.getDouble("longitude"));
			map.put(bestann.getDescription(), new Double(1));
			map.put(geocode.getCountry(), new Double(1));
			map.put(geocode.getCity(), new Double(1));
		}

		// Identify landmarks in the picture
		List<EntityAnnotation> labels = identifyLabel(MAX_RESULTS, mediaFile);

		if (labels != null)
			for (EntityAnnotation annotation : labels) {
				map.put(annotation.getDescription(), new Double(annotation.getScore()));
			}

		// Identify landmarks in the picture

		List<EntityAnnotation> logos = identifyLabel(MAX_RESULTS, mediaFile);

		if (logos != null)
			for (EntityAnnotation annotation : logos) {
				map.put(annotation.getDescription(), new Double(annotation.getScore()));

			}

		// Identify landmarks in the picture
		List<EntityAnnotation> text = identifyLabel(MAX_RESULTS, mediaFile);
		if (text != null)
			for (EntityAnnotation annotation : text) {
				map.put(annotation.getDescription(), new Double(annotation.getScore()));

			}
		return map;
	}

	public static Vision getVisionService() throws IOException, GeneralSecurityException {
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

		GoogleCredential credential = new GoogleCredential.Builder().setTransport(httpTransport)
				.setJsonFactory(jsonFactory).setServiceAccountId("analyzer@fbhackathon-142307.iam.gserviceaccount.com")
				.setServiceAccountScopes(VisionScopes.all())
				.setServiceAccountPrivateKeyFromP12File(new File("C:\\tmp\\FbHackathon-fcfb27cdc616.p12")).build();

		return new Vision.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, credential)
				.setApplicationName(APPLICATION_NAME).build();
	}

	/**
	 * Gets up to {@code maxResults} landmarks for an image stored at
	 * {@code uri}.
	 */
	public static List<EntityAnnotation> identifyLandmark(int maxResults, File mediaFile) throws IOException {
		AnnotateImageRequest request = new AnnotateImageRequest()
				.setImage(new Image().setContent(Base64.encodeBase64String(FileUtils.readFileToByteArray(mediaFile))))
				.setFeatures(ImmutableList.of(new Feature().setType("LANDMARK_DETECTION").setMaxResults(maxResults)));

		Vision.Images.Annotate annotate = vision.images()
				.annotate(new BatchAnnotateImagesRequest().setRequests(ImmutableList.of(request)));

		BatchAnnotateImagesResponse batchResponse = annotate.execute();
		assert batchResponse.getResponses().size() == 1;

		AnnotateImageResponse response = batchResponse.getResponses().get(0);

		return response.getLandmarkAnnotations();
	}

	public static List<EntityAnnotation> identifyLabel(int maxResults, File mediaFile) throws IOException {
		AnnotateImageRequest request = new AnnotateImageRequest()
				.setImage(new Image().setContent(Base64.encodeBase64String(FileUtils.readFileToByteArray(mediaFile))))
				.setFeatures(ImmutableList.of(new Feature().setType("LABEL_DETECTION").setMaxResults(maxResults)));

		Vision.Images.Annotate annotate = vision.images()
				.annotate(new BatchAnnotateImagesRequest().setRequests(ImmutableList.of(request)));

		BatchAnnotateImagesResponse batchResponse = annotate.execute();
		assert batchResponse.getResponses().size() == 1;

		AnnotateImageResponse response = batchResponse.getResponses().get(0);

		return response.getLabelAnnotations();
	}

	public static List<EntityAnnotation> identifyLogo(int maxResults, File mediaFile) throws IOException {
		AnnotateImageRequest request = new AnnotateImageRequest()
				.setImage(new Image().setContent(Base64.encodeBase64String(FileUtils.readFileToByteArray(mediaFile))))
				.setFeatures(ImmutableList.of(new Feature().setType("LOGO_DETECTION").setMaxResults(maxResults)));

		Vision.Images.Annotate annotate = vision.images()
				.annotate(new BatchAnnotateImagesRequest().setRequests(ImmutableList.of(request)));

		BatchAnnotateImagesResponse batchResponse = annotate.execute();
		assert batchResponse.getResponses().size() == 1;

		AnnotateImageResponse response = batchResponse.getResponses().get(0);

		return response.getLogoAnnotations();
	}

	public static List<EntityAnnotation> identifyText(int maxResults, File mediaFile) throws IOException {
		AnnotateImageRequest request = new AnnotateImageRequest()
				.setImage(new Image().setContent(Base64.encodeBase64String(FileUtils.readFileToByteArray(mediaFile))))
				.setFeatures(ImmutableList.of(new Feature().setType("TEXT_DETECTION").setMaxResults(maxResults)));

		Vision.Images.Annotate annotate = vision.images()
				.annotate(new BatchAnnotateImagesRequest().setRequests(ImmutableList.of(request)));

		BatchAnnotateImagesResponse batchResponse = annotate.execute();
		assert batchResponse.getResponses().size() == 1;

		AnnotateImageResponse response = batchResponse.getResponses().get(0);

		return response.getTextAnnotations();
	}

}
