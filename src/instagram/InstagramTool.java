package instagram;

import java.util.List;

import org.jinstagram.Instagram;
import org.jinstagram.auth.model.Token;
import org.jinstagram.entity.tags.TagInfoData;
import org.jinstagram.entity.tags.TagMediaFeed;
import org.jinstagram.entity.tags.TagSearchFeed;
import org.jinstagram.entity.users.basicinfo.UserInfo;
import org.jinstagram.entity.users.basicinfo.UserInfoData;
import org.jinstagram.entity.users.feed.MediaFeedData;
import org.jinstagram.exceptions.InstagramException;

public class InstagramTool {
	
	Instagram instagram;
	public InstagramTool(String accessToken, String clientSecret){
		Token token = new Token(accessToken, clientSecret);
		instagram = new Instagram(token);
	}
	
	public List<TagInfoData> searchTagsFromTerm(String term) throws InstagramException {
		TagSearchFeed searchFeed = instagram.searchTags(term);
		return searchFeed.getTagList();
	}
	
	public List<MediaFeedData> searchMediasFromTag(String tag) throws InstagramException {
		TagMediaFeed mediaFeed = instagram.getRecentMediaTags(tag);
		return mediaFeed.getData();
	}
	
	public UserInfoData searchUserFromId(String userId) throws InstagramException {
		UserInfo userInfo = instagram.getUserInfo(userId);
		return userInfo.getData();
	}
}