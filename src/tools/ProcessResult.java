package tools;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ProcessResult {
	
	private final double likeRatio;
	private final List<Entry<String, Double>> tags;
	private final Map<String, List<String>> thumbnails; // Map: Tag -> List of thumbnail URLs
	
	public ProcessResult(double likeRatio, List<Entry<String, Double>> tags, Map<String, List<String>> thumbnails) {
		this.likeRatio = likeRatio;
		this.tags = tags;
		this.thumbnails = thumbnails;
	}

	public double getLikeRatio() {
		return likeRatio;
	}

	public List<Entry<String, Double>> getTags() {
		return tags;
	}

	public Map<String, List<String>> getThumbnails() {
		return thumbnails;
	}
	
}
