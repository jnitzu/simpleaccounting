package joni.lehtinen.fi.simpleaccounting.Dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import joni.lehtinen.fi.simpleaccounting.AccountProvider;
import joni.lehtinen.fi.simpleaccounting.LabelListFragment;
import joni.lehtinen.fi.simpleaccounting.R;

/**
 * Label create/edit dialog
 */
public class CreateLabelDialog extends DialogFragment {

    private OnLabelCreatedListener mCallback;
    private EditText mLabelName;
    private Uri mEditUri;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if(getArguments() != null)
            mEditUri = getArguments().getParcelable(LabelListFragment.EXTRA_EDIT_LABEL_URI);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_create_label, null);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.create_label_toolbar);
        toolbar.setTitle(mEditUri != null ? R.string.dialog_edit_label_title : R.string.dialog_create_label_title);

        mLabelName = (EditText)view.findViewById(R.id.edit_text_create_label);

        builder.setView(view)
            .setPositiveButton(mEditUri != null ? R.string.save : R.string.create, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(AccountProvider.PAYMENT_LABEL.LABEL.toString(), mLabelName.getText().toString());
                    if (mEditUri != null) {
                        getActivity().getContentResolver().update(mEditUri, contentValues, null, null);
                    } else {
                        Uri insertedRow = getActivity().getContentResolver().insert(AccountProvider.CONTENT_URI_PAYMENT_LABEL, contentValues);
                        mCallback.onLabelCreated(insertedRow);
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null);

        if(mEditUri != null){
            Cursor cursor = getActivity().getContentResolver().query(
                    mEditUri,
                    new String[]{
                            AccountProvider.PAYMENT_LABEL.ID.toString(),              // 0
                            AccountProvider.PAYMENT_LABEL.LABEL.toString()},          // 1
                    null,
                    null,
                    null);

            if(cursor != null){
                cursor.moveToFirst();
                mLabelName.setText(cursor.getString(1));
            }

        }

        Dialog dialog = builder.create();
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Only in create mode does activity have to implement OnLabelCreatedListener
        if(activity instanceof OnLabelCreatedListener) {
            mCallback = (OnLabelCreatedListener) activity;
        }
    }

    public interface OnLabelCreatedListener {
        void onLabelCreated(Uri uri);
    }
}
