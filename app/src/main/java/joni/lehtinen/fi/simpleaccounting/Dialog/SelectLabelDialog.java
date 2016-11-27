package joni.lehtinen.fi.simpleaccounting.Dialog;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import joni.lehtinen.fi.simpleaccounting.AccountProvider;
import joni.lehtinen.fi.simpleaccounting.R;

/**
 * Label selection dialog. Used as popup selection menu.
 * To get selected label, fragment that calls this must implement LabelResult interface.
 */
public class SelectLabelDialog extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {


    private static final String[] PROJECTION = new String[] {AccountProvider.PAYMENT_LABEL.ID.toString(),AccountProvider.PAYMENT_LABEL.LABEL.toString()};

    private SimpleCursorAdapter mAdapter;
    private ListView mItems;

    private OnLabelSelectedListener mCallback;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_select_label, container, false);
        mItems = (ListView)view.findViewById(R.id.list_view_dialog_label_select);

        String[] dataColumns = { AccountProvider.PAYMENT_LABEL.LABEL.toString() };
        int[] viewIDs = { R.id.list_item_label_title };

        mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.list_item_label, null, dataColumns, viewIDs, 0);

        mItems.setAdapter(mAdapter);

        mItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor c = (Cursor) mAdapter.getItem(position);
                int rowId = c.getInt(c.getColumnIndex(AccountProvider.PAYMENT_LABEL.ID.toString()));
                mCallback.onLabelSelected(ContentUris.withAppendedId(AccountProvider.CONTENT_URI_PAYMENT_LABEL, rowId));

                c.close();
                dismiss();
            }
        });

        getLoaderManager().initLoader(1, null, this);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnLabelSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnLabelSelectedListener");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), AccountProvider.CONTENT_URI_PAYMENT_LABEL, PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    public interface OnLabelSelectedListener {
        void onLabelSelected(Uri uri);
    }
}
