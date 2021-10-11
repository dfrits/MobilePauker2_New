package de.daniel.mobilepauker2.dropbox

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dropbox.core.DbxException
import com.dropbox.core.v2.files.DeletedMetadata
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.ListFolderResult
import com.dropbox.core.v2.files.Metadata
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.CoroutinesAsyncTask
import de.daniel.mobilepauker2.utils.Log
import java.io.File
import javax.inject.Inject

class SyncDialogViewModel @Inject constructor(private val dataManager: DataManager) {
    private val tasks: MutableList<CoroutinesAsyncTask<*, *, *>> = mutableListOf()
    private val _tasksLiveData: MutableLiveData<List<CoroutinesAsyncTask<*, *, *>>> =
        MutableLiveData()
    val tasksLiveData: LiveData<List<CoroutinesAsyncTask<*, *, *>>> = _tasksLiveData

    private val _errorLiveData = MutableLiveData<DbxException>()
    val errorLiveData: LiveData<DbxException> = _errorLiveData

    private val _uploadList = MutableLiveData<List<File>>()
    val uploadList: LiveData<List<File>> = _uploadList

    private val _downloadList = MutableLiveData<List<FileMetadata>>()
    val downloadList: LiveData<List<FileMetadata>> = _downloadList

    private val _deleteLocalList = MutableLiveData<List<File>>()
    val deleteLocalList: LiveData<List<File>> = _deleteLocalList

    private val _deleteServerList = MutableLiveData<List<File>>()
    val deleteServerList: LiveData<List<File>> = _deleteServerList

    var downloadSize = 0L

    fun loadDataFromDropbox(files: List<File>) {
        val listFolderTask =
            ListFolderTask(DropboxClientFactory.client, getCachedCursor(),
                object : ListFolderTask.Callback {
                    override fun onDataLoaded(listFolderResult: ListFolderResult?) {
                        if (listFolderResult == null) onError(DbxException("Result is null"))
                        else {
                            compareFiles(files, getEntries(listFolderResult), getCachedList())
                        }
                    }

                    override fun onError(e: DbxException?) {
                        e?.let {
                            Log.d(
                                "LessonImportActivity::loadData::onError",
                                "Error loading Files: " + e.message
                            )
                            _errorLiveData.postValue(e)
                        }
                    }
                })
        listFolderTask.execute(Constants.DROPBOX_PATH)
        addTask(listFolderTask)
    }

    fun addTask(task: CoroutinesAsyncTask<*, *, *>) {
        tasks.add(task)
        _tasksLiveData.postValue(tasks)
    }

    fun removeTask(task: CoroutinesAsyncTask<*, *, *>) {
        tasks.remove(task)
        _tasksLiveData.postValue(tasks)
    }

    fun cancelTasks() {
        for (task in tasks) {
            if (task.status != CoroutinesAsyncTask.Status.FINISHED) {
                task.cancel(false)
                tasks.remove(task)
            }
        }
        _tasksLiveData.postValue(tasks)
    }

    fun downloadFiles(
        list: List<FileMetadata>,
        callback: DownloadFileTask.Callback
    ): DownloadFileTask {
        val task = DownloadFileTask(DropboxClientFactory.client, callback)
        task.execute(*list.toTypedArray())
        addTask(task)
        return task
    }

    fun uploadFiles(list: List<File>, callback: UploadFileTask.Callback): UploadFileTask {
        val task = UploadFileTask(DropboxClientFactory.client, callback)
        task.execute(*list.toTypedArray())
        addTask(task)
        return task
    }

    private fun getCachedCursor(): String? { // TODO
        return null
    }

    private fun getEntries(listFolderResult: ListFolderResult): List<Metadata> {
        var result = listFolderResult
        val dbFiles = mutableListOf<Metadata>()

        while (true) {
            val entries: List<Metadata> = result.entries
            for (entry in entries) {
                if (dataManager.isNameValid(entry.name) && entry !is DeletedMetadata) {
                    dbFiles.add(entry)
                }
            }
            if (!result.hasMore) break

            try {
                result = DropboxClientFactory.client.files()
                    .listFolderContinue(result.cursor)
            } catch (e: DbxException) {
                e.printStackTrace()
            }
        }
        return dbFiles.toList()
    }

    private fun getCachedList(): List<File>? { // TODO
        return null
    }

    private fun compareFiles(
        lokalFiles: List<File>,
        dbFiles: List<Metadata>,
        cachedFiles: List<File>?
    ) {
        val filesToUpload = mutableListOf<File>()
        val filesToDownload = mutableListOf<FileMetadata>()
        val filesToDeleteLocal = mutableListOf<File>()
        val filesToDeleteServer = mutableListOf<File>()

        lokalFiles.forEach { localFile ->
            val dbFile = dbFiles.find(localFile) as FileMetadata?
            val cachedFile = cachedFiles?.find(localFile)
            if (dbFile == null && cachedFile == null) {
                filesToUpload.add(localFile)
            } else if (dbFile != null) {
                val clientModified = dbFile.clientModified.time
                val cachedModified = cachedFile?.lastModified() ?: clientModified
                if (clientModified == cachedModified && localFile.lastModified() > clientModified) {
                    filesToUpload.add(localFile)
                }
            }
        }

        dbFiles.forEach { dbFile ->
            val localFile = lokalFiles.find(dbFile as FileMetadata)
            val cachedFile = cachedFiles?.find(dbFile)
            if (localFile == null && cachedFile == null) {
                filesToDownload.add(dbFile)
                downloadSize += dbFile.size
            } else if (localFile != null) {
                val localModified = localFile.lastModified()
                val cachedModified = cachedFile?.lastModified() ?: localModified
                if (localModified == cachedModified
                    && localModified < dbFile.clientModified.time
                ) {
                    filesToUpload.add(localFile)
                }
            }
        }

        cachedFiles?.forEach { cachedFile ->
            val localFile = lokalFiles.find(cachedFile)
            val dbFile = dbFiles.find(cachedFile)

            if (localFile == null && dbFile != null) {
                filesToDeleteServer.add(cachedFile)
            } else if (localFile != null && dbFile == null) {
                filesToDeleteLocal.add(cachedFile)
            }
        }

        _downloadList.postValue(filesToDownload)
        _uploadList.postValue(filesToUpload)
        _deleteLocalList.postValue(filesToDeleteLocal)
        _deleteServerList.postValue(filesToDeleteServer)
    }

    private fun List<Metadata>.find(file: File): Metadata? = find { it.name == file.name }

    private fun List<File>.find(metadata: FileMetadata): File? = find { it.name == metadata.name }

    private fun List<File>.find(file: File): File? = find { it.name == file.name }
}