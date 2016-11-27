package joni.lehtinen.fi.simpleaccounting;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

/**
 * Account create/edit fragment
 */
public class CreateAccountFragment extends Fragment implements AddItem, LoaderManager.LoaderCallbacks<Cursor> {

    private EditText mTitle;
    private EditText mBalance;
    private EditText mDescription;
    private Spinner mAccountType;

    private Uri mEditUri;
    private Long mSelectedAccountType;

    private SimpleCursorAdapter mAdapter;

    private static final String[] PROJECTION = new String[]{
            AccountProvider.ACCOUNT_TYPE.ID.toString(),
            AccountProvider.ACCOUNT_TYPE.TYPE.toString()};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_account, container, false);

        if(getArguments() != null)
            mEditUri = getArguments().getParcelable(CreateActivity.EXTRA_EDIT_URI);

        mTitle = (EditText)view.findViewById(R.id.edit_text_account_title);
        mBalance = (EditText)view.findViewById(R.id.edit_text_account_balance);
        mDescription = (EditText)view.findViewById(R.id.edit_text_account_description);
        mAccountType = (Spinner)view.findViewById(R.id.spinner_account_type);

        mAccountType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Cursor c = mAdapter.getCursor();
                c.moveToPosition(position);
                mSelectedAccountType = c.getLong(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        String[] dataColumns = { AccountProvider.ACCOUNT_TYPE.TYPE.toString() };

        int[] viewIDs = { android.R.id.text1 };

        mAdapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_spinner_item, null, dataColumns, viewIDs, 0);
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mAccountType.setAdapter(mAdapter);

        if(mEditUri != null){
            Cursor cursor = getActivity().getContentResolver().query(
                    mEditUri,
                    new String[]{
                            AccountProvider.ACCOUNT.ID.fullName(),              // 0
                            AccountProvider.ACCOUNT.TITLE.fullName(),           // 1
                            AccountProvider.ACCOUNT.BALANCE.fullName(),         // 2
                            AccountProvider.ACCOUNT.TYPE_ID.fullName(),         // 3
                            AccountProvider.ACCOUNT.DESCRIPTION.fullName()},    // 4
                    null,
                    null,
                    null);

            if(cursor != null){
                cursor.moveToFirst();
                mTitle.setText(cursor.getString(1));
                mBalance.setText(cursor.getString(2));
                mDescription.setText(cursor.getString(4));
                mSelectedAccountType = cursor.getLong(3);
            }

        }

        getLoaderManager().initLoader(5, null, this);

        return view;
    }

    @Override
    public void saveToDatabase() {

        ContentValues contentValues = new ContentValues();

        contentValues.put(AccountProvider.ACCOUNT.TITLE.toString(), mTitle.getText().toString());
        contentValues.put(AccountProvider.ACCOUNT.BALANCE.toString(), Double.parseDouble(mBalance.getText().toString()));
        contentValues.put(AccountProvider.ACCOUNT.TYPE_ID.toString(), mSelectedAccountType);
        contentValues.put(AccountProvider.ACCOUNT.DESCRIPTION.toString(), mDescription.getText().toString());

        if(mEditUri != null){
            getActivity().getContentResolver().update(mEditUri,contentValues,null,null);
        } else {
            getActivity().getContentResolver().insert(AccountProvider.CONTENT_URI_ACCOUNT,contentValues);
        }
    }

    @Override
    public boolean isFormValid() {
        boolean emptyTitle = mTitle.getText().toString().isEmpty();
        boolean emptyBalance = mBalance.getText().toString().isEmpty();

        if(emptyTitle || emptyBalance){
            String emptyFields = Utility.createIsEmptyString(
                    new boolean[]{emptyTitle,emptyBalance},
                    new String[]{getString(R.string.title),getString(R.string.account_balance)},
                    getString(R.string.and));

            Snackbar snackbar = Snackbar.make(
                    getView(),
                    emptyFields + " " + getString(R.string.must_have_value),
                    Snackbar.LENGTH_LONG);
            snackbar.show();
            return false;
        }
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), AccountProvider.CONTENT_URI_ACCOUNT_TYPE, PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);

        if(mEditUri != null){
            for (int i = 0; i < mAccountType.getCount(); i++) {
                Cursor value = (Cursor) mAccountType.getItemAtPosition(i);
                long id = value.getLong(value.getColumnIndex(AccountProvider.ACCOUNT_TYPE.ID.toString()));

                if (id == mSelectedAccountType) {
                    mAccountType.setSelection(i);
                    break;
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}
