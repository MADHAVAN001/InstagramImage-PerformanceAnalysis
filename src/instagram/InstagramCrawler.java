package instagram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import org.jinstagram.entity.common.Caption;
import org.jinstagram.entity.common.Likes;
import org.jinstagram.entity.common.User;
import org.jinstagram.entity.users.feed.MediaFeedData;

public class InstagramCrawler {

	private static final String TAG_URL = "https://www.instagram.com/explore/tags/";
	private static final String POST_URL = "https://www.instagram.com/p/";
	private static final String USER_URL = "https://www.instagram.com/";
	
	/*public UserInfoData searchUserFromId(String userId) throws InstagramException {
		UserInfo userInfo = instagram.getUserInfo(userId);
		return userInfo.getData();
	}*/
	
	public static ArrayList<MediaFeedData> searchMediasFromTag(String tag) throws IOException {
		String source = getSource(TAG_URL + tag);
		int startIndex = source.indexOf("\"top_posts\"");
		int endIndex = source.indexOf("\"environment_switcher_visible_server_guess\": true};</script>")
				+ "\"environment_switcher_visible_server_guess\": true}".length();
		source = source.substring(startIndex, endIndex);

		int likes = 0;
		String uploadId = "";
		String ownerId = "";
		String thumbnailSrc = "";
		String code = "";
		String[] splitted = source.split("\"");
		ArrayList<MediaFeedData> result = new ArrayList<>();
		MediaFeedData mfd = new MediaFeedData();
		for (int i = 0; i < splitted.length; i++) {
			if (splitted[i].equals("likes")) {
				likes = Integer.parseInt(splitted[i + 3].replaceAll("[^0-9]", ""));
				Likes l = new Likes();
				l.setCount(likes);
				mfd.setLikes(l);
			} else if (splitted[i].equals("code")) {
				code = splitted[i + 2];
				String postSource = getSource(POST_URL + code);
				int ownerIndex = postSource.indexOf("\"owner\": {\"username\": ");
				String username = postSource.substring(ownerIndex, ownerIndex + 100).split("\"")[5];
				String userSource = getSource(USER_URL + username);
				int followedByIndex = userSource.indexOf("followed_by");
				String followedBy = userSource.substring(followedByIndex, followedByIndex + 100).split("\"")[3].replaceAll("[^0-9]", "");
				
				mfd.setLink(followedBy);
			} else if (splitted[i].equals("owner")) {
				ownerId = splitted[i + 4];
				User user = new User();
				user.setId(ownerId);
				mfd.setUser(user);
			} else if(splitted[i].equals("thumbnail_src")) {
			    thumbnailSrc = splitted[i + 2];
			    Caption cap = new Caption();
			    cap.setText(thumbnailSrc);
			    mfd.setCaption(cap);
			} else if(splitted[i].equals("caption")) {
			    String cap = splitted[i + 2] + " ";
			    ArrayList<String> tags = new ArrayList<String>();
			    while(cap.contains("#")){
			    	cap = cap.substring(cap.indexOf("#") + 1);
			    	String hashtag = cap.substring(0, cap.indexOf(" "));
			    	tags.add(hashtag);
			    }
			    mfd.setTags(tags);
			}else if (splitted[i].equals("is_video")) {
				uploadId = splitted[i + 4];
				mfd.setId(uploadId);
				result.add(mfd);
				mfd = new MediaFeedData();
			}
		}
		return result;
	}

	private static String getSource(String weburl) throws IOException {
		URL url = new URL(weburl);
		InputStream is = url.openStream(); // throws an IOException
		BufferedReader br = new BufferedReader(new InputStreamReader(is));

		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString();
	}
}