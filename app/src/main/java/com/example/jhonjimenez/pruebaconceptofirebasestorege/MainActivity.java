package com.example.jhonjimenez.pruebaconceptofirebasestorege;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    public static final int RC_GALLERY = 21;
    public static final int RC_CAMERA = 22;

    public static final int RP_CAMERA = 121;
    public static final int RP_STORAGE = 122;

    public static final String IMAGE_DIRECTORY = "/MyPhotoApp";
    public static final String MY_PHOTO = "my_photo";

    public static final String PATH_PROFILE = "profile";
    public static final String PATH_PHOTO_URL = "photoUrl";


    @BindView(R.id.imgPhoto)
    AppCompatImageView imgPhoto;
    @BindView(R.id.btnDelete)
    ImageButton btnDelete;
    @BindView(R.id.container)
    ConstraintLayout container;
    @BindView(R.id.btnUpload)
    Button btnUpload;
    private TextView mTextMessage;
    private StorageReference mStorageReference;
    private DatabaseReference mDatabaseReference;

    private String mCurrentPhotoPath;
    private Uri mPhotoSelectedUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        configFirebase();

        configPhotoProfile();
    }

    private void configFirebase() {

        mStorageReference = FirebaseStorage.getInstance().getReference();

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        mDatabaseReference = database.getReference().child(PATH_PROFILE).child(PATH_PHOTO_URL);

    }

    private void configPhotoProfile() {
        //Usando storage
        mStorageReference.child(PATH_PROFILE).child(MY_PHOTO).getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {

                        final RequestOptions options = new RequestOptions()
                                .centerCrop()
                                .diskCacheStrategy(DiskCacheStrategy.ALL);

                        Glide.with(MainActivity.this)
                                .load(uri)
                                .apply(options)
                                .into(imgPhoto);

                        btnDelete.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        btnDelete.setVisibility(View.GONE);
                        Snackbar.make(container, R.string.main_message_error_notFound,
                                Snackbar.LENGTH_LONG).show();
                    }
                });

        // Usando Realtime
        /*mDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final RequestOptions options = new RequestOptions()
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL);

                Glide.with(MainActivity.this)
                        .load(dataSnapshot.getValue())
                        .apply(options)
                        .into(imgPhoto);
                btnDelete.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                btnDelete.setVisibility(View.GONE);
                Snackbar.make(container, R.string.main_message_error_notFound,
                        Snackbar.LENGTH_LONG).show();
            }
        });*/
    }


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_gallery:

//                    fromGallery();
                    checkPermissionToApp(Manifest.permission.READ_EXTERNAL_STORAGE, RP_STORAGE);
                    return true;
                case R.id.navigation_camera:
                    mTextMessage.setText(R.string.main_label_camera);

                    //fromCamera();
//                    dispatchTakePictureIntent();
                    checkPermissionToApp(Manifest.permission.CAMERA, RP_CAMERA);
                    return true;
            }
            return false;
        }
    };

    private void checkPermissionToApp(String permissionStr, int requestPermission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, permissionStr) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{permissionStr}, requestPermission);
                return;
            }
        }

        switch (requestPermission){
            case RP_STORAGE:
                fromGallery();
                break;
            case RP_CAMERA:
                dispatchTakePictureIntent();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            switch (requestCode){
                case RP_STORAGE:
                    fromGallery();
                    break;
                case RP_CAMERA:
                    dispatchTakePictureIntent();
                    break;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void fromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, RC_GALLERY);
    }

    private void fromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, RC_CAMERA);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null){
            File photoFile;
            photoFile = createImageFile();

            if (photoFile != null){
                Uri photoUri = FileProvider.getUriForFile(this,
                        "com.example.jhonjimenez.pruebaconceptofirebasestorege", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, RC_CAMERA);
            }
        }
    }

    private File createImageFile() {
        final String timeStamp = new SimpleDateFormat("dd-MM-yyyy_HHmmss", Locale.ROOT)
                .format(new Date());
        final String imageFileName = MY_PHOTO + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = null;
        try {
            image = File.createTempFile(imageFileName, ".jpg", storageDir);
            mCurrentPhotoPath = image.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RC_GALLERY:

                    if (data != null) {
                        mPhotoSelectedUri = data.getData();

                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),
                                    mPhotoSelectedUri);
                            imgPhoto.setImageBitmap(bitmap);
                            btnDelete.setVisibility(View.GONE);
                            mTextMessage.setText(R.string.main_message_question_upload);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    break;
                case RC_CAMERA:
                    /*Bundle extras = data.getExtras();
                    Bitmap bitmap = (Bitmap)extras.get("data");*/

                    mPhotoSelectedUri = addPicGallery();

                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),
                                mPhotoSelectedUri);
                        imgPhoto.setImageBitmap(bitmap);
                        btnDelete.setVisibility(View.GONE);
                        mTextMessage.setText(R.string.main_message_question_upload);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    private Uri addPicGallery() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
        mCurrentPhotoPath = null;
        return contentUri;
    }

    @OnClick(R.id.btnUpload)
    public void onUploadPhoto() {
        StorageReference profileReference = mStorageReference.child(PATH_PROFILE);

        StorageReference photoReference = profileReference.child(MY_PHOTO);

        photoReference.putFile(mPhotoSelectedUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Snackbar.make(container, R.string.main_message_upload_succes, Snackbar.LENGTH_LONG).show();
                taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        savePhotoUri(uri);
                        btnDelete.setVisibility(View.VISIBLE);
                        mTextMessage.setText(R.string.main_message_done);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Snackbar.make(container, R.string.main_message_upload_error, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void savePhotoUri(Uri downloadUri) {
        mDatabaseReference.setValue(downloadUri.toString());
    }

    @OnClick(R.id.btnDelete)
    public void onDeletePhoto() {
        mStorageReference.child(PATH_PROFILE).child(MY_PHOTO).delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mDatabaseReference.removeValue();
                        imgPhoto.setImageBitmap(null);
                        btnDelete.setVisibility(View.GONE);
                        Snackbar.make(container, R.string.main_message_delete_success,
                                Snackbar.LENGTH_LONG).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Snackbar.make(container, R.string.main_message_delete_error, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
