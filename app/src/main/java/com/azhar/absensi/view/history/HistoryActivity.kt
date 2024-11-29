package com.azhar.absensi.view.history

import android.content.DialogInterface
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.azhar.absensi.R
import com.azhar.absensi.model.ModelDatabase
import com.azhar.absensi.view.history.HistoryAdapter.HistoryAdapterCallback
import com.azhar.absensi.databinding.ActivityHistoryBinding // Import ViewBinding
import com.azhar.absensi.viewmodel.HistoryViewModel

class HistoryActivity : AppCompatActivity(), HistoryAdapterCallback {
    var modelDatabaseList: MutableList<ModelDatabase> = ArrayList()
    lateinit var historyAdapter: HistoryAdapter
    lateinit var historyViewModel: HistoryViewModel

    // Deklarasi binding
    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi binding
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setInitLayout()
        setViewModel()
    }

    private fun setInitLayout() {
        setSupportActionBar(binding.toolbar) // Gunakan binding untuk toolbar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        binding.tvNotFound.visibility = View.GONE // Gunakan binding untuk tvNotFound

        historyAdapter = HistoryAdapter(this, modelDatabaseList, this)
        binding.rvHistory.setHasFixedSize(true) // Gunakan binding untuk rvHistory
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = historyAdapter
    }

    private fun setViewModel() {
        historyViewModel = ViewModelProvider(this).get(HistoryViewModel::class.java) // Gunakan ViewModelProvider yang baru
        historyViewModel.dataLaporan.observe(this) { modelDatabases: List<ModelDatabase> ->
            if (modelDatabases.isEmpty()) {
                binding.tvNotFound.visibility = View.VISIBLE // Gunakan binding untuk tvNotFound
                binding.rvHistory.visibility = View.GONE // Gunakan binding untuk rvHistory
            } else {
                binding.tvNotFound.visibility = View.GONE // Gunakan binding untuk tvNotFound
                binding.rvHistory.visibility = View.VISIBLE // Gunakan binding untuk rvHistory
            }
            historyAdapter.setDataAdapter(modelDatabases)
        }
    }

    override fun onDelete(modelDatabase: ModelDatabase?) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setMessage("Hapus riwayat ini?")
        alertDialogBuilder.setPositiveButton("Ya, Hapus") { dialogInterface, i ->
            val uid = modelDatabase!!.uid
            historyViewModel.deleteDataById(uid)
            Toast.makeText(this@HistoryActivity, "Yeay! Data yang dipilih sudah dihapus",
                Toast.LENGTH_SHORT).show()
        }
        alertDialogBuilder.setNegativeButton("Batal") { dialogInterface: DialogInterface, i: Int -> dialogInterface.cancel() }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
