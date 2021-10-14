package de.daniel.mobilepauker2.dropbox

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dropbox.core.DbxException
import com.dropbox.core.v2.files.*
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

    private val _errorLiveData = MutableLiveData<Exception>()
    val errorLiveData: LiveData<Exception> = _errorLiveData

    private val _downloadList = MutableLiveData<List<FileMetadata>>()
    val downloadList: LiveData<List<FileMetadata>> = _downloadList

    var downloadSize = 0L

    fun loadDataFromDropbox(
        files: List<File>,
        callback: ListFolderTask.Callback,
        cachedFiles: List<File>?,
        cachedCursor: String?
    ) {
        var listFolderTask: ListFolderTask? = null
        listFolderTask = ListFolderTask(DropboxClientFactory.client, cachedCursor,
            object : ListFolderTask.Callback {
                override fun onDataLoaded(listFolderResult: ListFolderResult?) {
                    if (listFolderResult == null) onError(DbxException("Result is null"))
                    else {
                        callback.onDataLoaded(listFolderResult)
                        compareFiles(files, getEntries(listFolderResult), cachedFiles)
                    }
                    removeTask(listFolderTask)
                }

                override fun onError(e: DbxException?) {
                    Log.d(
                        "LessonImportActivity::loadData::onError",
                        "Error loading Files: " + e?.message
                    )
                    _errorLiveData.postValue(e)
                }
            })
        listFolderTask.execute(Constants.DROPBOX_PATH)
        addTask(listFolderTask)
    }

    fun cancelTasks() {
        for (task in tasks) {
            if (task.status != CoroutinesAsyncTask.Status.FINISHED) {
                task.cancel(false)
            }
        }
        tasks.clear()
    }

    fun downloadFiles(list: List<FileMetadata>, callback: DownloadFileTask.Callback) {
        var task: DownloadFileTask? = null
        task = DownloadFileTask(DropboxClientFactory.client, object : DownloadFileTask.Callback {
            override fun onDownloadStartet() {
                callback.onDownloadStartet()
            }

            override fun onProgressUpdate(metadata: FileMetadata) {
                callback.onProgressUpdate(metadata)
            }

            override fun onDownloadComplete(result: List<File>) {
                callback.onDownloadComplete(result)
                removeTask(task)
            }

            override fun onError(e: Exception?) {
                Log.e(
                    "LessonImportActivity::downloadFiles",
                    "Failed to download file.", e
                )
                _errorLiveData.postValue(e)
            }

        })
        task.execute(*list.toTypedArray())
        addTask(task)
    }

    private fun uploadFiles(list: List<File>): UploadFileTask {
        var task: UploadFileTask? = null
        task = UploadFileTask(DropboxClientFactory.client, object : UploadFileTask.Callback {
            override fun onUploadComplete(result: List<Metadata?>?) {
                removeTask(task)
            }

            override fun onError(e: Exception?) {
                _errorLiveData.postValue(e)
            }
        })
        task.execute(*list.toTypedArray())
        addTask(task)
        return task
    }

    private fun deleteFilesOnDB(list: List<File>) {
        var task: DeleteFileTask? = null
        task = DeleteFileTask(DropboxClientFactory.client, object : DeleteFileTask.Callback {
            override fun onDeleteComplete(result: List<DeleteResult>) {
                removeTask(task)
            }

            override fun onError(e: Exception?) {
                _errorLiveData.postValue(e)
            }

        })
        task.execute(*list.toTypedArray())
        addTask(task)
    }

    private fun deleteFilesOnPhone(list: List<File>) {
        list.forEach { it.delete() }
    }

    private fun addTask(task: CoroutinesAsyncTask<*, *, *>) {
        tasks.add(task)
        _tasksLiveData.postValue(tasks)
    }

    private fun removeTask(task: CoroutinesAsyncTask<*, *, *>?) {
        tasks.remove(task)
        _tasksLiveData.postValue(tasks)
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
        uploadFiles(filesToUpload)
        deleteFilesOnPhone(filesToDeleteLocal)
        deleteFilesOnDB(filesToDeleteServer)
    }

    private fun List<Metadata>.find(file: File): Metadata? = find { it.name == file.name }

    private fun List<File>.find(metadata: FileMetadata): File? = find { it.name == metadata.name }

    private fun List<File>.find(file: File): File? = find { it.name == file.name }
}