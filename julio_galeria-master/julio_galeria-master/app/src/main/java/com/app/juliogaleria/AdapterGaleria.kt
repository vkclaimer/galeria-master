package com.app.juliogaleria

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.ceylonlabs.imageviewpopup.ImagePopup
import kotlin.collections.ArrayList

class AdapterGaleria(context: Context?, data: ArrayList<Uri>?) : RecyclerView.Adapter<AdapterGaleria.ViewHolder>() {

    var data: ArrayList<Uri>? = null
    private var inflater: LayoutInflater? = null
    var context: Context? = null

    init {
        this.context = context
        inflater = LayoutInflater.from(context)
        this.data = data
    }

    override fun onBindViewHolder(holder: ViewHolder,position: Int) {
        var uri: Uri = data!![position];
        var imagePopup = ImagePopup(this.context)
        imagePopup.setBackgroundColor(Color.BLACK);
        imagePopup.setHideCloseIcon(true);  // Optional

        Glide
            .with(this.context!!)
            .load(uri)
            .listener(object : RequestListener<Drawable>{
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    TODO("Not yet implemented")
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    imagePopup.initiatePopup(resource)

                    return false
                }
            })
            .centerCrop()
            .into(holder.iw_img)


        holder.iw_img.setOnClickListener {
            imagePopup.viewPopup();
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup,viewType: Int): ViewHolder {
        val view: View = inflater!!.inflate(R.layout.layout_image_grid, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data!!.size
    }

    class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var iw_img: ImageView

        init {
            iw_img = itemView.findViewById(R.id.iw_img)
        }
    }
}