package de.daniel.mobilepauker2.data.saving

interface SaveAsCallback {
    fun okClicked(fileName: String)
    fun cancelClicked()
}