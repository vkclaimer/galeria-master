package com.app.juliogaleria

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private lateinit var mCurrentPhotoPath: String
    var rv_fotos : RecyclerView? = null
    var btn_add_foto : Button? = null

    /**
     *
     * Codigos de permisos
     *
     */
    val PERMISSION_CODE_GALLERY: Int = 1001
    val PERMISSION_CODE_CAMERA: Int = 1002
    val PERMISSION_CODE_READ_STORAGE: Int = 1003

    val appDirectoryName: String = "galeria_julio"
    var photos_uri: ArrayList<Uri> = ArrayList();

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rv_fotos = findViewById(R.id.rv_fotos)
        btn_add_foto = findViewById(R.id.btn_add_foto)

        addEvents()
        checkPermissionForStorage()
    }

    private fun readFolderGallery(){
        photos_uri.clear()

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){

            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.RELATIVE_PATH,
                MediaStore.MediaColumns.DISPLAY_NAME
            )
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("DCIM/$appDirectoryName%") // Test was my folder name
            val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"


            this.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use {
                val id = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val path = it.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                val file_name = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)

                while (it.moveToNext()) {
                    Log.v("path", it.getString(path));
                    Log.v("file", it.getString(file_name));
                    val photoUri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        it.getString(id)
                    )
                    photos_uri.add(photoUri);
                }
            }

        }else{

            val imageRoot = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ), appDirectoryName)

            if(imageRoot.exists()) {
                val listAllFiles = imageRoot.listFiles()

                if (listAllFiles != null && listAllFiles.size > 0) {
                    for (currentFile in listAllFiles) {
                        if( currentFile.name.endsWith(".jpeg") || currentFile.name.endsWith(".jpg") ) {
                            photos_uri.add(Uri.fromFile(currentFile))
                        }
                    }
                }
            }


        }


        populateGrid()
    }

    private fun createFolderGallery(){
        val img_sample_1: Bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.sample_1);
        val img_sample_2: Bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.sample_2);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = this.contentResolver

            Log.i("galeria","entro1");
            val contentValues1 = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "sample_01")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/" + appDirectoryName)
            }

            val contentValues2 = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "sample_02")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/" + appDirectoryName)
            }

            val uri_1 =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues1)
            val uri_2 =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues2)

            if (uri_1 != null && uri_2 != null ) {
                Log.i("galeria","entro2");
                resolver.openOutputStream(uri_1).use {
                    img_sample_1.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    it?.flush()
                    it?.close()
                }

                resolver.openOutputStream(uri_2).use {
                    img_sample_2.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    it?.flush()
                    it?.close()
                }

            }




        }else{

            val imageRoot = File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                ), appDirectoryName
            )

            if( !imageRoot.exists() ) {
                imageRoot.mkdirs()

                val image_1 = File(imageRoot, "sample_01.jpg")
                val image_2 = File(imageRoot, "sample_02.jpg")

                val fOut1 = FileOutputStream(image_1)
                img_sample_1.compress(Bitmap.CompressFormat.PNG, 100, fOut1)
                fOut1.flush()
                fOut1.close()

                val fOut2 = FileOutputStream(image_2)
                img_sample_1.compress(Bitmap.CompressFormat.PNG, 100, fOut2)
                fOut2.flush()
                fOut2.close()
            }


        }




    }

    private fun addEvents(){
        btn_add_foto?.setOnClickListener(View.OnClickListener {

            MaterialAlertDialogBuilder(this)
                .setTitle("Agregar una foto")
                .setMessage("¿Desde donde deseas agregar la foto?")
                .setNeutralButton("Cancelar") { dialog, which ->
                    // Respond to neutral button press
                }
                .setNegativeButton("Camara") { dialog, which ->
                    checkPermissionForCamera()
                }
                .setPositiveButton("Galeria") { dialog, which ->
                    checkPermissionForImage()
                }
                .show()

        })


    }

    var resultLauncherPhoneGallery = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {

            if(result.data!=null) {
                val data: Intent = result.data!!

                var bitmap: Bitmap? = null;

                if (Build.VERSION.SDK_INT < 28) {
                    bitmap = MediaStore.Images.Media.getBitmap(
                        this.contentResolver,
                        data.data
                    )
                } else {
                    val photoUri = data.data;
                    val source = ImageDecoder.createSource(this.contentResolver,photoUri!!)
                    bitmap = ImageDecoder.decodeBitmap(source)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                    val contentValues = ContentValues().apply {
                        put(
                            MediaStore.MediaColumns.DISPLAY_NAME,
                            "jg_" + System.currentTimeMillis()
                        )
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/" + appDirectoryName)
                    }

                    val uri =
                        this.contentResolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                        )

                    if (uri != null) {
                        this.contentResolver.openOutputStream(uri).use {
                            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, it)
                            it?.flush()
                            it?.close()

                            readFolderGallery()
                        }


                    }


                }else{

                    val imageRoot = File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                    ), appDirectoryName)

                    if( !imageRoot.exists() ) {
                        imageRoot.mkdirs()
                    }

                    val image_1 = File(imageRoot, "jg_"+System.currentTimeMillis()+".jpg")


                    val fOut1 = FileOutputStream(image_1)
                    bitmap?.compress(Bitmap.CompressFormat.PNG, 100, fOut1)
                    fOut1.flush()
                    fOut1.close()

                    readFolderGallery()
                }


            }
        }
    }

    var resultLauncherPhoneCamera = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            var bitmap :Bitmap=BitmapFactory.decodeFile(mCurrentPhotoPath)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "jg_" + System.currentTimeMillis())
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/" + appDirectoryName)
                }

                val uri =
                    this.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )

                if (uri != null) {
                    this.contentResolver.openOutputStream(uri).use {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                        it?.flush()
                        it?.close()

                        readFolderGallery()
                    }
                }

            }else{

                val imageRoot = File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                ), appDirectoryName)

                if( !imageRoot.exists() ) {
                    imageRoot.mkdirs()
                }

                val image_1 = File(imageRoot, "jg_"+System.currentTimeMillis()+".jpg")


                val fOut1 = FileOutputStream(image_1)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut1)
                fOut1.flush()
                fOut1.close()

                readFolderGallery()
            }


        }
    }

    private fun checkPermissionForImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                || (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            ) {
                val permissionCoarse = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE)

                requestPermissions(permissionCoarse, PERMISSION_CODE_GALLERY)
            } else {
                openPhoneGallery()
            }
        }else{
            openPhoneGallery()
        }
    }

    private fun checkPermissionForCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                || (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
            ) {
                val permissionCoarse = arrayOf(Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE)

                requestPermissions(permissionCoarse, PERMISSION_CODE_CAMERA)
            } else {
                openPhoneCamera()
            }
        }else{
            openPhoneCamera()
        }
    }

    private fun checkPermissionForStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ( (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) ) {
                val permissionCoarse = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

                requestPermissions(permissionCoarse, PERMISSION_CODE_READ_STORAGE)
            } else {
                readFolderGallery()
            }
        }else{
            readFolderGallery()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_CODE_GALLERY) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openPhoneGallery()
            }
        }

        if (requestCode == PERMISSION_CODE_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openPhoneCamera()
            }
        }

        if (requestCode == PERMISSION_CODE_READ_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readFolderGallery()
            }
        }

    }

    private fun openPhoneGallery(){
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )

        resultLauncherPhoneGallery.launch(galleryIntent)
    }

    @Throws(IOException::class)
    private fun createFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            mCurrentPhotoPath = absolutePath
        }
    }

    private fun openPhoneCamera(){
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val file: File = createFile()

        val uri: Uri = FileProvider.getUriForFile(
            this,
            "com.app.juliogaleria.fileprovider",
            file
        )
        intent.putExtra(MediaStore.EXTRA_OUTPUT,uri)
        resultLauncherPhoneCamera.launch(intent)
    }

    private fun populateGrid(){
        if(photos_uri.size>0) {

            val adapterGaleria = AdapterGaleria(this, photos_uri)

            rv_fotos!!.layoutManager = GridLayoutManager(this, 2)
            rv_fotos!!.adapter = adapterGaleria

        }
    }




}
