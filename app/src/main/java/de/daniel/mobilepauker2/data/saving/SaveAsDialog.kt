package de.daniel.mobilepauker2.data.saving

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.data.DataManager
import javax.inject.Inject

class SaveAsDialog(private val saveAsCallback: SaveAsCallback) : DialogFragment() {
    private lateinit var textField: EditText
    private lateinit var bOK: Button
    private lateinit var bCancel: Button
    private lateinit var errorHint: TextView

    @Inject
    lateinit var dataManager: DataManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (context as PaukerApplication).applicationSingletonComponent.inject(this)

        initView(view)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog!!.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        return inflater.inflate(R.layout.give_lesson_name_dialog, null, false)
    }

    private fun initView(view: View) {
        textField = view.findViewById(R.id.eTGiveLessonName)
        addTextwatcher(textField)

        bCancel = view.findViewById(R.id.bCancel)
        bCancel.setOnClickListener { saveAsCallback.cancelClicked() }

        bOK = view.findViewById(R.id.bOK)
        bOK.setOnClickListener { saveAsCallback.okClicked(textField.text.toString()) }

        errorHint = view.findViewById(R.id.tFileExistingHint)
    }

    private fun addTextwatcher(textField: EditText) {
        textField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                var newName = s.toString()
                val isEmptyString = newName.isNotEmpty()

                if (!newName.endsWith(".pau.gz")) newName = "$newName.pau.gz"

                val isValidName: Boolean = dataManager.isNameValid(newName)
                val isExisting: Boolean = dataManager.isFileExisting(newName)

                errorHint.visibility = if (isExisting) View.VISIBLE else View.GONE

                bOK.isEnabled = (isEmptyString && isValidName && !isExisting)
            }
        })
    }
}