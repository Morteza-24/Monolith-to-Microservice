package com.daquexian.chaoli.forum.meta;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;

import com.daquexian.chaoli.forum.utils.MyUtils;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.model.Post;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SFXParser3 {
	// Finally, I decide to use this way.

	private static final String TAG = "SFXParser3";

	private static final String[] iconStrs = new String[]{"/:)", "/:D", "/^b^", "/o.o", "/xx", "/#", "/))", "/--", "/TT", "/==",
			"/.**", "/:(", "/vv", "/$$", "/??", "/:/", "/xo", "/o0", "/><", "/love",
			"/...", "/XD", "/ii", "/^^", "/<<", "/>.", "/-_-", "/0o0", "/zz", "/O!O",
			"/##", "/:O", "/<", "/heart", "/break", "/rose", "/gift", "/bow", "/moon", "/sun",
			"/coin", "/bulb", "/tea", "/cake", "/music", "/rock", "/v", "/good", "/bad", "/ok",
			"/asnowwolf-smile", "/asnowwolf-laugh", "/asnowwolf-upset", "/asnowwolf-tear",
			"/asnowwolf-worry", "/asnowwolf-shock", "/asnowwolf-amuse"};
	private static final int[] icons = new int[]{R.drawable.emoticons__0050_1, R.drawable.emoticons__0049_2, R.drawable.emoticons__0048_3, R.drawable.emoticons__0047_4,
			R.drawable.emoticons__0046_5, R.drawable.emoticons__0045_6, R.drawable.emoticons__0044_7, R.drawable.emoticons__0043_8, R.drawable.emoticons__0042_9,
			R.drawable.emoticons__0041_10, R.drawable.emoticons__0040_11, R.drawable.emoticons__0039_12, R.drawable.emoticons__0038_13, R.drawable.emoticons__0037_14,
			R.drawable.emoticons__0036_15, R.drawable.emoticons__0035_16, R.drawable.emoticons__0034_17, R.drawable.emoticons__0033_18, R.drawable.emoticons__0032_19,
			R.drawable.emoticons__0031_20, R.drawable.emoticons__0030_21, R.drawable.emoticons__0029_22, R.drawable.emoticons__0028_23, R.drawable.emoticons__0027_24,
			R.drawable.emoticons__0026_25, R.drawable.emoticons__0025_26, R.drawable.emoticons__0024_27, R.drawable.emoticons__0023_28, R.drawable.emoticons__0022_29,
			R.drawable.emoticons__0021_30, R.drawable.emoticons__0020_31, R.drawable.emoticons__0019_32, R.drawable.emoticons__0018_33, R.drawable.emoticons__0017_34,
			R.drawable.emoticons__0016_35, R.drawable.emoticons__0015_36, R.drawable.emoticons__0014_37, R.drawable.emoticons__0013_38, R.drawable.emoticons__0012_39,
			R.drawable.emoticons__0011_40, R.drawable.emoticons__0010_41, R.drawable.emoticons__0009_42, R.drawable.emoticons__0008_43, R.drawable.emoticons__0007_44,
			R.drawable.emoticons__0006_45, R.drawable.emoticons__0005_46, R.drawable.emoticons__0004_47, R.drawable.emoticons__0003_48, R.drawable.emoticons__0002_49,
			R.drawable.emoticons__0001_50, R.drawable.asonwwolf_smile, R.drawable.asonwwolf_laugh, R.drawable.asonwwolf_upset, R.drawable.asonwwolf_tear,
			R.drawable.asonwwolf_worry, R.drawable.asonwwolf_shock, R.drawable.asonwwolf_amuse};

	private static String[] TAGS = {"(?i)\\[center]", "(?i)\\[/center]", "(?i)\\[h]", "(?i)\\[/h]", "(?i)\\[s]", "(?i)\\[/s]", "(?i)\\[b]", "(?i)\\[/b]",
			"(?i)\\[i]", "(?i)\\[/i]", "(?i)\\[c=.{3,6}]", "(?i)\\[/c]", "(?i)\\[curtain]", "(?i)\\[/curtain]"};

	/**
	 * remove remaining tags (they indicate resolve error)
	 * plan to refactor the code to avoid these error
     */
	public static SpannableStringBuilder removeTags(SpannableStringBuilder builder) {
		for (String tag : TAGS) {
			Pattern pattern = Pattern.compile(tag);
			Matcher matcher = pattern.matcher(builder);
			while (matcher.find()) {
				builder.replace(matcher.start(), matcher.end(), "");
				matcher = pattern.matcher(builder);
			}
		}
		return builder;
	}

	public static SpannableStringBuilder parse(final Context context, SpannableStringBuilder builder, List<Post.Attachment> attachmentList) {
		Pattern cPattern = Pattern.compile("(?i)\\[c=(.*?)]((.|\n)*?)\\[/c]");
		Matcher c = cPattern.matcher(builder);
		while (c.find()) {
			try {
				int color = Color.parseColor(c.group(1));
				builder.setSpan(new ForegroundColorSpan(color), c.start(), c.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

				builder.replace(c.end(2), c.end(), "");
				builder.replace(c.start(), c.start(2), "");
				c = cPattern.matcher(builder);
			} catch (IllegalArgumentException e) {
				//避免不支持的颜色引起crash
			}
		}

		cPattern = Pattern.compile("(?i)\\[color=(.*?)]((.|\n)*?)\\[/color]");
		c = cPattern.matcher(builder);
		while (c.find()) {
			try {
				int color = Color.parseColor(c.group(1));
				builder.setSpan(new ForegroundColorSpan(color), c.start(), c.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

				builder.replace(c.end(2), c.end(), "");
				builder.replace(c.start(), c.start(2), "");
				c = cPattern.matcher(builder);
			} catch (IllegalArgumentException e) {
				//避免不支持的颜色引起crash
			}
		}

		Pattern urlPattern = Pattern.compile("(?i)\\[url=(.*?)](.*?)\\[/url]");
		Matcher url = urlPattern.matcher(builder);
		while (url.find()) {
			String site = url.group(1).toLowerCase();
			if (!site.startsWith("http://") && !site.startsWith("https://")) {
				site = "http://" + site;
			}

			final String finalSite = site;
			builder.setSpan(new ClickableSpan() {
				@Override
				public void onClick(View widget) {
					context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(finalSite)));
				}
			}, url.start(2), url.end(2), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			builder.replace(url.end(2), url.end(), "");
			builder.replace(url.start(), url.start(2), "");
			url = urlPattern.matcher(builder);
		}

		Pattern curtainPattern = Pattern.compile("(?i)(?<=\\[curtain\\])((.|\\n)+?)(?=\\[/curtain\\])");
		Matcher curtain = curtainPattern.matcher(builder);
		while (curtain.find()) {
			builder.setSpan(new BackgroundColorSpan(Color.DKGRAY), curtain.start(), curtain.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
//			spannable.setSpan(new Touchable, curtain.start(), curtain.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			builder.replace(curtain.end(), curtain.end() + 10, "");
			builder.replace(curtain.start() - 9, curtain.start(), "");
			curtain = Pattern.compile("(?i)(?<=\\[curtain\\])(.+?)(?=\\[/curtain\\])").matcher(builder);
		}

		Pattern bPattern = Pattern.compile("(?i)\\[b]((.|\\n)+?)\\[/b]");
		Matcher b = bPattern.matcher(builder);
		while (b.find()) {
			builder.setSpan(new StyleSpan(Typeface.BOLD), b.start(1), b.end(1), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			builder.replace(b.end(1), b.end(), "");
			builder.replace(b.start(), b.start(1), "");
			b = bPattern.matcher(builder);
		}

		Pattern iPattern = Pattern.compile("(?i)(?<=\\[i\\])((.|\\n)+?)(?=\\[/i\\])");
		Matcher i = iPattern.matcher(builder);
		while (i.find()) {
			builder.setSpan(new StyleSpan(Typeface.ITALIC), i.start(), i.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			builder.replace(i.end(), i.end() + 4, "");
			builder.replace(i.start() - 3, i.start(), "");
			i = iPattern.matcher(builder);
		}

		Pattern uPattern = Pattern.compile("(?i)(?<=\\[u\\])((.|\\n)+?)(?=\\[/u\\])");
		Matcher u = uPattern.matcher(builder);
		while (u.find()) {
			builder.setSpan(new UnderlineSpan(), u.start(), u.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			builder.replace(u.end(), u.end() + 4, "");
			builder.replace(u.start() - 3, u.start(), "");
			u = uPattern.matcher(builder);
		}

		Pattern sPattern = Pattern.compile("(?i)(?<=\\[s\\])((.|\\n)+?)(?=\\[/s\\])");
		Matcher s = sPattern.matcher(builder);
		while (s.find()) {
			builder.setSpan(new StrikethroughSpan(), s.start(), s.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			builder.replace(s.end(), s.end() + 4, "");
			builder.replace(s.start() - 3, s.start(), "");
			s = sPattern.matcher(builder);
		}

		Pattern centerPattern = Pattern.compile("(?i)(?<=\\[center\\])((.|\\n)+?)(?=\\[/center\\])");
		Matcher center = centerPattern.matcher(builder);
		while (center.find()) {
			builder.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), center.start(), center.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			builder.replace(center.end(), center.end() + 9, "\n\n");
			builder.replace(center.start() - 8, center.start(), "\n\n");
			center = centerPattern.matcher(builder);
		}

		Pattern hPattern = Pattern.compile("(?i)(?<=\\[h\\])((.|\\n)+?)(?=\\[/h\\])");
		Matcher h = hPattern.matcher(builder);
		while (h.find()) {
			builder.setSpan(new RelativeSizeSpan(1.3f), h.start(), h.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			builder.replace(h.end(), h.end() + 4, "\n\n");
			builder.replace(h.start() - 3, h.start(), "\n\n");
			h = hPattern.matcher(builder);
		}


		Pattern attachmentPattern = Pattern.compile("(?i)\\[attachment:(.*?)]");
		Matcher attachmentM = attachmentPattern.matcher(builder);
		boolean isImage = false;
		while (attachmentList != null && attachmentM.find()) {
			for (int j = attachmentList.size() - 1; j >= 0; j--) {
				final Post.Attachment attachment = attachmentList.get(j);
				if (attachment.getAttachmentId().equals(attachmentM.group(1))) {
					// skip images
					for (String image_ext : Constants.IMAGE_FILE_EXTENSION) {
						if (attachment.getFilename().endsWith(image_ext)) {
							isImage = true;
						}
					}

					if (!isImage) {
						builder.replace(attachmentM.start(), attachmentM.end(), attachment.getFilename());
						builder.setSpan(new ClickableSpan() {
							@Override
							public void onClick(View view) {
								MyUtils.downloadAttachment(context, attachment);
							}
						}, attachmentM.start(), attachmentM.start() + attachment.getFilename().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
						attachmentM = attachmentPattern.matcher(builder);
					}
					break;
				}
			}
		}

		String str = builder.toString();
		for (int j = 0; j < iconStrs.length; j++) {
			int from = 0;
			String iconStr = iconStrs[j];
			while ((from = str.indexOf(iconStr, from)) >= 0) {
				if (("/<".equals(iconStr) && str.substring(from).startsWith("/<<") || ("/#".equals(iconStr) && str.substring(from).startsWith("/##")))) {
					from++;
					continue;
				}

				/**
				 * only show icons when iconStr is surrounded by spaces
				 */
				if (iconStr.equals("/^^")) Log.d(TAG, "parse: " + str.trim().length() + ", " + iconStr.length() + ", " + (from + iconStr.length()) + ", " + str.length());
				if (str.trim().length() == iconStr.length() ||
						((from == 0 || ' ' == str.charAt(from - 1)) && (from + iconStr.length() == str.length() || ' ' == str.charAt(from + iconStr.length()) || '\n' == str.charAt(from + iconStr.length())))) {
					builder.setSpan(new ImageSpan(context, icons[j]), from, from + iconStr.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
				}
				from += iconStr.length();
			}
		}
		return builder;
	}
	public static SpannableStringBuilder parse(final Context context, String string, List<Post.Attachment> attachmentList) {
		final SpannableStringBuilder builder = new SpannableStringBuilder(string);
		return parse(context, builder, attachmentList);
	}
}
