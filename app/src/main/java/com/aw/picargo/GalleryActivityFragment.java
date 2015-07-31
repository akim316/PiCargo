package com.aw.picargo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;


/**
 * This is a fragment (small component) of the GalleryActivity and includes functions that deal with
 *  the camera button, what to do with the picture after it is taken, and some error handling.
 */
public class GalleryActivityFragment extends Fragment {
    private Uri fileUri; //A Uri is used for requesting data or requesting actions
    private ArrayList<Bitmap> bitmaps = new ArrayList<>(); //A list of image bitmaps used for storing the pictures from the server
    long totalSize = 0; //Total size variable for keeping track of the size of the image file (long is an integer data type)
    private SQLiteHandler db; //SQLiteHandler instantiation which handles the database stored on your device
    private SessionManager session; //SessionManager instantiation which keeps track of your current session state
    private String email; //String for keeping track of your email

    private static final String TAG = GalleryActivity.class.getSimpleName(); //Class name tag used for recognizing where Log errors originated from
    private static final int MEDIA_TYPE_IMAGE = 1; //Used to check for the Intent request code
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100; //Used to check for the Intent request code

    private ImageView img; //Image view variable which I use to test if a bitmap is working

    public GalleryActivityFragment() { //Constructor which has no function, but is required
    }

    /**
     * This method is run when the GalleryActivity first starts up, and takes care of the layout
     * I'm currently not doing anything with the container or savedInstanceState in particular, so don't
     * worry about that too much.
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //These 6 lines deal with having a variable keep track of the layout objects so I can manipulate them
        //Layout is the overall layout, gridview is a gridded view that is placed inside the layout,
        //img is the image view, takePhoto and logout is self explanatory, user text is the email of the account
        //that is signed on.
        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.fragment_gallery, container, false);
        GridView gridview = (GridView) layout.findViewById(R.id.gridView);
        img = (ImageView) layout.findViewById(R.id.imageView);

        Button takePhoto = (Button) layout.findViewById(R.id.button);
        Button logout = (Button) layout.findViewById(R.id.button3);
        TextView userText = (TextView) layout.findViewById(R.id.textView);

        //Gets an SQLiteHandler object based off the current activity (has data that stores the user's email
        db = new SQLiteHandler(getActivity().getApplicationContext());
        //user accesses the user details
        HashMap<String, String> user = db.getUserDetails();
        //sets the email string based off the HashMap (a key-pair value)
        email = user.get("email");
        //session manager object based off the current activity
        session = new SessionManager(getActivity().getApplicationContext());
        //new ReceiveAllImages().execute();
        //gridview.setAdapter(new GridViewAdapter(getActivity().getApplicationContext(), R.id.gridView, bitmaps));
        //img.setImageBitmap(bitmaps.get(0));
        //Sets the userText TextView object's text as the email string
        userText.setText(email);

        //Checks if the user is logged in, and if not, logs them out
        if (!session.isLoggedIn()) {
            logoutUser();
        }

        //An onClickListener is used to see if the button is clicked, and the takePhoto button's
        //onClickListener is set here to run the captureImage function
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImage();

            }
        });

        //Similarly done for the logout user button
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });
        return layout;
    }


    /**
     * Function for opening up the camera
     */
    private void captureImage() {
        //Information about intents: http://developer.android.com/reference/android/content/Intent.html
        //Basically used to start up activities or send data between parts of a program
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //Obtains the unique URI which sets up and prepares the image file to be taken, where it is to be placed,
        //and errors that may occur when taking the image.
        fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
        //Stores this information in the Intent (which can be usefully passed on to the following Activity)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        // start the image capture Intent (opens up the camera application)
        startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
    }

