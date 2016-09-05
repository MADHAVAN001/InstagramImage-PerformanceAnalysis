package tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jinstagram.entity.tags.TagInfoData;
import org.jinstagram.entity.users.basicinfo.UserInfoData;
import org.jinstagram.entity.users.feed.MediaFeedData;
import org.jinstagram.exceptions.InstagramException;

import instagram.InstagramCrawler;
import instagram.InstagramTool;

public class TermProcessor {

	public static ProcessResult processTerms(Map<String, Double> searchTerms) {
		return processTerms(new ArrayList<Entry<String, Double>>(searchTerms.entrySet()));
	}
	
	public static ProcessResult processTerms(List<Entry<String, Double>> searchTerms) {
		filterSearchTerms(searchTerms);
		TermProcessor processor = new TermProcessor(searchTerms);
		processor.processData();
		return new ProcessResult(processor.getLikeRatio(), processor.getRecommendedTags(), processor.getThumbnails());
	}

	private static final double SEARCH_TERM_THRESHOLD = 0.6;
	
	protected static void filterSearchTerms(List<Entry<String, Double>> searchTerms) {
		int index = 0;
		while (index < searchTerms.size()) {
			if (searchTerms.get(index).getValue() < SEARCH_TERM_THRESHOLD) {
				searchTerms.remove(index);
			} else {
				index++;
			}
		}
	}
	
	private static final int RETURN_TERM_TAGS_COUNT = 3;
	private static final int RETURN_MEDIA_COUNT = 9;
	private static final int RETURN_RECOMMENDED_TAGS_COUNT = 10;
	private static final int THUMBNAIL_COUNT = 5;
	
	private static final String INSTAGRAM_ACCESS_KEY = "1524647669.1e29c32.ed455a71a5574a1f8511acb2017f2a5d";
	private static final String INSTAGRAM_CLIENT_SECRET = "032e2187c52d4413a472715091966db9";
	
	private InstagramTool instaTool;
	// LIKE CALCULATIONS
	protected List<Entry<String, Double>> searchTerms;
	protected Map<String, List<String>> relatedTags; // Map: Search term -> List of tags
	protected Map<String, List<String>> tagMedias; // Map: Tag -> List of media IDs
	protected Map<String, Integer> mediaLikes; // Map: Media ID -> Likes
	protected Map<String, Integer> mediaFollowers; // Map: Media ID -> Uploader Followers
	protected Map<String, Double> tagLikeRatios; // Map: Tag -> Likes-to-Followers ratio
	protected Map<String, Double> termLikeRatios; // Map: Search term -> Likes-to-Followers ratio
	protected double likeRatio;
	
	// TAG CALCULATIONS
	protected Map<String, List<String>> mediaTags; // Map: Media ID -> List of tags
	protected double minMediaLikeRatio;
	protected double maxMediaLikeRatio;
	protected Map<String, Entry<Double, Integer>> tagScores; // Map: Tag -> Score
	protected List<Entry<String, Double>> recommendedTags;
	
	// THUMBNAIL CACHING
	protected Map<String, List<String>> thumbnails; // Map: Tag -> List of thumbnail URLs
	
	private boolean hasProcessed = false;

	protected TermProcessor(List<Entry<String, Double>> searchTerms) {
		initialize(searchTerms);
	}
	
	public void processData() {
		// Likes Processor
		processTerms();
		processTags();
		processTagsLikeRatio();
		processTermsLikeRatio();
		processLikeRatio();
		// Tags Processor
		processTagsScores();
		processRecommendedTags();
		hasProcessed = true;
	}
	
	public boolean isProcessed() {
		return hasProcessed;
	}
	
	public double getLikeRatio() {
		if (!hasProcessed) {
			// throw new Exception("Analytics not processed.");
			return -1;
		}
		return likeRatio;
	}

	public List<Entry<String, Double>> getRecommendedTags() {
		return recommendedTags;
	}

	public Map<String, List<String>> getThumbnails() {
		return thumbnails;
	}

	private void initialize(List<Entry<String, Double>> searchTerms) {
		this.instaTool = new InstagramTool(INSTAGRAM_ACCESS_KEY, INSTAGRAM_CLIENT_SECRET);
		this.searchTerms = Collections.unmodifiableList(searchTerms);
		this.relatedTags = new HashMap<>();
		this.tagMedias = new ConcurrentHashMap<>();
		this.mediaLikes = new ConcurrentHashMap<>();
		this.mediaFollowers = new ConcurrentHashMap<>();
		this.tagLikeRatios = new HashMap<>();
		this.termLikeRatios = new HashMap<>();
		this.mediaTags = new ConcurrentHashMap<>();
		this.minMediaLikeRatio = Double.MAX_VALUE;
		this.maxMediaLikeRatio = Double.MIN_VALUE;
		this.tagScores = new HashMap<>();
		this.recommendedTags = new ArrayList<>();
		this.thumbnails = new ConcurrentHashMap<>();
	}

