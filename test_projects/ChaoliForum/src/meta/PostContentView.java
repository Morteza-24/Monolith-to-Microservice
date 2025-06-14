package com.daquexian.chaoli.forum.meta;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.model.Post;
import com.daquexian.chaoli.forum.utils.MyUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.kbiakov.codeview.CodeView;

/**
 * 包含QuoteView和OnlineImgTextView
 * 用于显示帖子
 * Created by jianhao on 16-8-26.
 */
public class PostContentView extends LinearLayout {
    private final static String TAG = "PostContentView";
    private final static String QUOTE_START_TAG = "[quote";
    private final static Pattern QUOTE_START_PATTERN = Pattern.compile("\\[quote(=(\\d+?):@(.*?))?]");
    private final static String QUOTE_END_TAG = "[/quote]";
    private final static String CODE_START_TAG = "[code]";
    private final static String CODE_END_TAG = "[/code]";
    private final static Pattern ATTACHMENT_PATTERN = Pattern.compile("\\[attachment:(.*?)]");
    private final static String[] TAGS = {QUOTE_START_TAG, QUOTE_END_TAG, CODE_START_TAG, CODE_END_TAG};

    private Context mContext;
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private Post mPost;
    private int mConversationId;
    private List<Post.Attachment> mAttachmentList;
    private OnViewClickListener mOnViewClickListener;

    private Boolean mShowQuote = true;

    public PostContentView(Context context) {
        super(context);
        init(context);
    }

