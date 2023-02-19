package wseemann.media.romote.fragment

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment

import wseemann.media.romote.R

class SearchDialog : DialogFragment() {

    companion object {
        var listener: SearchDialogListener? = null
        fun newInstance(activity: Activity): SearchDialog {
            listener = activity as SearchDialogListener
            return SearchDialog()
        }
    }

    interface SearchDialogListener {
        fun onSearch(searchText: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        val inflater = activity!!.layoutInflater

        var view: View = inflater.inflate(R.layout.dialog_fragment_search, null)
        var searchEditText: EditText = view.findViewById<EditText>(R.id.ip_address_text)
        var cancelButton: Button = view.findViewById<Button>(R.id.cancel_button)
        var searchButton: Button = view.findViewById<Button>(R.id.connect_button)

        cancelButton.setOnClickListener {
            dismiss()
        }

        searchButton.setOnClickListener {
            var searchText: EditText = searchEditText
            var searchListener: SearchDialogListener? = listener

            if (searchListener != null) {
                searchListener.onSearch(searchText.text.toString())
            }

            dismiss()
        }

        var builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        builder.setView(view)
        builder.setTitle(getString(R.string.action_search))
        //builder.setMessage(getString(R.string.search_help))

        return builder.create()
    }
}
