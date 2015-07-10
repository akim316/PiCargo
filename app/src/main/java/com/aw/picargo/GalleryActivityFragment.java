package com.aw.picargo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aw.picargo.login_helper.SQLiteHandler;
import com.aw.picargo.login_helper.SessionManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;


/**
 * A placeholder fragment containing a simple view.
 */
public class GalleryActivityFragment extends Fragment {
    private Uri fileUri;
    private ArrayList<Thumbnail> listImages = new ArrayList<>();
    private File pics = new File(Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/Android File Upload");
    private RelativeLayout layout;
    private int col = 0;
    private int row = 0;
    long totalSize = 0;
    private String filePath = null;
    private SQLiteHandler db;
    private SessionManager session;
    private String email;

    private static final String TAG = GalleryActivity.class.getSimpleName();
    private static final String STATE_IMAGES = "state images";
    private static final int MAX_COL = 4;
    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    private static final int THUMBSIZE = 256;

    public GalleryActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        layout = (RelativeLayout) inflater.inflate(R.layout.fragment_gallery, container, false);
        Button takePhoto = (Button) layout.findViewById(R.id.button);
        Button logout = (Button) layout.findViewById(R.id.button3);
        TextView userText = (TextView) layout.findViewById(R.id.textView);

        db = new SQLiteHandler(getActivity().getApplicationContext());
        HashMap<String, String> user = db.getUserDetails();
        email = user.get("email");
        session = new SessionManager(getActivity().getApplicationContext());

        userText.setText(email);
        if (!session.isLoggedIn()) {
            logoutUser();
        }
        if (pics.listFiles() != null) {
            for (File file : pics.listFiles()) {
                Bitmap thumb = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(file.getPath()),
                        THUMBSIZE, THUMBSIZE);
                addImageView(bitmapToThumbnail(thumb));
            }
        }

        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImage();

            }
        });
        if (savedInstanceState != null) {
            listImages = savedInstanceState.getParcelableArrayList(STATE_IMAGES);
            for (Thumbnail thumb : listImages) {
                addImageView(thumb);
            }
        }
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });
        return layout;
    }


    public void addImageView(Thumbnail thumb) {
        GridLayout grid = (GridLayout) layout.findViewById(R.id.gridLayout);
        ImageView img = new ImageView(getActivity());
        img.setImageBitmap(thumb.getImg());
        img.setPadding(10,10,10,10);
        GridLayout.Spec colParam = GridLayout.spec(thumb.getCol());
        GridLayout.Spec rowParam = GridLayout.spec(thumb.getRow());
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowParam, colParam);
        img.setLayoutParams(params);
        grid.addView(img);

    }

    public Thumbnail bitmapToThumbnail(Bitmap image) {
        Thumbnail thumb = new Thumbnail(image);
        if (col < MAX_COL) {
            thumb.setCol(col++);
            thumb.setRow(row);
        } else {
            col = 0;
            thumb.setCol(col);
            thumb.setRow(++row);
        }
        listImages.add(thumb);
        return thumb;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(STATE_IMAGES, listImages);
    }

    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        // start the image capture Intent
        startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
    }

    public Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * returning image / video
     */
    private static File getOutputMediaFile(int type) {

        // External sdcard location
        Log.d("Gallery", Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath());
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                Config.IMAGE_DIRECTORY_NAME);
        Log.d("Gallery", mediaStorageDir.getAbsolutePath());

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("Gallery", "Oops! Failed create "
                        + Config.IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        Log.d("Gallery", "reached");
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");
//        } else if (type == MEDIA_TYPE_VIDEO) {
//            mediaFile = new File(mediaStorageDir.getPath() + File.separator
//                    + "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }


    /**
     * Receiving activity result method will be called after closing the camera
     * */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if the result is capturing Image
        if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {

                // successfully captured the image
                // launching upload activity
                Bitmap thumb = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(fileUri.getPath()),
                        THUMBSIZE, THUMBSIZE);
                addImageView(bitmapToThumbnail(thumb));
                new UploadFileToServer().execute();


            } else if (resultCode == Activity.RESULT_CANCELED) {

                // user cancelled Image capture
                Toast.makeText(getActivity().getApplicationContext(),
                        "User cancelled image capture", Toast.LENGTH_SHORT)
                        .show();

            } else {
                // failed to capture image
                Toast.makeText(getActivity().getApplicationContext(),
                        "Sorry! Failed to capture image", Toast.LENGTH_SHORT)
                        .show();
            }

        }

        if (resultCode == 3232) {
            email = data.getStringExtra("email");
        }
    }

    private class UploadFileToServer extends AsyncTask<Void, Integer, String> {
        ProgressDialog progressDialog = new ProgressDialog(getActivity(), ProgressDialog.STYLE_SPINNER);
        @Override
        protected void onPreExecute() {
            progressDialog.setProgress(0);
            //progressBar.setProgress(0);
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            progressDialog.show();
            progressDialog.setTitle("Uploading");
            progressDialog.setProgress(progress[0]);
            progressDialog.setMessage(String.valueOf(progress[0]) + "%");
        }

        @Override
        protected String doInBackground(Void... params) {
            return uploadFile();
        }

        @SuppressWarnings("deprecation")
        private String uploadFile() {
            String responseString = null;

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(Config.FILE_UPLOAD_URL);

            try {
                AndroidMultipartEntity entity = new AndroidMultipartEntity(
                        new AndroidMultipartEntity.ProgressListener() {

                            @Override
                            public void transferred(long num) {
                                publishProgress((int) ((num / (float) totalSize) * 100));
                            }
                        });

                File sourceFile = new File(fileUri.getPath());

                // Adding file data to http body
                entity.addPart("image", new FileBody(sourceFile));
                entity.addPart("email", new StringBody(email));

                totalSize = entity.getContentLength();
                httppost.setEntity(entity);

                // Making server call
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity r_entity = response.getEntity();
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    // Server response
                    responseString = EntityUtils.toString(r_entity);
                } else {
                    responseString = "Error occurred! Http Status Code: "
                            + statusCode;
                }

            } catch (ClientProtocolException e) {
                responseString = e.toString();
            } catch (IOException e) {
                responseString = e.toString();
            }

            return responseString;

        }

        @Override
        protected void onPostExecute(String result) {
            Log.e(TAG, "Response from server: " + result);
            progressDialog.dismiss();
            // showing the server response in an alert dialog
            showAlert(result);

            super.onPostExecute(result);
        }
    }

    private void showAlert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(message).setTitle("Response from Servers")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // do nothing
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
//        String errorMessage = null;
//        try {
//            JSONObject arr = new JSONObject(message);
//            errorMessage = arr.getString("error");
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();

    }
    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
        getActivity().finish();
    }

}