    @SuppressWarnings("unused")
    public PostContentView(Context context, OnViewClickListener onViewClickListener) {
        super(context);
        init(context, onViewClickListener);
    }
    public PostContentView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }
    public PostContentView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        init(context);
    }


    /**
     * Recursive descent
     *
     * fullContent  -> codeBlock attachment
     *
     * codeBlock    -> quote CODE_START code CODE_END codeBlock
     *                  | quote
     *
     * quote        -> table QUOTE_START codeBlock QUOTE_END quote      // due to tech limit, codeBlock here is actually LaTeX
     *                  | table
     *
     * table        -> LaTeX TABLE_START codeBlock TABLE_END table
     *                  | LaTeX
     *
     * LaTeX        -> plainText img LaTeX plainText
     *
     * @param post the post
     */
    public void setPost(Post post) {
        removeAllViews();
        mPost = post;
        mAttachmentList = post.getAttachments();
        List<Post.Attachment> attachmentList = new ArrayList<>(post.getAttachments());
        String content = post.getContent();
        content = content.replaceAll("\u00AD", "");
        fullContent(content, attachmentList);
    }

    /**
     * see {@link #setPost(Post)}
     */
    private void fullContent(String str, List<Post.Attachment> attachmentList) {
        Matcher attachmentMatcher = ATTACHMENT_PATTERN.matcher(str);
        while (attachmentMatcher.find()) {
            String id = attachmentMatcher.group(1);
            for (int i = attachmentList.size() - 1; i >= 0; i--) {
                Post.Attachment attachment = attachmentList.get(i);
                if (attachment.getAttachmentId().equals(id)) {
                    attachmentList.remove(i);
                }
            }
        }

        codeBlock(str);

        SpannableStringBuilder builder = new SpannableStringBuilder();

        boolean isImage = false;
        for (final Post.Attachment attachment : attachmentList) {
            for (String image_ext : Constants.IMAGE_FILE_EXTENSION) {
                if (attachment.getFilename().endsWith(image_ext)) {
                    isImage = true;
                    break;
                }
            }

            if (isImage) {
                String url = MyUtils.getAttachmentImageUrl(attachment);
                final ImageView imageView = new ImageView(mContext);

                LinearLayout.LayoutParams layoutParams = new LayoutParams(Constants.MAX_IMAGE_WIDTH, Constants.MAX_IMAGE_WIDTH / 2);
                imageView.setLayoutParams(layoutParams);
                imageView.setAdjustViewBounds(true);
                imageView.setPadding(0, 0, 0, 10);
                Log.d(TAG, "fullContent: " + url);

                Glide.with(mContext)
                        .load(url)
                        .placeholder(new ColorDrawable(ContextCompat.getColor(mContext, android.R.color.darker_gray)))
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                /**
                                 * adjust the size of ImageView according to image
                                 */
                                imageView.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                imageView.setImageDrawable(resource);

                                imageView.setOnClickListener(new OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (mOnViewClickListener != null) {
                                            mOnViewClickListener.onImgClick(imageView);
                                        }
                                    }
                                });
                                return false;
                            }
                        })
                        .into(imageView);
                addView(imageView);
            } else {
                int start = builder.length();
                builder.append(attachment.getFilename());
                builder.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "onClick() called with: view = [" + view + "]");
                        MyUtils.downloadAttachment(mContext, attachment);
                    }
                }, start, start + attachment.getFilename().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                builder.append("\n\n");
            }
        }

        if (builder.length() > 0) {
            TextView textView = new TextView(mContext);
            textView.setText(builder);
            /**
             * make links clickable
             */
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            addView(textView);
        }
    }

    private void codeBlock(String str) {
        int codeStartPos, codeEndPos = 0;
        String piece, code;
        while (codeEndPos != -1 && (codeStartPos = str.indexOf(CODE_START_TAG, codeEndPos)) >= 0) {//codeMatcher.find(codeEndPos)) {
            if (codeEndPos != codeStartPos) {
                piece = str.substring(codeEndPos, codeStartPos);
                quote(piece);
            }
            codeEndPos = pairedIndex(str, codeStartPos, CODE_START_TAG, CODE_END_TAG);
            //codeEndPos = content.indexOf(CODE_END_TAG, codeStartPos) + CODE_END_TAG.length();
            if (codeEndPos == -1) {
                piece = str.substring(codeStartPos);
                quote(piece);
                codeEndPos = str.length();
            } else {
                code = str.substring(codeStartPos + CODE_START_TAG.length(), codeEndPos - CODE_END_TAG.length());
                code(code);
            }
        }
        if (codeEndPos != str.length()) {
            piece = str.substring(codeEndPos);
            quote(piece);
        }
    }

    /**
     * see {@link #setPost(Post)}
     */
    private void quote(String str) {
        int quoteStartPos, quoteEndPos = 0;
        String piece, quote;
        Matcher quoteMatcher = QUOTE_START_PATTERN.matcher(str);
        while (quoteEndPos != -1 && quoteMatcher.find(quoteEndPos)) {
            quoteStartPos = quoteMatcher.start();

            if (quoteEndPos != quoteStartPos) {
                piece = str.substring(quoteEndPos, quoteStartPos);
                table(piece);
            }
            quoteEndPos = pairedIndex(str, quoteStartPos, QUOTE_START_TAG, QUOTE_END_TAG);

            if (quoteEndPos == -1) {
                piece = str.substring(quoteStartPos);
                table(piece);
                quoteEndPos = str.length();
            } else if (mShowQuote) {
                quote = str.substring(quoteStartPos + quoteMatcher.group().length(), quoteEndPos - QUOTE_END_TAG.length());
                addQuoteView(quote);
            } else {
                addQuoteView("...");
            }
        }

        if (quoteEndPos != str.length()) {
            piece = str.substring(quoteEndPos);
            table(piece);
        }
    }

    private void table(String str) {
        final String SPECIAL_CHAR = "\uF487";
        Pattern pattern = Pattern.compile("(?:\\n|^)( *\\|.+\\| *\\n)??( *\\|(?: *:?----*:? *\\|)+ *\\n)((?: *\\|.+\\| *(?:\\n|$))+)");
        Matcher matcher = pattern.matcher(str);
        int[] margins;
        final int LEFT = 0, RIGHT = 1, CENTER = 2;

        int startIndex = 0, endIndex;

        while (matcher.find()) {
            endIndex = matcher.start();
            if (endIndex != startIndex) {
                LaTeX2(str.substring(startIndex, endIndex));
            }
            startIndex = matcher.end();

            List<String> headers = null;
            if (!TextUtils.isEmpty(matcher.group(1))) {
                String wholeHeader = matcher.group(1);

                headers = new ArrayList<>(Arrays.asList(wholeHeader.split("\\|")));
                format(headers);
            }

            List<String> partitions = new ArrayList<>(Arrays.asList(matcher.group(2).split("\\|")));
            format(partitions);
            final int columnNum = partitions.size();
            margins = new int[columnNum];

            for (int i = 0; i < partitions.size(); i++) {
                String partition = partitions.get(i);
                if (partition.startsWith(":") && partition.endsWith(":")) {
                    margins[i] = CENTER;
                } else if (partition.startsWith(":")) {
                    margins[i] = LEFT;
                } else if (partition.endsWith(":")) {
                    margins[i] = RIGHT;
                } else {
                    margins[i] = CENTER;
                }
            }

            String[] rows = matcher.group(3).replace("\\|", SPECIAL_CHAR).split("\n");
            final List<List<String>> content = new ArrayList<>();
            for (String row : rows) {
                content.add(format(new ArrayList<>(Arrays.asList(row.split("\\|")))));
            }

            final List<String[]> whole = new ArrayList<>();
            if (headers != null) {
                whole.add(headers.toArray(new String[columnNum]));
            }
            for (List<String> strings : content) {
                whole.add(strings.toArray(new String[columnNum]));
            }

            // render table
            HorizontalScrollView scrollView = new HorizontalScrollView(getContext());
            TableLayout tableLayout = new TableLayout(mContext);

            tableLayout.addView(getHorizontalDivider());
            for (int i = 0; i < whole.size(); i++) {
                String[] row = whole.get(i);
                TableRow tableRow = new TableRow(mContext);
                final TableLayout.LayoutParams params = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                tableRow.setLayoutParams(params);

                tableRow.addView(getVerticalDivider());
                for (int j = 0; j < row.length; j++) {
                    String cell = row[j];
                    if (cell != null) {
                        cell = cell.replace(SPECIAL_CHAR, "|");
                    }
                    PostContentView postContentView = PostContentView.newInstance(getContext(), cell, mOnViewClickListener);
                    // TextView textView = new TextView(mContext);
                    // textView.setBackgroundResource((i % 2 == 0) ? R.drawable.cell_shape_black : R.drawable.code_shape_white);
                    // postContentView.setBackgroundResource(R.drawable.code_shape_white);
                    TableRow.LayoutParams pcvParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
                    switch (margins[j]) {
                        case CENTER:
                            pcvParams.gravity = Gravity.CENTER;
                            break;
                        case LEFT:
                            pcvParams.gravity = Gravity.START;
                            break;
                        case RIGHT:
                            pcvParams.gravity = Gravity.END;
                            break;
                    }
                    postContentView.setPadding(10, 10, 10, 10);
                    // pcvParams.setMargins(10, 10, 10, 10);
                    postContentView.setLayoutParams(pcvParams);
                    tableRow.addView(postContentView);
                    tableRow.addView(getVerticalDivider());
                }
                tableLayout.addView(tableRow);
                tableLayout.addView(getHorizontalDivider());
            }

            scrollView.addView(tableLayout);

            addView(scrollView);

            LayoutParams svParams = (LinearLayout.LayoutParams) scrollView.getLayoutParams();
            svParams.setMargins(0, 10, 0, 10);
            scrollView.setLayoutParams(svParams);

            /* Button button = new Button(mContext);
            button.setText("click to see table");
            final List<String> finalHeaders = headers;
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    mOnViewClickListener.onTableButtonClick(finalHeaders != null ? finalHeaders.toArray(new String[columnNum]) : new String[0], whole);
                }
            });
            addView(button); */

            //TableView tableView = (TableView) LayoutInflater.from(mContext).inflate(R.layout.table_view, this, false);
            /* final TableView tableView = new TableView(mContext);
            final String[][] DATA_TO_SHOW = { { "This", "is", "a", "test" },
                    { "and", "a", "second", "test" } };
            final SimpleTableDataAdapter dataAdapter = new SimpleTableDataAdapter(mContext, whole);
            tableView.setDataAdapter(dataAdapter);
            tableView.setColumnCount(columnNum);

            Log.d(TAG, "table: " + tableView.getColumnCount());
            if (headers != null) {
                tableView.setHeaderAdapter(new SimpleTableHeaderAdapter(mContext, headers.toArray(new String[columnNum])));
            }
            addView(tableView);

            tableView.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "run: " + tableView.getHeight());
                    // tableView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    tableView.invalidate();
                }
            });

            //Log.d(TAG, "table: " + tableView.getColumnCount());*/
        }

        if (startIndex != str.length()) {
            LaTeX2(str.substring(startIndex));
        }
    }

    /**
     * see {@link #setPost(Post)}
     */
    private void LaTeX2(String str) {
        Log.d(TAG, "LaTeX2: " + str);
        str = removeTags(str);
        SpannableStringBuilder builder = new SpannableStringBuilder(OnlineImgUtils.removeNewlineInFormula(str));
        builder = SFXParser3.removeTags(SFXParser3.parse(mContext, builder, mAttachmentList));
        List<OnlineImgUtils.Formula> formulaList = OnlineImgUtils.getAll(builder, mAttachmentList);
        formulaList.add(new OnlineImgUtils.Formula(builder.length(), builder.length(), "", "", OnlineImgUtils.Formula.TYPE_IMG));

        int beginIndex = 0, endIndex;
        for (int i = 0; i < formulaList.size() && beginIndex < builder.length(); i++) {
            final OnlineImgUtils.Formula formula = formulaList.get(i);
            endIndex = formula.start;
            if (formula.type == OnlineImgUtils.Formula.TYPE_ATT || formula.type == OnlineImgUtils.Formula.TYPE_IMG) {
                TextView textView = new TextView(mContext);
                final CharSequence subSequence = builder.subSequence(beginIndex, endIndex);
                final SpannableStringBuilder subBuilder = new SpannableStringBuilder(subSequence);
                textView.setText(subSequence);
                /**
                 * make links clickable
                 */
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                addView(textView);
                OnlineImgUtils.retrieveFormulaOnlineImg(OnlineImgUtils.formulasBetween(formulaList, beginIndex, endIndex), textView, subBuilder, 0, beginIndex);
                beginIndex = formula.end + 1;

                if (formula.url.equals("")) {
                    continue;
                }

                final ImageView imageView = new ImageView(mContext);

                LinearLayout.LayoutParams layoutParams;
                if (formula.size == -1) {
                    layoutParams = new LayoutParams(Constants.MAX_IMAGE_WIDTH, Constants.MAX_IMAGE_WIDTH / 2);
                } else {
                    layoutParams = new LayoutParams(formula.size, formula.size);
                }
                imageView.setLayoutParams(layoutParams);
                imageView.setAdjustViewBounds(true);
                imageView.setPadding(0, 0, 0, 10);
                Log.d(TAG, "fullContent: " + formula.url);
                Glide.with(mContext)
                        .load(formula.url)
                        .placeholder(new ColorDrawable(ContextCompat.getColor(mContext, android.R.color.darker_gray)))
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                /**
                                 * adjust the size of ImageView according to image
                                 */
                                if (formula.size == -1) {
                                    imageView.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                }

                                imageView.setOnClickListener(new OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (mOnViewClickListener != null) {
                                            mOnViewClickListener.onImgClick(imageView);
                                        }
                                    }
                                });
                                return false;
                            }
                        })
                        .into(imageView);
                addView(imageView);
            }
        }
    }

    private List<String> format(List<String> strings) {
        for (int i = strings.size() - 1; i >= 0; i--) {
            String str = strings.get(i);
            if (TextUtils.isEmpty(str) || str.equals("\n")) {
                strings.remove(i);
            }
        }

        for (int i = 0; i < strings.size(); i++) {
            strings.set(i, strings.get(i).trim());
        }

        return strings;
    }

    /**
     * see {@link #setPost(Post)}
     */
    private void code(String str) {
        str = removeTags(str);
        CodeView codeView = (CodeView) LayoutInflater.from(mContext).inflate(R.layout.code_view, this, false);
        codeView.setCode(str);
        addView(codeView);
    }

    public static PostContentView newInstance(Context context, String string, OnViewClickListener onViewClickListener) {
        PostContentView postContentView = new PostContentView(context, onViewClickListener);

        if (!TextUtils.isEmpty(string)) {
            postContentView.codeBlock(string);
        }

        return postContentView;
    }

    private int pairedIndex(String str, int from, String startTag, String endTag) {
        int times = 0;
        for (int i = from; i < str.length(); i++) {
            if (str.substring(i).startsWith(startTag)) {
                times++;
            } else if (str.substring(i).startsWith(endTag)) {
                times--;
                if (times == 0) {
                    return i + endTag.length();
                }
            }
        }
        return -1;
    }

    private void addQuoteView(String content) {
        QuoteView quoteView = new QuoteView(mContext, mAttachmentList);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = params.rightMargin = 20;
        quoteView.setLayoutParams(params);
        quoteView.setOrientation(VERTICAL);
        quoteView.setText(content);
        addView(quoteView);
    }

    private String removeTags(String str) {
        for (String tag : TAGS) {
            str = str.replace(tag, "");
        }

        return str;
    }

    private View getHorizontalDivider() {
        View horizontalDivider = new View(mContext);
        horizontalDivider.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        horizontalDivider.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.black));

        return horizontalDivider;
    }

    private View getVerticalDivider() {
        View verticalDivider = new View(mContext);
        verticalDivider.setLayoutParams(new TableRow.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT));
        verticalDivider.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.black));

        return verticalDivider;
    }

    private void init(Context context) {
        init(context, null);
    }

    private void init(Context context, OnViewClickListener onViewClickListener) {
        mOnViewClickListener = onViewClickListener;
        mContext = context;
        removeAllViews();
    }

    public void setOnImgClickListener(OnViewClickListener onViewClickListener) {
        mOnViewClickListener = onViewClickListener;
    }

    public int getConversationId() {
        return mConversationId;
    }

    public void setConversationId(int mConversationId) {
        this.mConversationId = mConversationId;
    }

    @SuppressWarnings("unused")
    public void showQuote(Boolean showQuote) {
        mShowQuote = showQuote;
    }

    public interface OnViewClickListener {
        void onImgClick(ImageView imageView);
    }

    /* private static class Formula {
        static final int TYPE_1 = 1;
        static final int TYPE_2 = 2;
        static final int TYPE_3 = 3;
        static final int TYPE_4 = 4;
        static final int TYPE_5 = 5;
        static final int TYPE_IMG = 4;
        static final int TYPE_ATT = 5;
        int start, end;
        String content, url;
        int type;

        Formula(int start, int end, String content, String url, int type) {
            this.start = start;
            this.end = end;
            this.content = content;
            this.url = url;
            this.type = type;
        }
    } */
}
