package com.daquexian.chaoli.forum.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

@Deprecated
public class Splash extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//TODO Any splash screen here?
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		//Toast.makeText(Splash.this, String.format(Locale.getDefault(), "%.2f%%", TODO.getStatus() * 100), Toast.LENGTH_SHORT).show();
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		Thread t = new Thread()
		{
			@Override
			public void run()
			{
				super.run();
				try
				{
					sleep(0);
					startActivity(new Intent(Splash.this, Class.forName("com.geno.chaoli.forum.view.MainActivity")));
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				finally
				{
					finish();
				}
			}
		};
		t.start();
	}
}