	// takes search terms (searchTerms) and adds their corresponding tags (relatedTags)
	private void processTerms() {
		for (Entry<String, Double> term : searchTerms) {
			final Entry<String, Double> finalTerm = term;
			new TryDelegate() {
				@Override
				public void tryExecute() throws Exception {
					relatedTags.put(finalTerm.getKey(), searchTagsFromTerm(finalTerm.getKey()));
				}
			}.execute();
		}
	}
	
	private void processTags() {
		List<String> tags = new ArrayList<String>();
		for (List<String> termTags : relatedTags.values()) {
			for (String tag : termTags) {
				tags.add(tag);
			}
		}
		Thread t[] = new Thread[tags.size()];
		for (int i = 0; i < tags.size(); i++) {
			final String tag = tags.get(i);
			t[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					processTagMedia(tag);
				}
			}, "");
			t[i].start();
		}
		for (int i = 0; i < tags.size(); i++) {
			try {
				t[i].join();
			} catch (InterruptedException e) {
			}
		}
	}

	private void processTagMedia(String tag) {
		final String finalTag = tag;
		new TryDelegate() {
			@Override
			public void tryExecute() throws Exception {
				List<MediaFeedData> searchResult = searchMediasFromTag(finalTag);
				Collections.sort(searchResult, new Comparator<MediaFeedData>() {
					@Override
					public int compare(MediaFeedData a, MediaFeedData b) {
						return Long.compare(b.getLikes().getCount(), a.getLikes().getCount());
					}
				});
				List<String> mediaIds = new ArrayList<>();
				for (int i = 0; i < Math.min(RETURN_MEDIA_COUNT, searchResult.size()); i++) {
					MediaFeedData curMedia = searchResult.get(i);
					String mediaId = curMedia.getId();
					mediaIds.add(mediaId);
					mediaLikes.put(mediaId, curMedia.getLikes().getCount());
					//mediaUploaderIds.put(mediaId, curMedia.getUser().getId());
					mediaFollowers.put(mediaId, Integer.parseInt(curMedia.getLink())); // Really stores follower count of uploader >:D
					mediaTags.put(mediaId, curMedia.getTags());
				}
				tagMedias.put(finalTag, mediaIds);
				List<String> tagThumbnails = new ArrayList<>(THUMBNAIL_COUNT);
				for (int i = 0; i < searchResult.size() && tagThumbnails.size() < THUMBNAIL_COUNT; i++) {
					MediaFeedData curMedia = searchResult.get(i);
					tagThumbnails.add(curMedia.getCaption().getText()); // Really stores thumbnail URL >:D
				}
				thumbnails.put(finalTag, tagThumbnails);
			}
		}.execute();
	}
	
	private void processTagsLikeRatio() {
		for (String tag : tagMedias.keySet()) {
			processTagLikeRatio(tag);
		}
	}
	
	private void processTagLikeRatio(String tag) {
		List<String> mediaIds = tagMedias.get(tag);
		int totalMediaLikes = 0;
		int totalMediaFollowers = 0;
		for (String mediaId : mediaIds) {
			if (!mediaLikes.containsKey(mediaId)) {
				continue; // media likes not found
			} else if (!mediaFollowers.containsKey(mediaId)) {
				continue; // media uploader's followers not found
			}
			int curMediaLikes = mediaLikes.get(mediaId);
			int curMediaFollowers = mediaFollowers.get(mediaId);
			totalMediaLikes += curMediaLikes;
			totalMediaFollowers += curMediaFollowers;
			double mediaLikeRatio = 0;
			if (curMediaFollowers > 0) {
				mediaLikeRatio = (double)curMediaLikes / curMediaFollowers;
			}
			this.minMediaLikeRatio = Math.min(mediaLikeRatio, this.minMediaLikeRatio);
			this.maxMediaLikeRatio = Math.max(mediaLikeRatio, this.maxMediaLikeRatio);
		}
		double tagLikeRatio = 0;
		if (totalMediaFollowers > 0) {
			tagLikeRatio = (double)totalMediaLikes / totalMediaFollowers;
		}
		tagLikeRatios.put(tag, tagLikeRatio);
	}
	
	private void processTermsLikeRatio() {
		for (String term : relatedTags.keySet()) {
			processTermLikeRatio(term);
		}
	}
	
	private void processTermLikeRatio(String term) {
		List<String> tags = relatedTags.get(term);
		double totalRatio = 0;
		int ratioCount = 0;
		for (String tag : tags) {
			if (!tagLikeRatios.containsKey(tag)) {
				continue;
			}
			totalRatio += tagLikeRatios.get(tag);
			ratioCount += 1;
		}
		double ratio = 0;
		if (ratioCount > 0) {
			ratio = totalRatio / ratioCount;
		}
		termLikeRatios.put(term, ratio);
	}
	
	private void processLikeRatio() {
		double totalWeightedRatio = 0;
		double totalWeight = 0;
		for (Entry<String, Double> term : searchTerms) {
			if (!termLikeRatios.containsKey(term.getKey())) {
				continue;
			}
			double termRatio = termLikeRatios.get(term.getKey());
			totalWeightedRatio += termRatio * term.getValue(); // weighted average
			totalWeight += term.getValue();
		}
		likeRatio = 0;
		if (totalWeight > 0) {
			likeRatio = totalWeightedRatio / totalWeight;
		}
	}
	
	private void processTagsScores() {
		for (Entry<String, Double> searchTerm : searchTerms) {
			String term = searchTerm.getKey();
			double termConfidence = searchTerm.getValue();
			if (!relatedTags.containsKey(term)) {
				continue;
			}
			List<String> tags = relatedTags.get(term);
			for (String tag : tags) {
				processTagScore(tag, termConfidence);
			}
		}
	}
	
	private void processTagScore(String tag, double termConfidence) {
		if (!tagMedias.containsKey(tag)) {
			return;
		}
		List<String> mediaIds = tagMedias.get(tag);
		for (String mediaId : mediaIds) {
			 processMediaTag(mediaId, termConfidence);
		}
	}
	
	private void processMediaTag(String mediaId, double termConfidence) {
		if (!mediaLikes.containsKey(mediaId)) {
			return; // media likes not found
		} else if (!mediaFollowers.containsKey(mediaId)) {
			return; // media uploader's followers not found
		} else if (!mediaTags.containsKey(mediaId)) {
			return; // media tags not found
		}
		double mediaLikeRatio = 0;
		int followers = mediaFollowers.get(mediaId);
		if (followers > 0) {
			mediaLikeRatio = (double)mediaLikes.get(mediaId) / followers;
		}
		double mediaPopularity = 1;
		if (this.maxMediaLikeRatio > this.minMediaLikeRatio) {
			mediaPopularity = 0.5 + ((mediaLikeRatio - this.minMediaLikeRatio) * 0.5 / (this.maxMediaLikeRatio - this.minMediaLikeRatio));
		}
		double tagScore = termConfidence * mediaPopularity;
		List<String> tags = mediaTags.get(mediaId);
		for (String tag : tags) {
			double score = 0;
			int freq = 0;
			if (tagScores.containsKey(tag)) {
				Entry<Double, Integer> info = tagScores.get(tag);
				score = info.getKey();
				freq = info.getValue();
			}
			score += tagScore;
			freq++;
			tagScores.put(tag, new SimpleEntry<Double, Integer>(score, freq));
		}
	}
	
	private void processRecommendedTags() {
		List<Entry<String, Double>> tagInfos = new ArrayList<>();
		for (Entry<String, Entry<Double, Integer>> tagScore : tagScores.entrySet()) {
			String tag = tagScore.getKey();
			double score = 0;
			if (tagScore.getValue().getValue() > 0) {
				score = tagScore.getValue().getKey() / tagScore.getValue().getValue();
			}
			tagInfos.add(new SimpleEntry<String, Double>(tag, score));
		}
		Collections.sort(tagInfos, new Comparator<Entry<String, Double>>() {
			@Override
			public int compare(Entry<String, Double> a, Entry<String, Double> b) {
				return Double.compare(b.getValue(), a.getValue());
			}
		});
		recommendedTags.clear();
		for (int i = 0; i < Math.min(RETURN_RECOMMENDED_TAGS_COUNT, tagInfos.size()); i++) {
			recommendedTags.add(new SimpleEntry<String, Double>(tagInfos.get(i)));
		}
	}
	
	// Returns top 3 popular tags
	protected List<String> searchTagsFromTerm(String term) throws InstagramException {
		List<TagInfoData> searchResult = instaTool.searchTagsFromTerm(term);
		Collections.sort(searchResult, new Comparator<TagInfoData>() {
			@Override
			public int compare(TagInfoData a, TagInfoData b) {
				return Long.compare(b.getMediaCount(), a.getMediaCount());
			}
		});
		List<String> result = new ArrayList<>();
		for (int i = 0; i < Math.min(RETURN_TERM_TAGS_COUNT, searchResult.size()); i++) {
			result.add(searchResult.get(i).getTagName());
		}
		return result;
	}

	// Returns the top 9 images' likes + userId for a specified tag
	protected List<MediaFeedData> searchMediasFromTag(String tag) throws InstagramException, IOException {
		// List<MediaFeedData> searchResult = instaTool.searchMediasFromTag(tag);
		List<MediaFeedData> searchResult = InstagramCrawler.searchMediasFromTag(tag);
		return searchResult;
	}
	
	// Returns user data from userId
	protected UserInfoData searchUserFromId(String userId) throws InstagramException {
		return instaTool.searchUserFromId(userId);
	}
	
	// ===== DEBUG FUNCTIONS =====
	/*
	public static void main(String[] args) {
		List<Entry<String, Double>> searchTerms = new ArrayList<Entry<String, Double>>(TermProcessorTest.initializeSearchTerms().entrySet());
		filterSearchTerms(searchTerms);
		TermProcessor processor = new TermProcessor(searchTerms) {
			@Override
			protected List<String> searchTagsFromTerm(String term) throws InstagramException {
				if (!TermProcessorTest.getTermTags().containsKey(term)) {
					System.out.println("Cannot find term data: " + term);
					throw new InstagramException("Cannot find term data: " + term);
				}
				return TermProcessorTest.getTermTags().get(term);
			}

			@Override
			protected List<MediaFeedData> searchMediasFromTag(String tag) throws InstagramException, IOException {
				if (!TermProcessorTest.getTagMedias().containsKey(tag)) {
					System.out.println("Cannot find tag data: " + tag);
					throw new IOException("Cannot find tag data: " + tag);
				}
				return TermProcessorTest.getTagMedias().get(tag);
			}
		};
		processor.processData();
		processor.exportData();
		ProcessResult result = new ProcessResult(processor.getLikeRatio(), processor.getRecommendedTags(), processor.getThumbnails());
		System.out.println(result.toString());
		System.out.println("Test done.");
	}*/

	/*
	public static void main(String[] args) {
		List<Entry<String, Double>> searchTerms = new ArrayList<Entry<String, Double>>();
		searchTerms.add(new SimpleEntry<String, Double>("watercraft rowing", 0.8920196294784546));
		searchTerms.add(new SimpleEntry<String, Double>("waterway", 0.8859228491783142));
		searchTerms.add(new SimpleEntry<String, Double>("Italy", 1.0));
		searchTerms.add(new SimpleEntry<String, Double>("vacation", 0.9397507309913635));
		searchTerms.add(new SimpleEntry<String, Double>("boating", 0.8926211595535278));
		searchTerms.add(new SimpleEntry<String, Double>("Grand Canal", 1.0));
		searchTerms.add(new SimpleEntry<String, Double>("Venice", 1.0));
		filterSearchTerms(searchTerms);
		TermProcessor processor = new TermProcessor(searchTerms);
		processor.processData();
		processor.exportData();
		ProcessResult result = new ProcessResult(processor.getLikeRatio(), processor.getRecommendedTags(), processor.getThumbnails());
		System.out.println(result.toString());
		System.out.println("Test done.");
	}*/
	
	private void exportData() {
		try {
			PrintWriter out = new PrintWriter("out.txt");
			List<String> keys;
			out.println(" ===== Like Ratios ===== ");
			out.println("Like Ratio : \t" + likeRatio);
			out.println(" ===== Term Ratios ===== ");
			keys = new ArrayList<>(termLikeRatios.keySet());
			Collections.sort(keys);
			for (String s : keys) {
				out.println(s + " : \t" + termLikeRatios.get(s).toString());
			}
			out.println(" ===== Tag Ratios ===== ");
			keys = new ArrayList<>(tagLikeRatios.keySet());
			Collections.sort(keys);
			for (String s : keys) {
				out.println(s + " : \t" + tagLikeRatios.get(s).toString());
			}
			out.println(" ===== Tag Scores ===== ");
			List<Entry<String, Double>> tagInfos = new ArrayList<>();
			for (Entry<String, Entry<Double, Integer>> tagScore : tagScores.entrySet()) {
				String tag = tagScore.getKey();
				double score = 0;
				if (tagScore.getValue().getValue() > 0) {
					score = tagScore.getValue().getKey() / tagScore.getValue().getValue();
				}
				tagInfos.add(new SimpleEntry<String, Double>(tag, score));
			}
			Collections.sort(tagInfos, new Comparator<Entry<String, Double>>() {
				@Override
				public int compare(Entry<String, Double> a, Entry<String, Double> b) {
					return Double.compare(b.getValue(), a.getValue());
				}
			});
			for (Entry<String, Double> tagInfo : tagInfos) {
				out.println(tagInfo.getKey() + " : \t" + tagInfo.getValue());
			}
			out.close();
		} catch (FileNotFoundException e) {
			System.out.println("Unable to save to file:");
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private static class TryDelegate {
		private static final int FAILURE_RETRIES = 2;
		
		public boolean execute() {
			for (int i = 0; i <= FAILURE_RETRIES; i++) {
				try {
					tryExecute();
					return true;
				} catch (Exception ex) {
				}
			}
			return false;
		}
		
		public void tryExecute() throws Exception {
		}
	}
}