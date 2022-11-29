package com.example.blog.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.blog.R;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class AddPostActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    DatabaseReference userDbRef;


    EditText titleEt, descriptionEt;
    ImageView postIv;
    Button uploadBtn;


    ProgressDialog progressDialog;


    String editTitle, editDescription, editImage;


    String name, email, uid, dp;


    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_CAMERA_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;


    String[] cameraPermissions;
    String[] galleryPermissions;


    Uri image_uri = null;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);



        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Add New Post");
        actionBar.setDisplayHomeAsUpEnabled(true);


        titleEt = findViewById(R.id.post_title_Et);
        descriptionEt = findViewById(R.id.post_description_Et);
        postIv = findViewById(R.id.post_image);
        uploadBtn = findViewById(R.id.post_upload_btn);


        progressDialog = new ProgressDialog(this);


        Intent intent = getIntent();


        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (("text/plains").equals(type)) {
                handleSendText(intent);
            } else if (type.startsWith("image")) {

                handleSendImage(intent);
            }
        }
        final String isUpdateKey = intent.getStringExtra("key");
        final String editPostId = intent.getStringExtra("editPostId");


        if (isUpdateKey != null) {
            if (isUpdateKey.equals("editPost")) {

                actionBar.setTitle("Update Post");
                uploadBtn.setText("Update");
                loadPostData(editPostId);

            }
        } else {
            actionBar.setTitle("Add New Post");
            uploadBtn.setText("Upload");
        }


        firebaseAuth = FirebaseAuth.getInstance();

        checkUserStatus();


        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        galleryPermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};


        userDbRef = FirebaseDatabase.getInstance().getReference("Users");
        Query userQuery = userDbRef.orderByChild("email").equalTo(email);
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    name = "" + snapshot.child("name").getValue();
                    email = "" + snapshot.child("email").getValue();
                    dp = "" + snapshot.child("profile").getValue();

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        actionBar.setSubtitle(email);


        postIv.setOnClickListener(view -> {
            showImagePickDialog();

        });

        uploadBtn.setOnClickListener(view -> {

            String title = titleEt.getText().toString().trim();
            String description = descriptionEt.getText().toString().trim();
            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description)) {
                Toast.makeText(getApplicationContext(), "All Fields Are Required!", Toast.LENGTH_SHORT).show();
            } else {

                if (isUpdateKey != null) {
                    if (isUpdateKey.equals("editPost")) {
                        beginUpdate(title, description, editPostId);
                    }
                } else {
                    uploadData(title, description);

                }
            }


        });

    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    private void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            descriptionEt.setText(sharedText);
        }
    }

    private void handleSendImage(Intent intent) {
        Uri imageURI = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageURI != null) {
            image_uri = imageURI;
            postIv.setImageURI(image_uri);
        }
    }

    private void checkUserStatus() {

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            uid = firebaseUser.getUid();
            email = firebaseUser.getEmail();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

    }

    private void showImagePickDialog() {

        String[] options = {"Camera", "Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image From");
        builder.setItems(options, (dialogInterface, i) -> {


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (i == 0) {
                    if (!checkCameraPermissions()) {
                        requestCameraPermissions();
                    } else {
                        pickFromCamera();
                    }

                } else if (i == 1) {
                    if (!checkGalleryPermissions()) {
                        requestGalleryPermissions();
                    } else {
                        pickFromGallery();
                    }
                }
            } else {
                if (i == 0) {
                    pickFromCamera();
                } else if (i == 1) {
                    pickFromGallery();

                }
            }

        });
        builder.create().show();
    }

    private boolean checkCameraPermissions() {
        boolean storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        boolean cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);

        return storageResult && cameraResult;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestCameraPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {

            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed or your app will crash")
                    .setPositiveButton("OK", (dialog, which) -> requestPermissions(cameraPermissions, CAMERA_REQUEST_CODE))
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .create().show();

        } else {
            requestPermissions(cameraPermissions, CAMERA_REQUEST_CODE);
        }
    }

    private boolean checkGalleryPermissions() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestGalleryPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed or your app will crash")
                    .setPositiveButton("OK", (dialog, which) -> requestPermissions(galleryPermissions, GALLERY_REQUEST_CODE))
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .create().show();

        }
        requestPermissions(galleryPermissions, GALLERY_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted) {

                        pickFromCamera();
                    } else {
                        Toast.makeText(getApplicationContext(), "Camera and storage both permissions are necessary", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case GALLERY_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted) {
                        pickFromGallery();
                    } else {
                        Toast.makeText(getApplicationContext(), "Please enable storage permission", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;

        }
    }

    private void pickFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Title");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");

        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {

            if (requestCode == IMAGE_PICK_CAMERA_CODE) {

                postIv.setImageURI(image_uri);
            }
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                assert data != null;
                image_uri = data.getData();
                postIv.setImageURI(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    private void loadPostData(String editPostId) {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = reference.orderByChild("pId").equalTo(editPostId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    editTitle = "" + snapshot.child("pTitle").getValue();
                    editDescription = "" + snapshot.child("pDescription").getValue();
                    editImage = "" + snapshot.child("pImage").getValue();

                    titleEt.setText(editTitle);
                    descriptionEt.setText(editDescription);
                    if (!editImage.equals("noImage")) {
                        try {
                            Picasso.get().load(editImage).placeholder(R.drawable.ic_default_users).into(postIv);

                        } catch (Exception e) {
                            Picasso.get().load(R.drawable.ic_default_users).into(postIv);

                        }
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void uploadData(final String title, final String description) {

        progressDialog.setMessage("Publishing post...");
        progressDialog.show();

        final String timeStamp = String.valueOf(System.currentTimeMillis());
        String storagePath = "Posts/post_" + timeStamp;
        if (postIv.getDrawable() != null) {

            Bitmap bitmap = ((BitmapDrawable) postIv.getDrawable()).getBitmap();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] data = byteArrayOutputStream.toByteArray();


            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(storagePath);
            storageReference.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> {

                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful()) ;
                        Uri downloadUri = uriTask.getResult();

                        if (uriTask.isSuccessful()) {
                            HashMap<String, Object> results = new HashMap<>();
                            results.put("uid", uid);
                            results.put("uName", name);
                            results.put("uEmail", email);
                            results.put("uDp", dp);
                            results.put("pId", timeStamp);
                            results.put("pTitle", title);
                            results.put("pDescription", description);
                            assert downloadUri != null;
                            results.put("pImage", downloadUri.toString());
                            results.put("pTime", timeStamp);
                            results.put("pLikes", "0");
                            results.put("pComments", "0");

                            //path to store posts data
                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
                            //put data in this reference
                            reference.child(timeStamp).setValue(results)
                                    .addOnSuccessListener(aVoid -> {
                                        //post data is added successfully in posts's database
                                        //dismiss progress dialog
                                        progressDialog.dismiss();
                                        Toast.makeText(getApplicationContext(), "Post Published Successfully...", Toast.LENGTH_SHORT).show();
                                        //reset views
                                        titleEt.setText("");
                                        descriptionEt.setText("");
                                        postIv.setImageURI(null);
                                        image_uri = null;

                                        //send post notification
                                        prepareNotification(
                                                "" + timeStamp, //since we use timestamp for post id
                                                "" + name + " added new post",
                                                "" + title + "\n" + description
                                        );
                                    }).addOnFailureListener(e -> {
                                //post data isn't added successfully in posts's database
                                //dismiss progress dialog
                                progressDialog.dismiss();
                                Toast.makeText(getApplicationContext(), "Error Publishing Post...", Toast.LENGTH_SHORT).show();
                            });

//

//                        else {Execution failed for task ':app:mapDebugSourceSetPaths'.
//> Error while evaluating property 'extraGeneratedResDir' of task ':app:mapDebugSourceSetPaths'
//> Failed to calculate the value of task ':app:mapDebugSourceSetPaths' property 'extraGeneratedResDir'.
//> Querying the mapped value of provider(interface java.util.Set) before task ':app:processDebugGoogleServices' has completed is not supported
                            //error
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Some error occurred", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(e -> {

                //there is some error(s) when uploading the post image,get and show error message, dismiss the progress dialog
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

            });

        } else {
            //post without image
            HashMap<String, Object> results = new HashMap<>();
            results.put("uid", uid);
            results.put("uName", name);
            results.put("uEmail", email);
            results.put("uDp", dp);
            results.put("pId", timeStamp);
            results.put("pTitle", title);
            results.put("pDescription", description);
            results.put("pImage", "noImage");
            results.put("pTime", timeStamp);
            results.put("pLikes", "0");
            results.put("pComments", "0");


            //path to store posts data
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
            //put data in this reference
            reference.child(timeStamp).setValue(results)
                    .addOnSuccessListener(aVoid -> {
                        //post data is added successfully in posts's database
                        //dismiss progress dialog
                        progressDialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Post Published Successfully...", Toast.LENGTH_SHORT).show();
                        //reset views
                        titleEt.setText("");
                        descriptionEt.setText("");
                        postIv.setImageURI(null);
                        image_uri = null;

                        //send post notification
                        prepareNotification(
                                "" + timeStamp, //since we use timestamp for post id
                                "" + name + " added new post",
                                "" + title + "\n" + description
                        );

                    }).addOnFailureListener(e -> {
                //post data isn't added successfully in posts's database
                //dismiss progress dialog
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), "Error Publishing Post...", Toast.LENGTH_SHORT).show();
            });
        }


    }

    private void prepareNotification(String pId, String title, String description) {

        //prepare data for notification
        String NOTIFICATION_TOPIC = "/topics/" + "POST"; //topic must match with what the receiver subscribed to
        String NOTIFICATION_TYPE = "PostNotification"; //now there are two notification type chat & post, so to differentiate in FirebaseMessaging class

        //prepare json what to send and where to send
        JSONObject notificationJO = new JSONObject();
        JSONObject notificationBodyJO = new JSONObject();

        //what to send
        try {
            notificationBodyJO.put("notificationType", NOTIFICATION_TYPE);
            notificationBodyJO.put("sender", uid);  //uid of currently signed in user
            notificationBodyJO.put("pId", pId); //post id
            notificationBodyJO.put("pTitle", title);  //e.g. mohamed added new post
            notificationBodyJO.put("pDescription", description); //content of  post

            /*the parameter (data) is constant and should be written as i wrote it*/
            //where to send
            notificationJO.put("to", NOTIFICATION_TOPIC);
            notificationJO.put("data", notificationBodyJO);
        } catch (JSONException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        sendPostNotification(notificationJO);


    }

    private void sendPostNotification(JSONObject notificationJO) {

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", notificationJO, response -> {

            //response of the request
            Log.d("JSON_RESPONSE_SUCCESS", "onResponse: " + response.toString());


        }, error -> {
            //error occurred
            Log.d("JSON_RESPONSE_ERROR", "onResponse: " + error.toString());
        }) {
            @Override
            public Map<String, String> getHeaders() {

                //put parameters (required headers)
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Key=AAAASLQCUBg:APA91bGIKvb4uaERCa8yFPcTI9-aUnVEuYKVPL2xaJA2l5oL70SYpAEAmT6eXJKFPcsQu1XCiE-bQxaLE9H6xzeVjcSkgtU10rhoGKFzHjuNJDZ1eJKaApvloRdoNs7vQbKLoyNY1dTN");
                return headers;
            }
        };

        //add this request to the queue (enqueue the volley request)
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    private void beginUpdate(String title, String description, String editPostId) {

        //progress dialog
        progressDialog.setMessage("Updating Post...");
        progressDialog.show();

        if (!editImage.equals("noImage")) {
            //was with image
            updatePostWasWithImage(title, description, editPostId);
        } else if (postIv.getDrawable() != null) {
            //was without image, but now has image in image view
            updatePostWithNowImage(title, description, editPostId);
        } else {
            //was without image, but now still no image in image view
            updatePostWithoutImage(title, description, editPostId);

        }


    }

    private void updatePostWasWithImage(final String title, final String description, final String editPostId) {

        /*Steps:
         * 1) Delete image using its url from storage
         * 2) Upload the new image and update other post info like title and description*/

        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(editImage);
        picRef.delete().addOnSuccessListener(aVoid -> {

            //image deleted, now upload the new image
            //for post-image name,post-id and publish time
            final String timeStamp = String.valueOf(System.currentTimeMillis());
            String filePathAndName = "Posts/post_" + timeStamp;

            //get image from imageview
            Bitmap bitmap = ((BitmapDrawable) postIv.getDrawable()).getBitmap();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            //image compress
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] data = byteArrayOutputStream.toByteArray();

            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(filePathAndName);
            storageReference.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> {

                        //image is uploaded to the storage, now gets it's url and all post info and store it in user's database
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful()) ;
                        Uri downloadUri = uriTask.getResult();

                        //check if image is uploaded or not and url is received
                        if (uriTask.isSuccessful()) {
                            //image uploaded
                            //add post image url in post's database and also add some another info
                            HashMap<String, Object> results = new HashMap<>();
                            results.put("pTitle", title);
                            results.put("pDescription", description);
                            assert downloadUri != null;
                            results.put("pImage", downloadUri.toString());

                            //path to store posts data
                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
                            //put data in this reference
                            reference.child(editPostId).updateChildren(results)
                                    .addOnSuccessListener(aVoid1 -> {
                                        //post data is added successfully in posts's database
                                        //dismiss progress dialog
                                        progressDialog.dismiss();
                                        Toast.makeText(getApplicationContext(), "Post Updated Successfully...", Toast.LENGTH_SHORT).show();
                                        //reset views
                                        titleEt.setText("");
                                        descriptionEt.setText("");
                                        postIv.setImageURI(null);
                                        image_uri = null;
                                    }).addOnFailureListener(e -> {
                                //post data isn't added successfully in posts's database
                                //dismiss progress dialog
                                progressDialog.dismiss();
                                Toast.makeText(getApplicationContext(), "Error Updating Post...", Toast.LENGTH_SHORT).show();
                            });


                        } else {
                            //error
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Some error occurred", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(e -> {

                //there is some error(s) when uploading the post image,get and show error message, dismiss the progress dialog
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

            });


        }).addOnFailureListener(e -> {

            //failed, can't go further
            progressDialog.dismiss();
            Toast.makeText(AddPostActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

        });
    }

    private void updatePostWithNowImage(final String title, final String description, final String editPostId) {

        //for post-image name,post-id and publish time
        final String timeStamp = String.valueOf(System.currentTimeMillis());
        String filePathAndName = "Posts/post_" + timeStamp;

        //get image from imageview
        Bitmap bitmap = ((BitmapDrawable) postIv.getDrawable()).getBitmap();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //image compress
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] data = byteArrayOutputStream.toByteArray();

        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(filePathAndName);
        storageReference.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {

                    //image is uploaded to the storage, now gets it's url and all post info and store it in user's database
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!uriTask.isSuccessful()) ;
                    Uri downloadUri = uriTask.getResult();

                    //check if image is uploaded or not and url is received
                    if (uriTask.isSuccessful()) {
                        //image uploaded
                        //add post image url in post's database and also add some another info
                        HashMap<String, Object> results = new HashMap<>();
                        results.put("pTitle", title);
                        results.put("pDescription", description);
                        assert downloadUri != null;
                        results.put("pImage", downloadUri.toString());

                        //path to store posts data
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
                        //put data in this reference
                        reference.child(editPostId).updateChildren(results)
                                .addOnSuccessListener(aVoid -> {
                                    //post data is added successfully in posts's database
                                    //dismiss progress dialog
                                    progressDialog.dismiss();
                                    Toast.makeText(getApplicationContext(), "Post Updated Successfully...", Toast.LENGTH_SHORT).show();
                                    //reset views
                                    titleEt.setText("");
                                    descriptionEt.setText("");
                                    postIv.setImageURI(null);
                                    image_uri = null;
                                }).addOnFailureListener(e -> {
                            //post data isn't added successfully in posts's database
                            //dismiss progress dialog
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Error Updating Post...", Toast.LENGTH_SHORT).show();
                        });


                    } else {
                        //error
                        progressDialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Some error occurred", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {

            //there is some error(s) when uploading the post image,get and show error message, dismiss the progress dialog
            progressDialog.dismiss();
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

        });


    }

    private void updatePostWithoutImage(String title, String description, String editPostId) {
        HashMap<String, Object> results = new HashMap<>();
        results.put("pTitle", title);
        results.put("pDescription", description);
        results.put("pImage", "noImage");

        //path to store posts data
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
        //put data in this reference
        reference.child(editPostId).updateChildren(results)
                .addOnSuccessListener(aVoid -> {
                    //post data is added successfully in posts's database
                    //dismiss progress dialog
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Post Updated Successfully...", Toast.LENGTH_SHORT).show();
                    //reset views
                    titleEt.setText("");
                    descriptionEt.setText("");
                    postIv.setImageURI(null);
                    image_uri = null;
                }).addOnFailureListener(e -> {
            //post data isn't added successfully in posts's database
            //dismiss progress dialog
            progressDialog.dismiss();
            Toast.makeText(getApplicationContext(), "Error Updating Post...", Toast.LENGTH_SHORT).show();
        });


    }

}


