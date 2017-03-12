package com.example.ricky.myfirstapp;


import com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.contract.*;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.*;
import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.view.*;
import android.graphics.*;
import android.widget.*;
import android.provider.*;


import java.io.IOException;
import java.io.InputStream;

import static android.R.id.edit;
import java.util.ArrayList;
import java.util.UUID;



public class MainActivity extends AppCompatActivity {
    private FaceServiceClient faceServiceClient =
            new FaceServiceRestClient("3d2df18ce0d942149afe3fb6d2ee75c4");
    private final int PICK_IMAGE = 1;
    private RadioGroup rdioButtonSex;
    private RadioButton radioSexButton;
    private RadioGroup contactChoice;
    private RadioButton contactButton;
    private Button button1;


private ProgressDialog detectionProgressDialog;
   // private FaceServiceClient faceServiceClient = new FaceServiceRestClient("f18cc7f9302d43dd84b34fd25755882a");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        //Printout for radio button values(buttsex)
        View viewbutt = findViewById(R.id.rdioButtonSex);

        rdioButtonSex = (RadioGroup)viewbutt;
        rdioButtonSex.check(R.id.femaleChild);
        //Set for contact options
        contactChoice = (RadioGroup) findViewById(R.id.contactChoice);
        contactChoice.check(R.id.radioButton4);

        button1 = (Button)findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
                gallIntent.setType("image/*");
                startActivityForResult(Intent.createChooser(gallIntent, "Select Picture"), PICK_IMAGE);
            }
        });

        detectionProgressDialog = new ProgressDialog(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null){
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                ImageView imageView = (ImageView) findViewById(R.id.imageView1);
                imageView.setImageBitmap(bitmap);

                detectAndFrame(bitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static Bitmap drawFaceRectanglesOnBitmap(Bitmap originalBitmap, Face[] faces) {
        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        int stokeWidth = 2;
        paint.setStrokeWidth(stokeWidth); //come back
        if (faces != null) {
            for (Face face : faces) {
                FaceRectangle faceRectangle = face.faceRectangle;
                canvas.drawRect(
                        faceRectangle.left,
                        faceRectangle.top,
                        faceRectangle.left + faceRectangle.width,
                        faceRectangle.top + faceRectangle.height,
                        paint);
            }
        }
        return bitmap;
    }

    private void detectAndFrame(final Bitmap imageBitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        byte[] photoData = outputStream.toByteArray();

        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(photoData);
        ByteArrayInputStream inputMissing =
                new ByteArrayInputStream(photoData);
        int selectedId = rdioButtonSex.getCheckedRadioButtonId();
        rdioButtonSex = (RadioGroup) findViewById(R.id.rdioButtonSex);
        radioSexButton = (RadioButton) findViewById(selectedId);

        int selectedId2 = contactChoice.getCheckedRadioButtonId();
        contactChoice = (RadioGroup) findViewById(R.id.contactChoice);
        contactButton = (RadioButton) findViewById(selectedId2);

        final EditText editText = (EditText) findViewById(R.id.editText);
        String test0 = editText.getText().toString();
        String test = radioSexButton.getText().toString().toLowerCase();
        final String lostname = "lostboyz/" + test0 + test + ".jpg";

        AsyncTask<InputStream, Object, SimilarFace[]> detectTask =
                new AsyncTask<InputStream, Object, SimilarFace[]>() {
                    @Override
                    protected SimilarFace[] doInBackground(InputStream... params) {
                        // faces to search through (from gallery)
                        ArrayList<UUID> searchFacesList = new ArrayList<>();

                        // For proof of concept purposes, fetch from a phony database
                        // (image file on the disk)
                        // In the future, possibly fetch from a government database of missing person

                        Face missingFace = null;

                        try {
                            InputStream missingPhoto = (getAssets()).open(lostname);
                            publishProgress("Searching Database...");

                            Face[] missingFaces = faceServiceClient.detect(
                                    missingPhoto,
                                    true,
                                    false,
                                    null
                            );

                            if (missingFaces.length > 0) {
                                missingFace = missingFaces[0];
                            }
                        } catch (Exception e) {
                            publishProgress("Missing Person's Face Could not be Found.");
                            return null; // Can't match if we are unable to detect a face.
                        }

                        // detect faces in each of the user's selected images
                        for (int i = 1; i < params.length; i++) {
                            try {
                                publishProgress("Detecting...");
                                Face[] result = faceServiceClient.detect(
                                        params[i],
                                        true,         // returnFaceId
                                        false,        // returnFaceLandmarks
                                        null          // returnFaceAttributes: a string like "age, gender"
                                );

                                if (result != null) { // Found a Face!
                                    searchFacesList.add(result[0].faceId);
                                    publishProgress("Found Face...", result);
                                } else {
                                    publishProgress("Detection Finished. Nothing detected");
                                }
                            } catch (Exception e) {
                                publishProgress(String.format("Detection failed: %s", e.getMessage()));
                            }
                        }

                        UUID[] searchFaces = searchFacesList.toArray(new UUID[0]);
                        //UUID[] searchFaces = {};

                        // find similar faces to our target person
                        try {
                            SimilarFace[] matches = faceServiceClient.findSimilar(
                                    missingFace.faceId,
                                    searchFaces,
                                    20,
                                    FaceServiceClient.FindSimilarMatchMode.matchPerson
                            );

                            // For now, tell the user
                            // In the future, send data to a government agency (FBI, local police, etc)
                            if (matches.length > 0) {
                                publishProgress("Possible Matches Found!");
                                return matches;
                            } else {
                                publishProgress("No Matches Found.");
                                return null;
                            }
                        } catch (Exception e) {
                            publishProgress("Detection failed");
                            return null;
                        }
                    }

                    @Override
                    protected void onPreExecute() {
                        detectionProgressDialog.show();
                    }

                    @Override
                    protected void onProgressUpdate(Object... progress) {
                        if (progress.length > 1 && progress[1] != null) {
                            ImageView imageView = (ImageView) findViewById(R.id.imageView1);
                            imageView.setImageBitmap(drawFaceRectanglesOnBitmap(imageBitmap, (Face[]) progress[1]));
                            imageBitmap.recycle();
                        }
                        detectionProgressDialog.setMessage((String) progress[0]);
                    }

                    @Override
                    protected void onPostExecute(SimilarFace[] result) {
                        //detectionProgressDialog.dismiss();
                    }
                };
        detectTask.execute(inputMissing, inputStream);
    }


}
