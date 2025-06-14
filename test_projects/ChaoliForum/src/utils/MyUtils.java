package com.daquexian.chaoli.forum.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.widget.Toast;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.model.Post;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.DOWNLOAD_SERVICE;

/**
 * various strange things
 * Created by jianhao on 16-10-2.
 */

public class MyUtils {
    @SuppressWarnings("unused")
    private static final String TAG = "MyUtils";

    private static final String quoteRegex = "\\[quote((.|\n)*?)\\[/quote]";
    private static final String codeRegex = "\\[code]((.|\n)*?)\\[/code]";
    private static final String imgRegex = "\\[img](.*?)\\[/img]";
    private static final String attRegex = "\\[attachment:(.*?)]";

    /**
     * 针对可能有其他帖子被顶到最上方，导致下一页的主题帖与这一页的主题帖有重合的现象
     * @param A 已有的主题帖列表
     * @param B 下一页主题帖列表
     * @return 合成后的新列表的长度
     */
    public static <T extends Comparable<T>> int expandUnique(List<T> A, List<T> B) {
        return expandUnique(A, B, true);
    }

    /**
     * a > b 表示 a 排在 b 后面
     */
    public static <T extends Comparable<T>> int expandUnique(List<T> A, List<T> B, Boolean addBehind) {
        return expandUnique(A, B, addBehind, false);
    }

    public static <T extends Comparable<T>> int expandUnique(List<T> A, List<T> B, Boolean addBehind, Boolean reversed) {
        int lenA = A.size();
        if (lenA == 0) A.addAll(B);
        else {
            if (addBehind) {
                int i;
                for (i = 0; i < B.size(); i++)
                    if ((!reversed && B.get(i).compareTo(A.get(A.size() - 1)) > 0) || (reversed && B.get(i).compareTo(A.get(A.size() - 1)) < 0))
                        break;
                A.addAll(B.subList(i, B.size()));
            } else {
                int i;
                for (i = 0; i < B.size(); i++)
                    if ((!reversed && B.get(i).compareTo(A.get(0)) >= 0) || (reversed && B.get(i).compareTo(A.get(0)) <= 0))
                        break;
                A.addAll(0, B.subList(0, i));
            }
        }
        return A.size();
    }

    public static <T> List reverse(List<T> list) {
        List<T> reversed = new ArrayList<>();
        for (T item : list) {
            reversed.add(0, item);
        }
        return reversed;
    }

    public static String formatSignature(String str) {
        return str.replace("[code]", "").replace("[/code]", "");
    }

    public static void downloadAttachment(Context context, Post.Attachment attachment) {
        try {
            String attUrl = MyUtils.getAttachmentFileUrl(attachment);
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(attUrl));
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, attachment.getFilename());
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); // to notify when download is complete
                request.allowScanningByMediaScanner();// if you want to be available from media players
                DownloadManager manager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
                manager.enqueue(request);
            } catch (SecurityException e) {
                // in the case of user rejects the permission request
                e.printStackTrace();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(attUrl));
                context.startActivity(intent);
            }
        } catch (UnsupportedEncodingException e) {
            // say bye-bye to your poor phone
            Toast.makeText(context, "say bye-bye to your poor phone", Toast.LENGTH_SHORT).show();
        }
    }

    public static void setToolbarOffset(CoordinatorLayout coordinatorLayout, AppBarLayout appBarLayout, int offset) {
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
        AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) layoutParams.getBehavior();
        if (behavior != null) {
            behavior.setTopAndBottomOffset(offset);
            behavior.onNestedPreScroll(coordinatorLayout, appBarLayout, null, 0, 1, new int[2]);
        }
    }

    public static String getAttachmentImageUrl(Post.Attachment attachment) {
        return Constants.ATTACHMENT_IMAGE_URL + attachment.getAttachmentId() + attachment.getSecret();
    }

    @SuppressWarnings("WeakerAccess")
    public static String getAttachmentFileUrl(Post.Attachment attachment) throws UnsupportedEncodingException{
        return "https://chaoli.club/index.php/attachment/" + attachment.getAttachmentId() + "_" + URLEncoder.encode(attachment.getFilename(), "UTF-8");
    }

    /**
     * 去除引用
     * @param content API获得的帖子内容
     * @return 去除引用之后的内容，显示给用户或用于在发帖时避免多重引用
     */
    public static String removeQuote(String content) {
        return content.replaceAll(quoteRegex, "");
    }

    private static String replaceMany(String content) {
        return content.replaceAll(codeRegex, ChaoliApplication.getAppContext().getString(R.string.see_codes_in_original_post))
                .replaceAll(imgRegex, ChaoliApplication.getAppContext().getString(R.string.see_img_in_original_post))
                .replaceAll(attRegex, ChaoliApplication.getAppContext().getString(R.string.see_att_in_original_post));
    }

    public static String formatQuote(String quote) {
        return removeQuote(replaceMany(quote));
    }

}
