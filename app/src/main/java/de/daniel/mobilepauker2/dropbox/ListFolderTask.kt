package de.daniel.mobilepauker2.dropbox

import com.dropbox.core.DbxException
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.ListFolderResult
import de.daniel.mobilepauker2.utils.CoroutinesAsyncTask

/**
 * Async task to list items in a folder
 */
class ListFolderTask internal constructor(
    private val mDbxClient: DbxClientV2,
    private val mCallback: Callback
) : CoroutinesAsyncTask<String?, Void?, ListFolderResult?>("ListFolderTask") {
    private var mException: DbxException? = null

    interface Callback {
        fun onDataLoaded(result: ListFolderResult?)
        fun onError(e: DbxException?)
    }

    override fun onPostExecute(result: ListFolderResult?) {
        super.onPostExecute(result)
        if (mException != null) {
            mCallback.onError(mException)
        } else {
            mCallback.onDataLoaded(result)
        }
    }

    override fun doInBackground(vararg params: String?): ListFolderResult? {
        mException = try {
            return mDbxClient.files()
                .listFolderBuilder(params[0])
                .withRecursive(false)
                .withIncludeDeleted(true)
                .start()
        } catch (e: DbxException) {
            e
        }
        return null
    }
}