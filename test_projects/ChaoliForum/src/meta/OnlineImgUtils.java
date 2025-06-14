package com.daquexian.chaoli.forum.meta;

import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.daquexian.chaoli.forum.model.Post;
import com.daquexian.chaoli.forum.utils.MyUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substitute {@link OnlineImgImpl} temporarily
 * Created by daquexian on 17-2-6.
 */

public class OnlineImgUtils {
    private static final String TAG = "OnlineImgUtils";

    private static final String SITE = "http://latex.codecogs.com/gif.latex?\\dpi{220}";

    private static final Pattern PATTERN1 = Pattern.compile("(?i)\\$\\$?((.|\\n)+?)\\$\\$?");
    private static final Pattern PATTERN2 = Pattern.compile("(?i)\\\\[(\\[]((.|\\n)*?)\\\\[\\])]");
    private static final Pattern PATTERN3 = Pattern.compile("(?i)\\[tex]((.|\\n)*?)\\[/tex]");
    private static final Pattern PATTERN4 = Pattern.compile("(?i)\\\\begin\\{.*?\\}(.|\\n)*?\\\\end\\{.*?\\}");
    private static final Pattern PATTERN5 = Pattern.compile("(?i)\\$\\$(.+?)\\$\\$");
    private static final Pattern IMG_PATTERN = Pattern.compile("(?i)\\[img(=\\d+)?](.*?)\\[/img]");
    private static final Pattern ATTACHMENT_PATTERN = Pattern.compile("(?i)\\[attachment:(.*?)]");
    private static final Pattern[] PATTERNS = {PATTERN1, PATTERN2, PATTERN3, PATTERN4, PATTERN5, IMG_PATTERN, ATTACHMENT_PATTERN};
    private static final int[] indexInRegex = {1, 1, 1, 0, 1, 2, 1};

    public static List<Formula> getAll(CharSequence charSequence, List<Post.Attachment> attachmentList) {
        return getSpecific(charSequence, attachmentList, new int[]{0, 1, 2, 3, 4, 5, 6});
    }

    public static List<Formula> getLaTeXFormula(CharSequence charSequence, List<Post.Attachment> attachmentList) {
        return getSpecific(charSequence, attachmentList, new int[]{0, 1, 2, 3, 4});
    }

    public static List<Formula> getImgAndAtt(CharSequence charSequence, List<Post.Attachment> attachmentList) {
        return getSpecific(charSequence, attachmentList, new int[]{5, 6});
    }

    /**
     * @param charSequence 包含公式的字符串
     * @return 公式List
     */
    private static List<Formula> getSpecific(CharSequence charSequence, List<Post.Attachment> attachmentList, int[] indexs) {
        Matcher[] matchers = new Matcher[indexs.length];
        int[] indexInRegexLocal = new int[indexs.length];
        for (int i = 0; i < indexs.length; i++) {
            int index = indexs[i];
            matchers[i] = PATTERNS[index].matcher(charSequence);
            indexInRegexLocal[i] = indexInRegex[index];
        }

        List<Formula> formulaList = new ArrayList<>();

        int[] types = {Formula.TYPE_1, Formula.TYPE_2, Formula.TYPE_3, Formula.TYPE_4, Formula.TYPE_5, Formula.TYPE_IMG, Formula.TYPE_ATT};

        for (int i = 0; i < matchers.length; i++) {
            Matcher matcher = matchers[i];
            int index = indexInRegexLocal[i];

            int start, end, size;
            String content, url;
            int type = types[i];

            while (matcher.find()) {
                url = "";
                size = -1;
                start = matcher.start();
                end = matcher.end();
                content = matcher.group(index);

                if (type == Formula.TYPE_IMG) {
                    url = content;
                    if (!TextUtils.isEmpty(matcher.group(1))) {
                        try {
                            size = Integer.parseInt(matcher.group(1).substring(1));
                        } catch (NumberFormatException e) {
                            size = -1;
                        }
                    }
                } else if (type == Formula.TYPE_ATT) {
                    for (int j = attachmentList.size() - 1; j >= 0; j--) {
                        Post.Attachment attachment = attachmentList.get(j);
                        if (attachment.getAttachmentId().equals(content)) {
                            for (String image_ext : Constants.IMAGE_FILE_EXTENSION) {
                                if (attachment.getFilename().endsWith(image_ext)) {
                                    url = MyUtils.getAttachmentImageUrl(attachment);
                                }
                            }
                        }
                    }
                } else {
                    url = SITE + content;
                }
                formulaList.add(new Formula(start, end, content, url, type, size));
            }
        }

        removeOverlappingFormula(formulaList);
        Collections.sort(formulaList);
        return formulaList;
    }

    /**
     * 去掉相交的区间
     * @param formulaList 每个LaTeX公式的起始下标和终止下标组成的List
     */
    public static void removeOverlappingFormula(List<Formula> formulaList) {
        Collections.sort(formulaList, new Comparator<Formula>() {
            @Override
            public int compare(Formula p1, Formula p2) {
                return p1.start - p2.start;
            }
        });
        int size = formulaList.size();
        for (int i = 0; i < size - 1; i++) {
            for (int j = i + 1; j < size;) {
                if (formulaList.get(j).start < formulaList.get(i).end) {
                    formulaList.remove(j);
                    size--;
                } else j++;
            }
        }
    }

