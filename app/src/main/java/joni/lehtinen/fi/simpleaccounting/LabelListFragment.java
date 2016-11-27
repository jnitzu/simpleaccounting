package joni.lehtinen.fi.simpleaccounting;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import java.util.ArrayList;
import java.util.List;

import joni.lehtinen.fi.simpleaccounting.Dialog.CreateLabelDialog;


/**
 * Label management fragment
 */
public class LabelListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EXTRA_EDIT_LABEL_URI = "joni.lehtinen.fi.simpleaccounting.edit_label_uri";

    private static final String[] PROJECTION = new String[] {
            AccountProvider.PAYMENT_LABEL.ID.toString(),
            AccountProvider.PAYMENT_LABEL.LABEL.toString()};

    private SimpleCursorAdapter mAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if(actionBar != null)
            actionBar.setTitle(R.string.title_manage_labels);

        String[] dataColumns = {
                AccountProvider.PAYMENT_LABEL.LABEL.toString() };

        int[] viewIDs = {
                R.id.list_item_label_title };

        mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.list_item_label, null, dataColumns, viewIDs, 0);

        setListAdapter(mAdapter);

        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

                CheckBox checkBox = (CheckBox) getListView().getChildAt(position).findViewById(R.id.list_item_label_checked);
                checkBox.setChecked(checked);

                // This is used to display edit button when there is only one list item selected
                if (getListView().getCheckedItemCount() < 3) {
                    mode.invalidate();
                }

                mode.setTitle(getListView().getCheckedItemCount() + " selected");

            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

                Cursor cursor = (Cursor) getListAdapter().getItem(0);

                SparseBooleanArray checked = getListView().getCheckedItemPositions();

                switch (item.getItemId()) {
                    case R.id.contextual_edit:

                        Uri editedRow = null;

                        for (int offset = 0; offset < getListView().getCount(); offset++) {
                            if (checked.get(offset)) {
                                cursor.moveToFirst();
                                cursor.move(offset);
                                editedRow = ContentUris.withAppendedId(AccountProvider.CONTENT_URI_PAYMENT_LABEL, cursor.getLong(cursor.getColumnIndex(AccountProvider.PAYMENT_LABEL.ID.toString())));
                                break;
                            }
                        }

                        // Create and show the dialog.
                        CreateLabelDialog dialogFragment = new CreateLabelDialog();

                        Bundle bundle = new Bundle();
                        bundle.putParcelable(EXTRA_EDIT_LABEL_URI,editedRow);
                        dialogFragment.setArguments(bundle);

                        dialogFragment.show(getFragmentManager(), "dialog");

                        return true;
                    case R.id.contextual_remove:
                        List<Uri> selectedUris = new ArrayList<>();

                        for (int offset = 0; offset < getListView().getCount(); offset++) {
                            if (checked.get(offset)) {
                                cursor.moveToFirst();
                                cursor.move(offset);
                                selectedUris.add(ContentUris.withAppendedId(AccountProvider.CONTENT_URI_PAYMENT_LABEL, cursor.getLong(cursor.getColumnIndex(AccountProvider.PAYMENT_LABEL.ID.toString()))));
                            }
                        }

                        for (Uri selectedUri : selectedUris) {
                            getActivity().getContentResolver().delete(selectedUri, null, null);
                        }

                        getListView().clearChoices();
                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                ListView listView = getListView();
                for (int i = 0; i < listView.getCount(); i++) {
                    listView.getChildAt(i).findViewById(R.id.list_item_label_checked).setVisibility(View.VISIBLE);
                }
                mode.getMenuInflater().inflate(R.menu.fragment_account_list_contextual, menu);
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                getListView().clearChoices();
                ListView listView = getListView();
                for (int i = 0; i < listView.getCount(); i++) {
                    CheckBox checkBox = (CheckBox) getListView().getChildAt(i).findViewById(R.id.list_item_label_checked);
                    checkBox.setChecked(false);
                    checkBox.setVisibility(View.GONE);
                }
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                MenuItem menuItem = menu.findItem(R.id.contextual_edit);
                boolean changed = menuItem.isVisible();
                menuItem.setVisible(getListView().getCheckedItemCount() <= 1);
                return changed ^ menuItem.isVisible();
            }
        });

        getLoaderManager().initLoader(0, null, this);
    }



    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), AccountProvider.CONTENT_URI_PAYMENT_LABEL, PROJECTION, AccountProvider.PAYMENT_LABEL.ID + " IS NOT 0", null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}
