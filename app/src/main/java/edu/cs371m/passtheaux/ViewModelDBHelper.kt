package edu.cs371m.passtheaux

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import edu.cs371m.passtheaux.model.ProfileMeta
import edu.cs371m.passtheaux.model.SongMeta

class ViewModelDBHelper {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val rootCollection = "songs"
    private val userCollection = "profiles"

    // If we want to listen for real time updates use this
    // .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
    private fun limitAndGet(query: Query,
                            resultListener: (List<SongMeta>)->Unit) {
        query
            .get()
            .addOnSuccessListener { result ->
                Log.d(javaClass.simpleName, "allNotes fetch ${result!!.documents.size}")
                // NB: This is done on a background thread
                resultListener(result.documents.mapNotNull {
                    it.toObject(SongMeta::class.java)
                })
            }
            .addOnFailureListener {
                Log.d(javaClass.simpleName, "allNotes fetch FAILED ", it)
                resultListener(listOf())
            }
    }
    /////////////////////////////////////////////////////////////
    // Interact with Firestore db
    // https://firebase.google.com/docs/firestore/query-data/order-limit-data
    fun fetchSongMeta(
        resultListener: (List<SongMeta>) -> Unit
    ) {
        // XXX Write me and use limitAndGet
        val direction = Query.Direction.DESCENDING

        val query = db.collection(rootCollection).orderBy("totalListens", direction)
        limitAndGet(query, resultListener)

    }

    // https://firebase.google.com/docs/firestore/manage-data/add-data#add_a_document
    fun uploadSongMeta(
        songMeta: SongMeta,
        resultListener: (List<SongMeta>)->Unit
    ) {
        // XXX Write me: add photoMeta
        db.collection(rootCollection)
            .add(songMeta)
            .addOnSuccessListener {
                Log.d(
                    javaClass.simpleName,
                    "Song created \"${songMeta.name}\", id \"${songMeta.firestoreID}\""
                )
                fetchSongMeta(resultListener)
            }
            .addOnFailureListener { e ->
                Log.d(javaClass.simpleName, "Note create FAILED \"${songMeta.firestoreID}\"")
                Log.w(javaClass.simpleName, "Error ", e)

            }
    }

    // https://firebase.google.com/docs/firestore/manage-data/delete-data#delete_documents
    fun removeSongMeta(
        songMeta: SongMeta,
        resultListener: (List<SongMeta>)->Unit
    ) {
        // XXX Write me.  Make sure you delete the correct entry.  What uniquely identifies a songMeta?
        db.collection(rootCollection).document(songMeta.firestoreID)
            .delete()
            .addOnSuccessListener {
                Log.d(javaClass.simpleName, "Document successfully deleted")
                fetchSongMeta(resultListener)
            }
            .addOnFailureListener { e ->
                Log.w(javaClass.simpleName, "Error deleting document", e)
            }
    }

    fun updateSongMeta(
        songMeta: SongMeta,
        field: String,
        newName: Any,
        resultListener: (List<SongMeta>)->Unit
    ) {
        // XXX Write me.  Make sure you delete the correct entry.  What uniquely identifies a songMeta?
        db.collection(rootCollection).document(songMeta.firestoreID)
            .update(field, newName)
            .addOnSuccessListener {
                Log.d(javaClass.simpleName, "Document successfully renamed")
                fetchSongMeta(resultListener)
            }
            .addOnFailureListener { e ->
                Log.w(javaClass.simpleName, "Error renaming document", e)
            }
    }

    //Retrieve user profile meta, or creates it if it does not exist
    fun fetchProfileMeta(user: User, createUser: Boolean = true,
        resultListener: (ProfileMeta) -> Unit
    ) {
        db.collection(userCollection).document(user.uid).get()
            .addOnSuccessListener { result ->
                Log.d(javaClass.simpleName, "user fetch ${result.id}")
                // NB: This is done on a background thread
                if (result.exists()) {
                    val profile = result.toObject(ProfileMeta::class.java)
                    if (profile != null) {
                        resultListener(profile)
                    } else {
                        Log.d(javaClass.simpleName, "ProfileMeta fetch FAILED")
                    }
                } else if (createUser){
                    Log.d(javaClass.simpleName, "ProfileMeta fetch FAILED")
                    createProfileMeta(user, resultListener)
                }
            }
            .addOnFailureListener {
                Log.d(javaClass.simpleName, "users fetch FAILED ", it)
            }


    }

    //Creates a profile meta for the user if it does not exist
    private fun createProfileMeta(
        user: User,
        resultListener: (ProfileMeta)->Unit
    ) {
        val profileMeta = ProfileMeta(
            name = user.name,
            email = user.email,
            uid = user.uid,
        )
        db.collection(userCollection)
            .document(user.uid)
            .set(profileMeta)
            .addOnSuccessListener {
                Log.d(
                    javaClass.simpleName,
                    "Profile created \"${profileMeta.name}\", id \"${profileMeta.uid}\""
                )
                resultListener(profileMeta)
            }
            .addOnFailureListener { e ->
                Log.d(javaClass.simpleName, "User create FAILED \"${profileMeta.uid}\"")
                Log.w(javaClass.simpleName, "Error ", e)

            }
    }

    fun updateProfileMeta(
        profileMeta: ProfileMeta,
        field: String,
        newName: Any,
        resultListener: (ProfileMeta)->Unit
    ) {
        // XXX Write me.  Make sure you delete the correct entry.  What uniquely identifies a songMeta?
        db.collection(userCollection).document(profileMeta.uid)
            .update(field, newName)
            .addOnSuccessListener {
                Log.d(javaClass.simpleName, "Document successfully renamed")
                fetchProfileMeta(User(profileMeta.name, profileMeta.email, profileMeta.uid), resultListener = resultListener)
            }
            .addOnFailureListener { e ->
                Log.w(javaClass.simpleName, "Error renaming document", e)
            }
    }

    fun fetchFriends(
        resultListener: (List<ProfileMeta>) -> Unit
    ) {
        val query = db.collection(userCollection)
        query
            .get()
            .addOnSuccessListener { result ->
                Log.d(javaClass.simpleName, "profiles fetch ${result!!.documents.size}")
                // NB: This is done on a background thread
                resultListener(result.documents.mapNotNull {
                    it.toObject(ProfileMeta::class.java)
                })
            }
            .addOnFailureListener {
                Log.d(javaClass.simpleName, "profiles fetch FAILED ", it)
                resultListener(listOf())
            }

    }

}