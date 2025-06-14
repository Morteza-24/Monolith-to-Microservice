package com.daquexian.chaoli.forum.view;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.databinding.ActivitySettingsBinding;
import com.daquexian.chaoli.forum.utils.AccountUtils;
import com.daquexian.chaoli.forum.meta.AvatarView;

import java.io.File;

import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;
import com.daquexian.chaoli.forum.viewmodel.SettingsVM;

/**
 * Created by jianhao on 16-3-12.
 */
public class SettingsActivity extends BaseActivity implements AccountUtils.GetProfileObserver{
    private static final String TAG = "SettingsActivity";
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    private final String IMAGE_TYPE = "image/*";
    private final int IMAGE_CODE = 0;   //这里的IMAGE_CODE是自己任意定义的

    SettingsVM viewModel;

    ProgressDialog progressDialog;

    Context mContext;
    AvatarView avatar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();

        viewModel.showProcessDialog.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                if (((ObservableBoolean) observable).get()) {
                    progressDialog = ProgressDialog.show(mContext, "", getString(R.string.just_a_sec));
                } else {
                    if (progressDialog != null) progressDialog.dismiss();
                }
            }
        });

        viewModel.showToast.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                showToast(viewModel.toastContent.get());
            }
        });

        viewModel.goToAlbum.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                goToAlbum();
            }
        });

        viewModel.complete.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String selectedPath = getPath(selectedImage);
            Log.d(TAG, "onActivityResult: " + selectedPath);
            File selectedFile = new File(selectedPath);
            viewModel.avatarFile = selectedFile;
            ((AvatarView) findViewById(R.id.iv_avatar)).update(selectedFile);
        }
    }

    @Override
    public void onGetProfileSuccess() {
        updateViews();
    }

    @Override
    public void onGetProfileFailure() {

    }

    private void init() {
        setViewModel(new SettingsVM(Me.getMySignature(), Me.getMyStatus(), Me.getMyPrivateAdd(), Me.getMyStarOnReply(), Me.getMyStarPrivate(), Me.getMyHideOnline()));
        configToolbar(R.string.settings);

        mContext = this;
        avatar = (AvatarView)findViewById(R.id.iv_avatar);

        updateViews();
    }

    public void updateViews(){
        avatar.update(Me.getAvatarSuffix(), Me.getMyUserId(), Me.getUsername());
        //private_add_chk.setChecked(Me.getPreferences().getPrivateAdd());
        //star_on_reply_chk.setChecked(Me.getPreferences().getStarOnReply());
        //star_private_chk.setChecked(Me.getPreferences().getStarPrivate());
        //hide_online_chk.setChecked(Me.getPreferences().getHideOnline());
        //signature_edtTxt.setText(Me.getPreferences().getSignature());
        //user_status_edtTxt.setText(Me.getStatus());
    }

    /**
     * helper to retrieve the path of an image URI
     */
    public String getPath(Uri uri) {
        // just some safety built in
        if( uri == null ) {
            return null;
        }
        // try to retrieve the image from the media store first
        // this will only work for images selected from gallery
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if( cursor != null ){
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        // this is our fallback here
        return uri.getPath();
    }

    /* Get the real path from the URI */
    public String getPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    public void goToAlbum() {
        //Intent getAlbum = new Intent(Intent.ACTION_GET_CONTENT);
        //getAlbum.setType(IMAGE_TYPE);
        //startActivityForResult(Intent.createChooser(getAlbum, "Select Picture"), IMAGE_CODE);
        if (Build.VERSION.SDK_INT >= 23){
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(SettingsActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(SettingsActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(SettingsActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                    // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }else{
                ActivityCompat.requestPermissions(SettingsActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }else {

            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, IMAGE_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, IMAGE_CODE);
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                break;
        }
    }

    @Override
    public void setViewModel(BaseViewModel viewModel) {
        this.viewModel = (SettingsVM) viewModel;
        ActivitySettingsBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_settings);
        binding.setViewModel(this.viewModel);
    }
}
