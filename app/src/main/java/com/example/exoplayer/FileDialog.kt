package com.example.exoplayer

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Environment
import android.util.Log

import androidx.appcompat.app.AlertDialog

import java.io.File
import java.io.FilenameFilter
import java.util.ArrayList

internal class FileDialog @JvmOverloads constructor(
    private val activity: Activity,
    initialPath: File,
    fileEndsWith: String? = null
) {
    private val TAG = javaClass.name
    private var fileList: Array<String>? = null
    private var currentPath: File? = null
    private val fileListenerList = ListenerList<FileSelectedListener>()
    private val dirListenerList = ListenerList<DirectorySelectedListener>()
    private var selectDirectoryOption: Boolean = false
    private var fileEndsWith: String? = null

    interface FileSelectedListener {
        fun fileSelected(file: File?)
    }

    interface DirectorySelectedListener {
        fun directorySelected(directory: File?)
    }

    init {
        var initialPath = initialPath
        setFileEndsWith(fileEndsWith)
        if (!initialPath.exists()) initialPath = Environment.getExternalStorageDirectory()
        loadFileList(initialPath)
    }

    /**
     * @return file dialog
     */
    fun createFileDialog(): Dialog? {
        var dialog: Dialog? = null
        val builder = AlertDialog.Builder(activity)

        builder.setTitle(currentPath!!.path)
        if (selectDirectoryOption) {
            builder.setPositiveButton("Select directory") { dialog, which ->
                Log.d(TAG, currentPath!!.path)
                fireDirectorySelectedEvent(currentPath)
            }
        }

        builder.setItems(fileList) { dialog, which ->
            val fileChosen = fileList!![which]
            val chosenFile = getChosenFile(fileChosen)
            if (chosenFile!!.isDirectory) {
                loadFileList(chosenFile)
                dialog.cancel()
                dialog.dismiss()
                showDialog()
            } else
                fireFileSelectedEvent(chosenFile)
        }

        dialog = builder.show()
        return dialog
    }


    fun addFileListener(listener: FileSelectedListener) {
        fileListenerList.add(listener)
    }

    fun removeFileListener(listener: FileSelectedListener) {
        fileListenerList.remove(listener)
    }

    fun setSelectDirectoryOption(selectDirectoryOption: Boolean) {
        this.selectDirectoryOption = selectDirectoryOption
    }

    fun addDirectoryListener(listener: DirectorySelectedListener) {
        dirListenerList.add(listener)
    }

    fun removeDirectoryListener(listener: DirectorySelectedListener) {
        dirListenerList.remove(listener)
    }

    /**
     * Show file dialog
     */
    fun showDialog() {
        createFileDialog()!!.show()
    }

    private fun fireFileSelectedEvent(file: File?) {
        fileListenerList.fireEvent(object : ListenerList.FireHandler<FileSelectedListener> {
            override fun fireEvent(listener: FileSelectedListener) {
                listener.fileSelected(file)
            }
        })
    }

    private fun fireDirectorySelectedEvent(directory: File?) {
        dirListenerList.fireEvent(object : ListenerList.FireHandler<DirectorySelectedListener> {
            override fun fireEvent(listener: DirectorySelectedListener) {
                listener.directorySelected(directory)
            }
        })
    }

    private fun loadFileList(path: File) {
        this.currentPath = path
        val r = ArrayList<String>()
        if (path.exists()) {
            if (path.parentFile != null) r.add(PARENT_DIR)
            val filter = FilenameFilter { dir, filename ->
                val sel = File(dir, filename)
                if (!sel.canRead()) return@FilenameFilter false
                if (selectDirectoryOption)
                    sel.isDirectory
                else {
                    val endsWith =
                        if (fileEndsWith != null) filename.toLowerCase().endsWith(fileEndsWith!!) else true
                    endsWith || sel.isDirectory
                }
            }
            val fileList1 = path.list(filter)
            for (file in fileList1!!) {
                r.add(file)
            }
        }
        fileList = r.toTypedArray()
    }

    private fun getChosenFile(fileChosen: String): File? {
        return if (fileChosen == PARENT_DIR)
            currentPath!!.parentFile
        else
            File(currentPath, fileChosen)
    }

    private fun setFileEndsWith(fileEndsWith: String?) {
        this.fileEndsWith = fileEndsWith?.toLowerCase() ?: fileEndsWith
    }

    companion object {
        private val PARENT_DIR = ".."
    }
}

/**
 * @param activity
 * @param initialPath
 */

internal class ListenerList<L> {
    private val listenerList = ArrayList<L>()

    interface FireHandler<L> {
        fun fireEvent(listener: L)
    }

    fun add(listener: L) {
        listenerList.add(listener)
    }

    fun fireEvent(fireHandler: FireHandler<L>) {
        val copy = ArrayList(listenerList)
        for (l in copy) {
            fireHandler.fireEvent(l)
        }
    }

    fun remove(listener: L) {
        listenerList.remove(listener)
    }

    fun getListenerList(): List<L> {
        return listenerList
    }
}