    /**
     * Fix the "bug" that ImageSpan shows as many as the num of lines of the string substituted
     * @param str string containing formulas
     * @return string containing single-line formulas
     */
    public static String removeNewlineInFormula(String str){
        Matcher m1 = PATTERN1.matcher(str);
        Matcher m2 = PATTERN2.matcher(str);
        Matcher m3 = PATTERN3.matcher(str);
        Matcher m4 = PATTERN4.matcher(str);
        Matcher m5 = PATTERN5.matcher(str);
        Matcher[] matchers = {m1, m2, m3, m4, m5};
        for (Matcher matcher : matchers) {
            while (matcher.find()) {
                String oldStr = matcher.group();
                String newStr = oldStr.replaceAll("[\\n\\r]", "");

                str = str.replace(oldStr, newStr);
            }
        }

        return str;
    }

    /**
     * 获取特定的公式渲染出的图片，结束时会调用回调函数（假如存在listener的话）
     * @param builder 包含公式的builder
     * @param i 公式在mFormulaList中的下标
     * @param start index in full text of first char in builder, use for adjust the beginning and ending of formula to fit with builder
     */
    public static void retrieveFormulaOnlineImg(final List<Formula> formulaList, final TextView view, final SpannableStringBuilder builder, final int i, final int start) {
        if (i >= formulaList.size()) {
            return;
        }
        final Formula formula = formulaList.get(i);
        Log.d(TAG, "retrieveFormulaOnlineImg: " + formula.url);
        final int finalStart = formula.start;
        final int finalEnd = formula.end;
        Glide.with(view.getContext())
                .load(formula.url)
                .asBitmap()
                .placeholder(new ColorDrawable(ContextCompat.getColor(view.getContext(), android.R.color.darker_gray)))
                .into(new SimpleTarget<Bitmap>()
                {
                    @Override
                    public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation)
                    {
                        final int HEIGHT_THRESHOLD = 60;
                        // post to avoid ConcurrentModificationException, from https://github.com/bumptech/glide/issues/375
                        view.post(new Runnable() {
                            @Override
                            public void run() {
                                Bitmap newImage;
                                if (resource.getWidth() > Constants.MAX_IMAGE_WIDTH) {
                                    int newHeight = resource.getHeight() * Constants.MAX_IMAGE_WIDTH / resource.getWidth();
                                    newImage = Bitmap.createScaledBitmap(resource, Constants.MAX_IMAGE_WIDTH, newHeight, true);
                                } else {
                                    newImage = resource;
                                }

                                if(newImage.getHeight() > HEIGHT_THRESHOLD) {
                                    builder.setSpan(new ImageSpan(((View)view).getContext(), newImage), finalStart - start, finalEnd - start, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                                } else {
                                    builder.setSpan(new CenteredImageSpan(((View)view).getContext(), newImage), finalStart - start, finalEnd - start, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                                }

                                if (view instanceof EditText) {
                                    EditText editText = (EditText) view;
                                    int selectionStart = editText.getSelectionStart();
                                    int selectionEnd = editText.getSelectionEnd();
                                    view.setText(builder);
                                    editText.setSelection(selectionStart, selectionEnd);
                                } else {
                                    view.setText(builder);
                                }
                                retrieveFormulaOnlineImg(formulaList, view, builder, i + 1, start);
                            }
                        });
                    }
                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        super.onLoadFailed(e, errorDrawable);
                        if (e != null) e.printStackTrace();
                        retrieveFormulaOnlineImg(formulaList, view, builder, i + 1, start);
                    }

                });
    }

    /**
     *
     * @param formulaList the whole formula list
     * @param start [start, end)
     * @param end [start, end)
     * @return formulas in [start, end)
     */
    public static List<Formula> formulasBetween(List<Formula> formulaList, int start, int end) {
        int first = -1, last = -1;
        for (int i = 0; i < formulaList.size(); i++) {
            Formula formula = formulaList.get(i);
            if (first == -1 && formula.start >= start) {
                first = i;
            }
            if (formula.end > end) {
                last = i;
                break;
            }
        }

        if (first == -1 && last == -1) {
            return formulaList;
        } else if (last == -1) {
            return formulaList.subList(first, formulaList.size());
        }

        return formulaList.subList(first, last);
    }

    public static class Formula implements Comparable<Formula> {
        static final int TYPE_1 = 1;
        static final int TYPE_2 = 2;
        static final int TYPE_3 = 3;
        static final int TYPE_4 = 4;
        static final int TYPE_5 = 5;
        static final int TYPE_IMG = 6;
        static final int TYPE_ATT = 7;
        int start, end;
        String content, url;
        int type;
        int size;

        Formula(int start, int end, String content, String url, int type) {
            this(start, end, content, url, type, -1);
        }

        Formula(int start, int end, String content, String url, int type, int size) {
            this.start = start;
            this.end = end;
            this.content = content;
            this.url = url;
            this.type = type;
            this.size = size;
        }

        @Override
        public int compareTo(@NotNull Formula formula) {
            return Integer.valueOf(start).compareTo(formula.start);
        }
    }
}
