package com.daquexian.chaoli.forum.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.daquexian.chaoli.forum.view.MainActivity;

public class SingleUtils
{
	private int dpi;

	private static SingleUtils ourInstance = new SingleUtils();

	public static SingleUtils getInstance()
	{
		return ourInstance;
	}

	private SingleUtils()
	{
		DisplayMetrics dm = new DisplayMetrics();
		((WindowManager) new MainActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(dm);
		dpi = dm.densityDpi;
	}

	public int getDpi()
	{
		return dpi;
	}
}
