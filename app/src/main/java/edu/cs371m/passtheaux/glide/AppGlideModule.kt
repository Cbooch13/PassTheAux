package edu.cs371m.passtheaux.glide

import android.content.Context
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.storage.StorageReference
import java.io.InputStream

@GlideModule
class AppGlideModule: AppGlideModule() {
//    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
//        // Register FirebaseImageLoader to handle StorageReference
//        registry.append(
//            StorageReference::class.java, InputStream::class.java,
//            FirebaseImageLoader.Factory()
//        )
//    }
}
object Glide {
    private var glideOptions = RequestOptions ()
        // Options like CenterCrop are possible, but I like this one best
        // Evidently you need fitCenter or dontTransform.  If you use centerCrop, your
        // list disappears.  I think that was an old bug.
        .fitCenter()
        // Rounded corners are so lovely.
        .transform(RoundedCorners (20))

//    fun fetch(storageReference: StorageReference, imageView: ImageView) {
//        // Layout engine does not know size of imageView
//        // Hardcoding this here is a bad idea.  What would be better?
//        val width = 400
//        val height = 400
//        GlideApp.with(imageView.context)
//            .asBitmap() // Try to display animated Gifs and video still
//            .load(storageReference)
//            .apply(glideOptions)
//            .error(android.R.color.holo_red_dark)
//            .override(width, height)
//            .into(imageView)
//    }
    fun loadImage(imageView: ImageView, url: String) {
        val options = RequestOptions()
            .fitCenter()
            .transform(RoundedCorners(20))

        Glide.with(imageView.context)
            .load(url)
            .apply(options)
            .into(imageView)
    }

    fun loadImage(imageView: ImageView, byteArray: ByteArray) {
        val options = RequestOptions()
            .fitCenter()
            .transform(RoundedCorners(20))

        Glide.with(imageView.context)
            .load(byteArray)
            .apply(options)
            .into(imageView)
    }

    fun clearImage(imageView: ImageView) {
        Glide.with(imageView.context)
            .clear(imageView)
        Log.d(javaClass.simpleName, "Cleared image from ImageView")
    }
}