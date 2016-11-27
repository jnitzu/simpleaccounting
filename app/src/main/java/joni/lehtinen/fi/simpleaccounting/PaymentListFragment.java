package joni.lehtinen.fi.simpleaccounting;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * List fragment that shows all payments and invoices that has payment date set to not null.
 * This component can be used by other components as it does not require other objects or variables to work
 */
public class PaymentListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int EDIT_PAYMENT_RESULT = 7;
    public static final String PAYMENT_LIST_ACCOUNT_ID = "joni.lehtinen.fi.simpleaccounting.payment_list_account_id";

    private static final String[] PROJECTION = new String[]{
            AccountProvider.PAYMENT.ID.fullName(),                  // 0
            AccountProvider.PAYMENT.TITLE.fullName(),               // 1
            AccountProvider.PAYMENT.AMOUNT.fullName(),              // 2
            AccountProvider.PAYMENT.PAYMENT_DATE.fullName(),        // 3
            AccountProvider.PAYMENT.LABEL_ID.fullName(),            // 4
            AccountProvider.PAYMENT_LABEL.LABEL.fullName()};        // 5

    private PaymentCursorAdapter mAdapter;

    // This is used if other fragment has added view to toolbar so that we can
    // set it gone on contextual action mode
    private FrameLayout mToolbarFramelayout;

    private int mAccountId;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mToolbarFramelayout = (FrameLayout)getActivity().findViewById(R.id.toolbar_extra_container);
        mAccountId = getArguments().getInt(PAYMENT_LIST_ACCOUNT_ID);

        String[] dataColumns = {
                AccountProvider.PAYMENT.TITLE.toString(),
                AccountProvider.PAYMENT.AMOUNT.toString(),
                AccountProvider.PAYMENT_LABEL.LABEL.toString()};

        int[] viewIDs = {
                R.id.list_item_payment_title,
                R.id.list_item_payment_amount,
                R.id.list_item_label_title };

        mAdapter = new PaymentCursorAdapter(getActivity(), R.layout.list_item_payment, null, dataColumns, viewIDs, 0);

        setListAdapter(mAdapter);
        setListShown(false);


        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

                CheckBox checkBox = (CheckBox) getListView().getChildAt(position).findViewById(R.id.list_item_payment_checked);
                checkBox.setChecked(checked);

                // This is used to display edit button when there is only one list item selected
                if (getListView().getCheckedItemCount() < 3) {
                    mode.invalidate();
                }

                mode.setTitle(getListView().getCheckedItemCount() + " selected");

            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

                Cursor cursor = (Cursor)getListAdapter().getItem(0);

                SparseBooleanArray checked = getListView().getCheckedItemPositions();

                switch (item.getItemId()) {
                    case R.id.contextual_edit:

                        Uri editedRow = null;

                        for (int offset = 0; offset < getListView().getCount(); offset++) {
                            if (checked.get(offset)) {
                                cursor.moveToFirst();
                                cursor.move(offset);
                                editedRow = ContentUris.withAppendedId(AccountProvider.CONTENT_URI_PAYMENT, cursor.getLong(cursor.getColumnIndex(AccountProvider.PAYMENT.ID.toString())));
                                break;
                            }
                        }

                        Intent intent = new Intent(getActivity(), CreateActivity.class);
                        intent.putExtra(CreateActivity.EXTRA_FRAGMENT_TYPE, CreateActivity.ADD_FRAGMENT_TYPE.PAYMENT);
                        intent.putExtra(CreateActivity.EXTRA_EDIT_URI,editedRow);
                        startActivityForResult(intent, EDIT_PAYMENT_RESULT);
                        return true;
                    case R.id.contextual_remove:
                        List<Uri> selectedUris = new ArrayList<>();

                        for (int offset = 0; offset < getListView().getCount(); offset++) {
                            if (checked.get(offset)) {
                                cursor.moveToFirst();
                                cursor.move(offset);
                                selectedUris.add(ContentUris.withAppendedId(AccountProvider.CONTENT_URI_PAYMENT, cursor.getLong(cursor.getColumnIndex(AccountProvider.PAYMENT.ID.toString()))));
                            }
                        }

                        for(Uri selectedUri : selectedUris){
                            getActivity().getContentResolver().delete(selectedUri,null,null);
                        }

                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }


            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mAdapter.isContextMode = true;
                mAdapter.notifyDataSetChanged();

                mode.getMenuInflater().inflate(R.menu.fragment_account_list_contextual, menu);

                mToolbarFramelayout.setVisibility(View.GONE);
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                getListView().clearChoices();

                mAdapter.isContextMode = false;
                mAdapter.notifyDataSetChanged();

                mToolbarFramelayout.setVisibility(View.VISIBLE);
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case EDIT_PAYMENT_RESULT:
                if (resultCode == Activity.RESULT_OK) {
                    Log.d("resultCode", "AccountListFragment");
                }
                break;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                getActivity(),
                AccountProvider.CONTENT_URI_PAYMENT,
                PROJECTION,
                AccountProvider.PAYMENT.ACCOUNT_ID + " = " + mAccountId + " AND " + AccountProvider.PAYMENT.PAYMENT_DATE + " IS NOT NULL",
                null,
                AccountProvider.PAYMENT.PAYMENT_DATE + " DESC");
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
     * Custom cursor adapter to have correct formatting and conditional checkbox when in contextual action mode
     */
    private class PaymentCursorAdapter extends SimpleCursorAdapter {

        public boolean isContextMode = false;

        public PaymentCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            PaymentHolder holder = (PaymentHolder)view.getTag();

            Cursor c = getCursor();
            c.moveToFirst();
            c.move(position);

            if( holder == null || holder.id != c.getInt(0) ){
                holder = new PaymentHolder();

                holder.mPaymentDate = (TextView)view.findViewById(R.id.list_item_payment_date);
                holder.mSelected = (CheckBox)view.findViewById(R.id.list_item_payment_checked);

                view.setTag(holder);
            } else {
                holder = (PaymentHolder)view.getTag();
            }

            holder.id = c.getInt(0);

            holder.mPaymentDate.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(c.getLong(3))));

            holder.mSelected.setVisibility(isContextMode ? View.VISIBLE : View.GONE);

            holder.mSelected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    getListView().setItemChecked(position, isChecked);
                }
            });

            return view;
        }
    }

    /**
     * Class that is used by PaymentCursorAdapter as tag for views for easy access to fields
     */
    private static class PaymentHolder {
        public int id;
        public TextView mPaymentDate;
        public CheckBox mSelected;
    }
}
