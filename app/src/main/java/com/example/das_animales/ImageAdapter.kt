package com.example.das_animales

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class ImageAdapter(var imageUrls: MutableList<String>, var animales: MutableList<String>, private val locationClickListener: (String) -> Unit) :
    RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        val animal = animales[position]

        // Agregar esquema http si no está presente
        val fullUrl = if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            "http://$imageUrl"
        } else {
            imageUrl
        }

        Picasso.get().load(fullUrl).into(holder.imageView)
        holder.nombreAnimal.text = animal
        holder.obtenerUbicacionButton.setOnClickListener {
            locationClickListener(imageUrl)
        }
    }

    override fun getItemCount(): Int {
        return imageUrls.size
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val obtenerUbicacionButton: Button = itemView.findViewById(R.id.obtenerUbicacionButton)
        val nombreAnimal: TextView = itemView.findViewById(R.id.nombreAnimal)
    }
}
