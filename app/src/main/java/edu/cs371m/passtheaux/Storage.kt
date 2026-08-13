package edu.cs371m.passtheaux

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

// Store files in firebase storage
class Storage {
    companion object {
        const val TAG: String = "Storage"
    }
    // Create a storage reference from our app
    private val songStorage: StorageReference =
        FirebaseStorage.getInstance().reference.child("songs")


    fun uploadSong(localFileUri: Uri, uuid: String, uploadSuccess:()->Unit) {
        // XXX Write me
        val songRef = songStorage.child(uuid)
        val uploadTask = songRef.putFile(localFileUri)

        // Register observers to listen for when the download is done or if it fails
        uploadTask
            .addOnFailureListener {
                // Handle unsuccessful uploads
                Log.d(TAG, "Upload FAILED $uuid")
            }
            .addOnSuccessListener {
                // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                uploadSuccess()
                Log.d(TAG, "Upload SUCCESS $uuid")
            }
    }
    // https://firebase.google.com/docs/storage/android/delete-files#delete_a_file
    fun deleteSong(songUUID: String) {
        // Delete the file
        // XXX Write me
        val songRef = songStorage.child(songUUID)
        songRef.delete().addOnSuccessListener {
            Log.d(TAG, "Deleted $songUUID")
        }.addOnFailureListener {
            Log.d(TAG, "Delete FAILED $songUUID")
        }
    }

    fun uuid2StorageReference(uuid: String): StorageReference {
        return songStorage.child(uuid)
    }
}