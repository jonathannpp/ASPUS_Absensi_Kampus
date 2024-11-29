package com.azhar.absensi.view.absen

import android.Manifest
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Geocoder
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.DatePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModelProvider
import com.azhar.absensi.BuildConfig
import com.azhar.absensi.R
import com.azhar.absensi.databinding.ActivityAbsenBinding
import com.azhar.absensi.utils.BitmapManager.bitmapToBase64
import com.azhar.absensi.viewmodel.AbsenViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.location.LocationServices
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AbsenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAbsenBinding
    private lateinit var absenViewModel: AbsenViewModel
    private lateinit var progressDialog: ProgressDialog
    private var strCurrentLatitude = 0.0
    private var strCurrentLongitude = 0.0
    private var strFilePath: String = ""
    private var strLatitude = "0"
    private var strLongitude = "0"
    private lateinit var fileDirectory: File
    private lateinit var imageFilename: File
    private lateinit var exifInterface: ExifInterface
    private lateinit var strBase64Photo: String
    private lateinit var strCurrentLocation: String
    private lateinit var strTitle: String
    private lateinit var strTimeStamp: String
    private lateinit var strImageName: String
    private val REQ_CAMERA = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAbsenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setInitLayout()
        setCurrentLocation()
        setUploadData()
    }

    private fun setCurrentLocation() {
        progressDialog.show()
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                progressDialog.dismiss()
                if (location != null) {
                    strCurrentLatitude = location.latitude
                    strCurrentLongitude = location.longitude
                    val geocoder = Geocoder(this, Locale.getDefault())
                    try {
                        val addressList = geocoder.getFromLocation(strCurrentLatitude, strCurrentLongitude, 1)
                        if (!addressList.isNullOrEmpty()) {
                            strCurrentLocation = addressList[0].getAddressLine(0)
                            binding.inputLokasi.setText(strCurrentLocation)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(this, "Ups, gagal mendapatkan lokasi. Silahkan periksa GPS atau koneksi internet Anda!", Toast.LENGTH_SHORT).show()
                    strLatitude = "0"
                    strLongitude = "0"
                }
            }
    }

    private fun setInitLayout() {
        progressDialog = ProgressDialog(this)
        strTitle = intent.extras?.getString(DATA_TITLE).toString()

        if (strTitle != null) {
            binding.tvTitle.text = strTitle
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        absenViewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(AbsenViewModel::class.java)

        binding.inputTanggal.setOnClickListener {
            val tanggalAbsen = Calendar.getInstance()
            val date = DatePickerDialog.OnDateSetListener { _: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                tanggalAbsen.set(year, monthOfYear, dayOfMonth)
                val strFormatDefault = "dd MMMM yyyy HH:mm"
                val simpleDateFormat = SimpleDateFormat(strFormatDefault, Locale.getDefault())
                binding.inputTanggal.setText(simpleDateFormat.format(tanggalAbsen.time))
            }
            DatePickerDialog(this, date, tanggalAbsen[Calendar.YEAR], tanggalAbsen[Calendar.MONTH], tanggalAbsen[Calendar.DAY_OF_MONTH]).show()
        }

        binding.layoutImage.setOnClickListener {
            Dexter.withContext(this)
                .withPermissions(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        if (report.areAllPermissionsGranted()) {
                            try {
                                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                                    putExtra("com.google.assistant.extra.USE_FRONT_CAMERA", true)
                                    putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
                                    putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                                    putExtra("android.intent.extras.CAMERA_FACING", 1)
                                    putExtra("camerafacing", "front")
                                    putExtra("previous_mode", "front")
                                    putExtra("default_camera", "1")
                                    putExtra("default_mode", "com.huawei.camera2.mode.photo.PhotoMode")
                                    putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this@AbsenActivity, BuildConfig.APPLICATION_ID + ".provider", createImageFile()))
                                }
                                startActivityForResult(cameraIntent, REQ_CAMERA)
                            } catch (ex: IOException) {
                                Toast.makeText(this@AbsenActivity, "Ups, gagal membuka kamera", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest>, token: PermissionToken) {
                        token.continuePermissionRequest()
                    }
                }).check()
        }
    }

    private fun setUploadData() {
        binding.btnAbsen.setOnClickListener {
            val strNama = binding.inputNama.text.toString()
            val strTanggal = binding.inputTanggal.text.toString()
            val strKeterangan = binding.inputKeterangan.text.toString()
            if (strFilePath.isEmpty() || strNama.isEmpty() || strCurrentLocation.isEmpty() || strTanggal.isEmpty() || strKeterangan.isEmpty()) {
                Toast.makeText(this, "Data tidak boleh ada yang kosong!", Toast.LENGTH_SHORT).show()
            } else {
                absenViewModel.addDataAbsen(strBase64Photo, strNama, strTanggal, strCurrentLocation, strKeterangan)
                Toast.makeText(this, "Laporan Anda terkirim, tunggu info selanjutnya ya!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        strTimeStamp = SimpleDateFormat("dd MMMM yyyy HH:mm:ss").format(Date())
        strImageName = "IMG_"
        fileDirectory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "")
        imageFilename = File.createTempFile(strImageName, ".jpg", fileDirectory)
        strFilePath = imageFilename.absolutePath
        return imageFilename
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        convertImage(strFilePath)
    }

    private fun convertImage(imageFilePath: String?) {
        val imageFile = File(imageFilePath ?: return)
        if (imageFile.exists()) {
            val options = BitmapFactory.Options()
            var bitmapImage = BitmapFactory.decodeFile(strFilePath, options)

            try {
                exifInterface = ExifInterface(imageFile.absolutePath)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
            val matrix = Matrix()
            if (orientation == 6) {
                matrix.postRotate(90f)
            } else if (orientation == 3) {
                matrix.postRotate(180f)
            } else if (orientation == 8) {
                matrix.postRotate(270f)
            }

            bitmapImage = Bitmap.createBitmap(bitmapImage, 0, 0, bitmapImage.width, bitmapImage.height, matrix, true)

            if (bitmapImage == null) {
                Toast.makeText(this, "Ups, foto kamu belum ada!", Toast.LENGTH_LONG).show()
            } else {
                val resizeImage = (bitmapImage.height * (512.0 / bitmapImage.width)).toInt()
                val scaledBitmap = Bitmap.createScaledBitmap(bitmapImage, 512, resizeImage, true)
                Glide.with(this)
                    .load(scaledBitmap)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_photo_camera)
                    .into(binding.imageSelfie)
                strBase64Photo = bitmapToBase64(scaledBitmap)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val DATA_TITLE = "TITLE"
    }
}
