package com.daquexian.chaoli.forum.utils;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.network.MyOkHttp;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Response;

public class PostUtils
{
	public static final String TAG = "PostUtils";

	public static void reply(int conversationId, String content, final ReplyObserver observer)
	{
		new MyOkHttp.MyOkHttpClient()
				.add("conversationId", String.valueOf(conversationId))
				.add("content", content)
				.add("userId", String.valueOf(Me.getUserId()))
				.add("token", LoginUtils.getToken())
				.post(Constants.replyURL + conversationId)
				.enqueue(new MyOkHttp.Callback() {
					@Override
					public void onFailure(Call call, IOException e) {
						observer.onReplyFailure(-1);
					}

					@Override
					public void onResponse(Call call, Response response, String responseStr) throws IOException {
						if (response.code() != 200) observer.onReplyFailure(response.code());
						else observer.onReplySuccess();
					}
				});
	}

	public static void edit(int postId, String content, final EditObserver observer)
	{
		new MyOkHttp.MyOkHttpClient()
				.add("content", content)
				.add("save", "true")
				.add("userId", String.valueOf(Me.getUserId()))
				.add("token", LoginUtils.getToken())
				.post(Constants.editURL + postId)
				.enqueue(new MyOkHttp.Callback() {
					@Override
					public void onFailure(Call call, IOException e) {
						observer.onEditFailure(-1);
					}

					@Override
					public void onResponse(Call call, Response response, String responseStr) throws IOException {
						if (response.code() != 200) observer.onEditFailure(response.code());
						else observer.onEditSuccess();
					}
				});
	}

	public static void delete(int postId, final DeleteObserver observer)
	{
		new MyOkHttp.MyOkHttpClient()
				.add("userId", String.valueOf(Me.getUserId()))
				.add("token", LoginUtils.getToken())
				.post(Constants.deleteURL + postId)		// Or get?
				.enqueue(new MyOkHttp.Callback() {
					@Override
					public void onFailure(Call call, IOException e) {
						observer.onDeleteFailure(-1);
					}

					@Override
					public void onResponse(Call call, Response response, String responseStr) throws IOException {
						if (response.code() != 200) observer.onDeleteFailure(response.code());
						else observer.onDeleteSuccess();
					}
				});
	}

	public static void restore(int postId, final RestoreObserver observer)
	{
		new MyOkHttp.MyOkHttpClient()
				.add("userId", String.valueOf(Me.getUserId()))
				.add("token", LoginUtils.getToken())
				.post(Constants.deleteURL + postId)		// Or get?
				.enqueue(new MyOkHttp.Callback() {
					@Override
					public void onFailure(Call call, IOException e) {
						observer.onRestoreFailure(-1);
					}

					@Override
					public void onResponse(Call call, Response response, String responseStr) throws IOException {
						if (response.code() != 200) observer.onRestoreFailure(response.code());
						else observer.onRestoreSuccess();
					}
				});
	}

	public static Boolean canEdit(int postId) {
		return false;
	}

	public static Boolean canDelete(int postId) {
		return false;
	}

	public interface ReplyObserver
	{
		void onReplySuccess();
		void onReplyFailure(int statusCode);
	}

	public interface EditObserver
	{
		void onEditSuccess();
		void onEditFailure(int statusCode);
	}

	public interface QuoteObserver
	{
		void onQuoteSuccess();
		void onQuoteFailure(int statusCode);
	}

	public interface DeleteObserver
	{
		void onDeleteSuccess();
		void onDeleteFailure(int statusCode);
	}

	public interface RestoreObserver
	{
		void onRestoreSuccess();
		void onRestoreFailure(int statusCode);
	}
}