    /**
     * Creates an Uri reference based on the media file type being passed in
     * @param type Intent request code
     * @return
     */
    public Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }


    /**
     * Creates the file needed to store the image based off the Intent result code passed in
     * @param type Intent result code
     * @return the File created
     */
    private static File getOutputMediaFile(int type) {

        // External sdcard location
        //If no sdcard, the picture is stored at the directory /storage/sdcard0/Pictures/Android File Upload
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                Config.IMAGE_DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) { //checks if it exists
            if (!mediaStorageDir.mkdirs()) { //mkdir makes a directory and returns if it was created
                Log.d("Gallery", "Oops! Failed create "
                        + Config.IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        //Currently only have it for making image types, not video types
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
     * Calls activities and tasks to start after closing the camera (whether or not you took a picture)
     * @param requestCode Request code that is from previous activity
     * @param resultCode Result code if the activity was successfully done or cancelled
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if the result is capturing Image
        if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
            //If the activity was successfully completed
            if (resultCode == Activity.RESULT_OK) {

                // successfully captured the image
                // launching upload activity
//                Bitmap thumb = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(fileUri.getPath()),
//                        THUMBSIZE, THUMBSIZE);
//                addImageView(bitmapToThumbnail(thumb));
                new UploadFileToServer().execute();
            //If the Activity was cancelled
            } else if (resultCode == Activity.RESULT_CANCELED) {

                // user cancelled Image capture
                //A Toast message is a short notification towards the bottom of your device
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
    }


    /**
     * An AsyncTask performs operations in a background thread. Most other operations not performed in
     * an AsyncTask runs on the UI thread of Android, which it highly disregards when running network
     * operations or other tasks that need to be run simultaneously with other tasks.
     * More info on AsyncTasks because they are important: http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * This AsyncTask is used to communicate with the server to obtain the images on your account and place them into
     * the ArrayList that I created at the very top and display them on your device.
     */
    private class ReceiveAllImages extends AsyncTask<Void, Integer, String> {

        /**
         * Does a task in the background
         * @param params
         * @return
         */
        @Override
        protected String doInBackground(Void... params) {
            return getImages();
        }

        /**
         * Gets all images on the server  (currently not functioning correctly)
         * @return
         */
        private String getImages() {
            //Keep track of a string for the response from the server
            String responseString = null;

            //HttpClient performs all HTTP operations which is used for communication between the server
            //and client
            HttpClient httpclient = new DefaultHttpClient();
            //Create a new HTTP post request to the server URL (Check the Config file for these URLs)
            HttpPost httppost = new HttpPost(Config.IMAGE_ACCESS_URL);

            try {
                //MultipartEntity which can package multiple body parts to be sent thru the post request
                AndroidMultipartEntity entity = new AndroidMultipartEntity(
                        new AndroidMultipartEntity.ProgressListener() { //Includes a progress listener used to publishProgress to a ProgressDialog

                            @Override
                            public void transferred(long num) {
                                publishProgress((int) ((num / (float) totalSize) * 100));
                            }
                        });
                //Adds a StringBody (the user's email address) in the entity
                entity.addPart("email", new StringBody(email));

                //Total size of the image file ("content length")
                totalSize = entity.getContentLength();
                //Prepares the entity to be sent thru the post request
                httppost.setEntity(entity);

                // Makes server call and obtains a response
                HttpResponse response = httpclient.execute(httppost);
                //Entity received from the response
                HttpEntity r_entity = response.getEntity();
                //Prints out the size of the entity through Android's Log feature
                Log.d(TAG, String.valueOf(r_entity.getContentLength()));
                //Sets the entity content to an input stream of bytes
                InputStream is = r_entity.getContent();
                //Output stream to collect the data
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                //Reads buffer from input stream to write to the output stream and collect the image data
                //from the server
                byte[] buffer = new byte[1024];
                int len = 0;
                try {
                    while ((len = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] b = baos.toByteArray();
                //Converts the resultant byte array into an image bitmap using the BitmapFactory class
                Bitmap bmp = BitmapFactory.decodeByteArray(b, 0, b.length);
                //Adds this bitmap to the ArrayList of bitmaps
                bitmaps.add(bmp);
                //img.setImageBitmap(bmp);
                //Status code upon communicating with the server
                int statusCode = response.getStatusLine().getStatusCode();
                //200 means the response was good
                if (statusCode == 200) {
                    // Server response - set this response to a string
                    responseString = EntityUtils.toString(r_entity);
                } else {
                    //If the response wasn't good, set the response string as unsuccesful
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

        /**
         * Performs upon execution - the string parameter is what is received from the doInBackground return value
         * @param s
         */
        @Override
        protected void onPostExecute(String s) {
            Log.d(TAG, "success");
            super.onPostExecute(s);
        }
    }

    /**
     * Another AsyncTask used for uploading the image file that you take on to the server
     */
    private class UploadFileToServer extends AsyncTask<Void, Integer, String> {
        //Prepares a ProgressDialog to show the progress of uploading the image
        ProgressDialog progressDialog = new ProgressDialog(getActivity(), ProgressDialog.STYLE_SPINNER);

        /**
         * Sets the Dialog progress to 0 before starting any tasks
         */
        @Override
        protected void onPreExecute() {
            progressDialog.setProgress(0);
            //progressBar.setProgress(0);
            super.onPreExecute();
        }

        /**
         * While doing the doInBackground task, this method updates the progress of what is happening
         * @param progress
         */
        @Override
        protected void onProgressUpdate(Integer... progress) {
            //Shows the ProgressDialog and sets the progress/message shown
            progressDialog.show();
            progressDialog.setTitle("Uploading");
            progressDialog.setProgress(progress[0]);
            progressDialog.setMessage(String.valueOf(progress[0]) + "%");
        }

        /**
         * Performs the task of uploading the file
         * @param params
         * @return
         */
        @Override
        protected String doInBackground(Void... params) {
            return uploadFile();
        }

        /**
         * Uploads the file that you have after taking a picture
         * @return
         */
        @SuppressWarnings("deprecation")
        private String uploadFile() {
            //Most of the same stuff in the other AsyncTask except we are sending a file
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

                //Recreates the source file to be sent
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
            //Dismiss the progress dialong when successful
            progressDialog.dismiss();
            // showing the server response in an alert dialog
            showAlert(result);

            super.onPostExecute(result);
        }
    }

    /**
     * Shows Alert Dialog upon completion of the upload task
     * @param message
     */
    private void showAlert(String message) {
        //Builds an AlertDialog with message, title, if cancellable, and what the positive button does
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

    //Logs the user out by removing from the SQLite Database and returns to the Login Activity
    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
        getActivity().finish();
    }

}
