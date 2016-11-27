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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Invoice management fragment. This fragment uses toolbar for filter view and cannot be used as a component to other fragments
 */
public class InvoiceListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int EDIT_INVOICE_RESULT = 6;

    private static final String[] PROJECTION = new String[]{
            AccountProvider.PAYMENT.ID.fullName(),                  // 0
            AccountProvider.PAYMENT.TITLE.fullName(),               // 1
            AccountProvider.PAYMENT.AMOUNT.fullName(),              // 2
            AccountProvider.PAYMENT.ACCOUNT_ID.fullName(),          // 3
            AccountProvider.ACCOUNT.TITLE.fullName(),               // 4
            AccountProvider.PAYMENT.PAYMENT_DATE.fullName(),        // 5
            AccountProvider.PAYMENT.DESCRIPTION.fullName(),         // 6
            AccountProvider.PAYMENT.LABEL_ID.fullName(),            // 7
            AccountProvider.PAYMENT_LABEL.LABEL.fullName(),         // 8
            AccountProvider.INVOICE.AUTO_PAY_DATE.fullName(),       // 9
            AccountProvider.INVOICE.NOTIFICATION_DATE.fullName(),   // 10
            AccountProvider.INVOICE.REPEAT_INTERVAL.fullName(),     // 11
            AccountProvider.INVOICE.DUE_DATE.fullName()};           // 12

    private Spinner mSortOrderSpinner;
    private Spinner mSortTypeSpinner;
    private CheckBox mShowPaidInvoicesCheckbox;

    private FrameLayout mToolbarFramelayout;

    private InvoiceCursorAdapter mAdapter;

    private String mSortOrder = "ASC";
    private String mSortType = AccountProvider.INVOICE.DUE_DATE.toString();
    private String mSelection = AccountProvider.PAYMENT.PAYMENT_DATE + " IS NULL";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View filter = inflater.inflate(R.layout.invoice_filter, container, false);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if(actionBar != null)
            actionBar.setTitle(R.string.title_invoices);

        mSortTypeSpinner = (Spinner)filter.findViewById(R.id.spinner_sort_type);
        mSortOrderSpinner = (Spinner)filter.findViewById(R.id.spinner_sort_order);
        mShowPaidInvoicesCheckbox = (CheckBox)filter.findViewById(R.id.checkbox_show_paid_invoices);

        ArrayAdapter<CharSequence> adapterSortType = ArrayAdapter.createFromResource(
                getActivity(),
                R.array.sort_type,
                R.layout.spinner_item);

        adapterSortType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSortTypeSpinner.setAdapter(adapterSortType);
        mSortTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        mSortType = AccountProvider.INVOICE.DUE_DATE.toString();
                        break;
                    case 1:
                        mSortType = AccountProvider.INVOICE.AUTO_PAY_DATE.toString();
                        break;
                    case 2:
                        mSortType = AccountProvider.PAYMENT.AMOUNT.toString();
                        break;
                }

                getLoaderManager().restartLoader(0, null, InvoiceListFragment.this);
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ArrayAdapter<CharSequence> adapterSortOrder = ArrayAdapter.createFromResource(
                getActivity(),
                R.array.sort_order,
                R.layout.spinner_item);

        adapterSortOrder.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSortOrderSpinner.setAdapter(adapterSortOrder);
        mSortOrderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        mSortOrder = "ASC";
                        break;
                    case 1:
                        mSortOrder = "DESC";
                        break;
                }

                getLoaderManager().restartLoader(0, null, InvoiceListFragment.this);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mShowPaidInvoicesCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    mSelection = "";
                } else {
                    mSelection = AccountProvider.PAYMENT.PAYMENT_DATE + " IS NULL";
                }

                getLoaderManager().restartLoader(0, null, InvoiceListFragment.this);
            }
        });

        mToolbarFramelayout = (FrameLayout)getActivity().findViewById(R.id.toolbar_extra_container);
        mToolbarFramelayout.addView(filter);

        return super.onCreateView(inflater, container, savedInstanceState);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] dataColumns = {
                AccountProvider.PAYMENT.AMOUNT.toString() };

        int[] viewIDs = {
                R.id.list_item_invoice_balance };

        mAdapter = new InvoiceCursorAdapter(getActivity(), R.layout.list_item_invoice, null, dataColumns, viewIDs, 0);

        setListAdapter(mAdapter);
        setListShown(false);


        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

                CheckBox checkBox = (CheckBox) getListView().getChildAt(position).findViewById(R.id.list_item_invoice_checked);
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
                                editedRow = ContentUris.withAppendedId(AccountProvider.CONTENT_URI_INVOICE, cursor.getLong(cursor.getColumnIndex(AccountProvider.PAYMENT.ID.toString())));
                                break;
                            }
                        }

                        Intent intent = new Intent(getActivity(), CreateActivity.class);
                        intent.putExtra(CreateActivity.EXTRA_FRAGMENT_TYPE, CreateActivity.ADD_FRAGMENT_TYPE.INVOICE);
                        intent.putExtra(CreateActivity.EXTRA_EDIT_URI,editedRow);
                        startActivityForResult(intent, EDIT_INVOICE_RESULT);
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
            case EDIT_INVOICE_RESULT:
                if (resultCode == Activity.RESULT_OK) {
                    Log.d("resultCode","AccountListFragment");
                }
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // When fragment is destroyed clear toolbar view
        mToolbarFramelayout.removeAllViews();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), AccountProvider.CONTENT_URI_INVOICE, PROJECTION, mSelection, null, mSortType + " " + mSortOrder);
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
    private class InvoiceCursorAdapter extends SimpleCursorAdapter {

        public boolean isContextMode = false;

        public InvoiceCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            InvoiceHolder holder = (InvoiceHolder)view.getTag();

            Cursor c = getCursor();
            c.moveToFirst();
            c.move(position);

            if( holder == null || holder.id != c.getInt(0) ){
                holder = new InvoiceHolder();

                holder.mInvoiceTitle = (TextView)view.findViewById(R.id.list_item_invoice_title);
                holder.mAmount = (TextView)view.findViewById(R.id.list_item_invoice_balance);
                holder.mAccountTitle = (TextView)view.findViewById(R.id.list_item_account_title);
                holder.mDueDate = (TextView)view.findViewById(R.id.list_item_invoice_due_date);
                holder.mAutoPayDate = (TextView)view.findViewById(R.id.list_item_invoice_auto_pay_date);
                holder.mPaymentDate = (TextView)view.findViewById(R.id.list_item_invoice_payment_date);
                holder.mNotification = (ImageView)view.findViewById(R.id.list_item_notification_checked);
                holder.mRepeat = (ImageView)view.findViewById(R.id.list_item_repeat_checked);
                holder.mSelected = (CheckBox)view.findViewById(R.id.list_item_invoice_checked);
                holder.mHolderAutoPaymentDate = view.findViewById(R.id.holder_auto_pay_date);
                holder.mHolderPaymentDate = view.findViewById(R.id.holder_payment_date);

                view.setTag(holder);
            } else {
                holder = (InvoiceHolder)view.getTag();
            }

            holder.id = c.getInt(0);
            long dueDate = c.getLong(12);
            boolean isNullAutoPay = c.isNull(9);
            boolean isNullPaymentDate = c.isNull(5);
            boolean isNullNotification = c.isNull(10);
            boolean isNullRepeat = c.isNull(11);

            holder.mInvoiceTitle.setText(c.getString(1));

            holder.mDueDate.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(dueDate)));

            if( !isNullAutoPay ){
                long autoPayDate = c.getLong(9);
                holder.mAutoPayDate.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(autoPayDate)));
            }

            if( !isNullPaymentDate ){
                long paymentDate = c.getLong(5);
                holder.mPaymentDate.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(paymentDate)));
            }

            holder.mHolderAutoPaymentDate.setVisibility( isNullAutoPay ? View.GONE : View.VISIBLE );
            holder.mHolderPaymentDate.setVisibility( isNullPaymentDate ? View.GONE : View.VISIBLE );
            holder.mNotification.setVisibility( isNullNotification ? View.GONE : View.VISIBLE );
            holder.mRepeat.setVisibility( isNullRepeat ? View.GONE : View.VISIBLE );
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
     * Class that is used by InvoiceCursorAdapter as tag for views for easy access to fields
     */
    private static class InvoiceHolder{
        public int id;
        public TextView mInvoiceTitle;
        public TextView mAmount;
        public TextView mAccountTitle;
        public TextView mDueDate;
        public TextView mAutoPayDate;
        public TextView mPaymentDate;
        public ImageView mNotification;
        public ImageView mRepeat;
        public CheckBox mSelected;
        public View mHolderAutoPaymentDate;
        public View mHolderPaymentDate;
    }
}
