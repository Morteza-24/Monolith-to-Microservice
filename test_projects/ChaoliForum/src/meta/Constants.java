package com.daquexian.chaoli.forum.meta;

import android.content.res.Resources;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Constants
{
	public static final String APP_NAME = "chaoli";		// for shared preference
	public static final String APP_DIR_NAME = "ChaoLi";	// for directory where save attachments
	public static final int paddingLeft = 16;
	public static final int paddingTop = 16;
	public static final int paddingRight = 16;
	public static final int paddingBottom = 16;
	public static final int getNotificationInterval = 15;

	public static final int MAX_IMAGE_WIDTH = (int) (Resources.getSystem().getDisplayMetrics().widthPixels * 0.8);

	public static final String BASE_BASE_URL = "https://chaoli.club/";
	public static final String BASE_URL = BASE_BASE_URL + "index.php/";
	public static final String GET_TOKEN_URL = BASE_URL + "user/login.json";
	// index.php/member/name.json/我是大缺弦
	public static final String GET_USER_ID_URL = BASE_URL + "member/name.json/";
	public static final String LOGIN_URL = BASE_URL + "user/login";
	public static final String HOMEPAGE_URL = BASE_URL;
	public static final String LOGOUT_PRE_URL = BASE_URL + "user/logout?token=";
	public static final String GET_CAPTCHA_URL = BASE_URL + "mscaptcha";
	public static final String SIGN_UP_URL = BASE_URL + "user/join?invite=";
	public static final String GET_ALL_NOTIFICATIONS_URL = BASE_URL + "settings/notifications.json";
	// activity.json/32/2
	public static final String GET_ACTIVITIES_URL = BASE_URL + "member/activity.json/";
	// statistics.ajax/32
	public static final String GET_STATISTICS_URL = BASE_URL + "member/statistics.ajax/";
	// index.json/channelName?searchDetail
	// index.json/all?search=%23%E4%B8%8A%E9%99%90%EF%BC%9A0%20~%20100
	// index.json/chem?search=%23%E7%B2%BE%E5%93%81
	//public static final String conversationListURL = BASE_URL + "/conversations/index.json/";
	public static final String conversationListURL = BASE_URL + "conversations/index.json/";
	public static final String ATTACHMENT_IMAGE_URL = "https://dn-chaoli-upload.qbox.me/";
	// index.json/1430/p2
	public static final String postListURL = BASE_URL + "conversation/index.json/";
	public static final String loginURL = BASE_URL + "user/login";
	public static final String replyURL = BASE_URL + "?p=conversation/reply.ajax/";
	public static final String editURL = BASE_URL + "?p=conversation/editPost.ajax/";
	public static final String cancelEditURL = BASE_URL + "?p=attachment/removeSession/";
	public static final String notifyNewMsgURL = BASE_URL + "settings/notificationCheck/";
	public static final String avatarURL = "https://dn-chaoli-upload.qbox.me/";
	// .ajax/<conversationId>/<floor>&userId=<myId>&token=<token>
	public static final String preQuoteURL = BASE_URL + "?p=conversation/quotePost.json/";
	// .json/<postId>&userId=<myId>&token=<token>
	public static final String quoteURL = BASE_URL + "?p=conversation/reply.ajax/";
	public static final String deleteURL = BASE_URL + "?p=conversation/deletePost.ajax/";
	public static final String restoreURL = BASE_URL + "conversation/restorePost/";

	public static final String GET_QUESTION_URL = "https://chaoli.club/reg-exam/get-q.php?tags=";
	public static final String CONFIRM_ANSWER_URL = "https://chaoli.club/reg-exam/confirm.php";

	public static final String GET_PROFILE_URL = BASE_URL + "settings/general.json";
	public static final String CHECK_NOTIFICATION_URL = BASE_URL + "?p=settings/notificationCheck.ajax";
	public static final String UPDATE_URL = BASE_URL + "?p=conversations/update.ajax/all/";
	public static final String MODIFY_SETTINGS_URL = BASE_URL + "settings/general";

	public static final String GO_TO_POST_URL = BASE_URL + "conversation/post/";

	/* 给主题设置版块 */
	public static final String SET_CHANNEL_URL = BASE_URL + "?p=conversation/save.json/";
	/* 发表主题 */
	public static final String POST_CONVERSATION_URL = BASE_URL + "?p=conversation/start.ajax";
	/* 添加可见用户 */
	public static final String ADD_MEMBER_URL = BASE_URL + "?p=conversation/addMember.ajax/";
	/* 取消可见用户 */
	public static final String REMOVE_MEMBER_URL = BASE_URL + "?p=conversation/removeMember.ajax/";
	/* 获取可见用户列表 */
	public static final String GET_MEMBERS_ALLOWED_URL = BASE_URL + "";
	/* 隐藏主题 */
	public static final String IGNORE_CONVERSATION_URL = BASE_URL + "?p=conversation/ignore.ajax/";
	/* 关注主题 */
	public static final String STAR_CONVERSATION_URL = BASE_URL + "?p=conversation/star.json/";

	public static final String SETTINGS_SP = "settings";
	public static final String INVITING_CODE_SP = "icsp";
	public static final String NIGHT_MODE = "nightMode";
	public static final String CLICK_TWICE_TO_EXIT = "ctte";

	public static final String GLOBAL = "global";
	public static final String FIRST_ENTER_DEMO_MODE = "fedm";

	public static final String conversationSP = "conversationList";
	public static final String conversationSPKey = "listJSON";

	public static final String postSP = "postList";
	public static final String postSPKey = "listJSON";

	public static final String loginSP = "loginReturn";
	public static final String loginSPKey = "listJSON";
	public static final String loginBool = "logged";

	public static final String[] IMAGE_FILE_EXTENSION = {".jpg", ".png", ".gif", ".jpeg"};

	public static final String NONE = "none";

	public static final float MIN_EXPRESSION_ALPHA = 0.4f;

	public static final int POST_PER_PAGE = 20;

	public static final String[] iconStrs = new String[]{ " /:) " ,  " /:D " ,  " /^b^ " ,  " /o.o " ,  " /xx " ,  " /# " ,  " /)) " ,  " /-- " ,  " /TT " ,  " /== " ,
			" /.** " ,  " /:( " ,  " /vv " ,  " /$$ " ,  " /?? " ,  " /:/ " ,  " /xo " ,  " /o0 " ,  " />< " ,  " /love " ,
			" /... " ,  " /XD " ,  " /ii " ,  " /^^ " ,  " /<< " ,  " />. " ,  " /-_- " ,  " /0o0 " ,  " /zz " ,  " /O!O " ,
			" /## " ,  " /:O " ,  " /< " ,  " /heart " ,  " /break " ,  " /rose " ,  " /gift " ,  " /bow " ,  " /moon " ,  " /sun " ,
			" /coin " ,  " /bulb " ,  " /tea " ,  " /cake " ,  " /music " ,  " /rock " ,  " /v " ,  " /good " ,  " /bad " ,  " /ok " ,
			" /asnowwolf-smile " ,  " /asnowwolf-laugh " ,  " /asnowwolf-upset " ,  " /asnowwolf-tear " ,
			" /asnowwolf-worry " ,  " /asnowwolf-shock " ,  " /asnowwolf-amuse " };
	public static final String[] icons = new String[]{"1", "2", "3", "4",
			"5", "6", "7", "8", "9",
			"10", "11", "12", "13", "14",
			"15", "16", "17", "18", "19",
			"20", "21", "22", "23", "24",
			"25", "26", "27", "28", "29",
			"30", "31", "32", "33", "34",
			"35", "36", "37", "38", "39",
			"40", "41", "42", "43", "44",
			"45", "46", "47", "48", "49",
			"50", "asonwwolf_smile", "asonwwolf_laugh", "asonwwolf_upset", "asonwwolf_tear",
			"asonwwolf_worry", "asonwwolf_shock", "asonwwolf_amuse"};
}
