package joni.lehtinen.fi.simpleaccounting;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Account management fragment
 */
public class AccountListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int EDIT_ACCOUNT_RESULT = 5;

    private static final String[] PROJECTION = new String[]{
            AccountProvider.ACCOUNT.ID.fullName(),
            AccountProvider.ACCOUNT.TITLE.fullName(),
            AccountProvider.ACCOUNT.BALANCE.fullName(),
            AccountProvider.ACCOUNT.TYPE_ID.fullName(),
            AccountProvider.ACCOUNT_TYPE.TYPE.fullName(),
            AccountProvider.ACCOUNT.DESCRIPTION.fullName()};

    private AccountCursorAdapter mAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if(actionBar != null)
            actionBar.setTitle(R.string.title_accounts);

        String[] dataColumns = {
                AccountProvider.ACCOUNT.TITLE.toString(),
                AccountProvider.ACCOUNT.BALANCE.toString(),
                AccountProvider.ACCOUNT.DESCRIPTION.toString(),
                AccountProvider.ACCOUNT_TYPE.TYPE.toString() };

        int[] viewIDs = {
                R.id.list_item_account_title,
                R.id.list_item_account_balance,
                R.id.list_item_account_description,
                R.id.list_item_account_type };

        mAdapter = new AccountCursorAdapter(getActivity(), R.layout.list_item_account, null, dataColumns, viewIDs, 0);

        setEmptyText(getString(R.string.no_accounts_created));
        setListAdapter(mAdapter);
        setListShown(false);

        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

                CheckBox checkBox = (CheckBox) getListView().getChildAt(position).findViewById(R.id.list_item_account_checked);
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
                                editedRow = ContentUris.withAppendedId(AccountProvider.CONTENT_URI_ACCOUNT, cursor.getLong(cursor.getColumnIndex(AccountProvider.ACCOUNT.ID.toString())));
                                break;
                            }
                        }

                        Intent intent = new Intent(getActivity(), CreateActivity.class);
                        intent.putExtra(CreateActivity.EXTRA_FRAGMENT_TYPE, CreateActivity.ADD_FRAGMENT_TYPE.ACCOUNT);
                        intent.putExtra(CreateActivity.EXTRA_EDIT_URI, editedRow);
                        startActivityForResult(intent, EDIT_ACCOUNT_RESULT);
                        return true;
                    case R.id.contextual_remove:
                        List<Uri> selectedUris = new ArrayList<>();

                        for (int offset = 0; offset < getListView().getCount(); offset++) {
                            if (checked.get(offset)) {
                                cursor.moveToFirst();
                                cursor.move(offset);
                                selectedUris.add(ContentUris.withAppendedId(AccountProvider.CONTENT_URI_ACCOUNT, cursor.getLong(cursor.getColumnIndex(AccountProvider.ACCOUNT.ID.toString()))));
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
                    listView.getChildAt(i).findViewById(R.id.list_item_account_checked).setVisibility(View.VISIBLE);
                }
                mode.getMenuInflater().inflate(R.menu.fragment_account_list_contextual, menu);
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                getListView().clearChoices();
                ListView listView = getListView();
                for (int i = 0; i < listView.getCount(); i++) {
                    CheckBox checkBox = (CheckBox) getListView().getChildAt(i).findViewById(R.id.list_item_account_checked);
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

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Fragment fragment = new AccountFragment();

                Cursor c = mAdapter.getCursor();
                c.moveToFirst();
                c.move(position);

                Bundle bundle = new Bundle();
                bundle.putInt(AccountFragment.ACCOUNT_ID, c.getInt(0));
                bundle.putString(AccountFragment.ACCOUNT_TITLE, c.getString(1));
                bundle.putString(AccountFragment.ACCOUNT_TYPE_TITLE, c.getString(4));
                fragment.setArguments(bundle);

                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case EDIT_ACCOUNT_RESULT:
                if (resultCode == Activity.RESULT_OK) {
                    Log.d("resultCode","AccountListFragment");
                }
                break;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), AccountProvider.CONTENT_URI_ACCOUNT, PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);

        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    /**
     * Custom cursor adapter to show checkboxes on contextual action mode
     */
    private class AccountCursorAdapter extends SimpleCursorAdapter {
        public AccountCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            CheckBox checkBox = (CheckBox)view.findViewById(R.id.list_item_account_checked);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    getListView().setItemChecked(position,isChecked);
                }
            });
            return view;
        }
    }
}
