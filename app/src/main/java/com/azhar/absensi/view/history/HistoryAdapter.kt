package com.azhar.absensi.view.history

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.azhar.absensi.R
import com.azhar.absensi.model.ModelDatabase
import com.azhar.absensi.utils.BitmapManager.base64ToBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.imageview.ShapeableImageView
import com.azhar.absensi.databinding.ListHistoryAbsenBinding // Import ViewBinding

class HistoryAdapter(
    var mContext: Context,
    var modelDatabase: MutableList<ModelDatabase>,
    var mAdapterCallback: HistoryAdapterCallback
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    fun setDataAdapter(items: List<ModelDatabase>) {
        modelDatabase.clear()
        modelDatabase.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListHistoryAbsenBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = modelDatabase[position]
        holder.binding.tvNomor.text = data.uid.toString()
        holder.binding.tvNama.text = data.nama
        holder.binding.tvLokasi.text = data.lokasi
        holder.binding.tvAbsenTime.text = data.tanggal
        holder.binding.tvStatusAbsen.text = data.keterangan

        Glide.with(mContext)
            .load(base64ToBitmap(data.fotoSelfie))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_photo_camera)
            .into(holder.binding.imageProfile)

        when (data.keterangan) {
            "Absen Masuk" -> {
                holder.binding.colorStatus.setBackgroundResource(R.drawable.bg_circle_radius)
                holder.binding.colorStatus.backgroundTintList = ColorStateList.valueOf(Color.GREEN)
            }
            "Absen Keluar" -> {
                holder.binding.colorStatus.setBackgroundResource(R.drawable.bg_circle_radius)
                holder.binding.colorStatus.backgroundTintList = ColorStateList.valueOf(Color.RED)
            }
            "Izin" -> {
                holder.binding.colorStatus.setBackgroundResource(R.drawable.bg_circle_radius)
                holder.binding.colorStatus.backgroundTintList = ColorStateList.valueOf(Color.BLUE)
            }
        }
    }

    override fun getItemCount(): Int {
        return modelDatabase.size
    }

    inner class ViewHolder(val binding: ListHistoryAbsenBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.cvHistory.setOnClickListener {
                val modelLaundry = modelDatabase[adapterPosition]
                mAdapterCallback.onDelete(modelLaundry)
            }
        }
    }

    interface HistoryAdapterCallback {
        fun onDelete(modelDatabase: ModelDatabase?)
    }
}
