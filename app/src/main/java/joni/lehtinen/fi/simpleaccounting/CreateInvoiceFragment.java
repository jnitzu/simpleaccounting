package joni.lehtinen.fi.simpleaccounting;

import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.app.TimePickerDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import joni.lehtinen.fi.simpleaccounting.Dialog.SelectAccountDialog;
import joni.lehtinen.fi.simpleaccounting.Dialog.SelectLabelDialog;

/**
 * Invoice create/edit fragment
 */
public class CreateInvoiceFragment extends Fragment implements AddItem, AccountResult, LabelResult {


    private TextView mAccount;
    private EditText mTitle;
    private EditText mAmount;
    private TextView mDueDate;
    private TextView mPaymentDate;
    private TextView mNotificationDate;
    private TextView mNotificationTime;
    private TextView mRepeatInterval;
    private TextView mAutoPayDate;
    private TextView mLabel;
    private EditText mNotes;

    private Date mSelectedDueDate;
    private Date mSelectedPaymentDate;
    private Date mSelectedNotificationDate;
    private Date mSelectedAutoPaymentDate;
    private Date mSelectedRepeatInterval;

    private Uri mEditUri;

    private int mAccountId = -1;
    private int mLabelId = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_invoice, container, false);

        if(getArguments() != null)
            mEditUri = getArguments().getParcelable(CreateActivity.EXTRA_EDIT_URI);

        mAccount = (TextView)view.findViewById(R.id.text_select_account);
        mTitle = (EditText)view.findViewById(R.id.edit_text_payment_title);
        mAmount = (EditText)view.findViewById(R.id.edit_text_payment_amount);
        mDueDate = (TextView)view.findViewById(R.id.text_select_due_date);
        mPaymentDate = (TextView)view.findViewById(R.id.text_select_date);
        mNotificationDate = (TextView)view.findViewById(R.id.text_invoice_notification_date);
        mNotificationTime = (TextView)view.findViewById(R.id.text_invoice_notification_time);
        mRepeatInterval = (TextView)view.findViewById(R.id.text_repeat_interval);
        mAutoPayDate = (TextView)view.findViewById(R.id.text_auto_pay_date);
        mLabel = (TextView)view.findViewById(R.id.text_select_label);
        mNotes = (EditText)view.findViewById(R.id.edit_text_notes);

        mAccount.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                Fragment previous = getFragmentManager().findFragmentByTag("dialog");
                if (previous != null) {
                    fragmentTransaction.remove(previous);
                }
                fragmentTransaction.addToBackStack(null);

                DialogFragment dialogFragment = new SelectAccountDialog();
                dialogFragment.show(fragmentTransaction, "dialog");

            }

        });

        mDueDate.setOnClickListener(new View.OnClickListener() {

            final Calendar calendar = Calendar.getInstance();

            @Override
            public void onClick(View v) {
                new DatePickerDialog(
                        getActivity(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                calendar.set(year,monthOfYear,dayOfMonth);
                                mSelectedDueDate = calendar.getTime();
                                mDueDate.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(mSelectedDueDate));
                            }
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)).show();
            }

        });

        mPaymentDate.setOnClickListener(new View.OnClickListener() {

            final Calendar calendar = Calendar.getInstance();

            @Override
            public void onClick(View v) {
                new DatePickerDialog(
                        getActivity(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                calendar.set(year,monthOfYear,dayOfMonth);
                                mSelectedPaymentDate = calendar.getTime();
                                mPaymentDate.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(mSelectedPaymentDate));
                            }
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)).show();
            }

        });

        mAutoPayDate.setOnClickListener(new View.OnClickListener() {

            final Calendar calendar = Calendar.getInstance();

            @Override
            public void onClick(View v) {
                new DatePickerDialog(
                        getActivity(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                calendar.set(year,monthOfYear,dayOfMonth);
                                mSelectedAutoPaymentDate = calendar.getTime();
                                mAutoPayDate.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(mSelectedAutoPaymentDate));
                            }
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)).show();
            }

        });

        mNotificationDate.setOnClickListener(new View.OnClickListener() {

            final Calendar calendar = Calendar.getInstance();

            @Override
            public void onClick(View v) {
                new DatePickerDialog(
                        getActivity(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

                                if(mSelectedNotificationDate != null)
                                    calendar.setTime(mSelectedNotificationDate);

                                calendar.set(year,monthOfYear,dayOfMonth);
                                mSelectedNotificationDate = calendar.getTime();
                                mNotificationDate.setText(DateFormat.getDateInstance(DateFormat.FULL).format(mSelectedNotificationDate));
                            }
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)).show();
            }

        });

        mNotificationTime.setOnClickListener(new View.OnClickListener() {

            final Calendar calendar = Calendar.getInstance();

            @Override
            public void onClick(View v) {
                new TimePickerDialog(
                        getActivity(),
                        new TimePickerDialog.OnTimeSetListener(){

                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

                                if(mSelectedNotificationDate != null)
                                    calendar.setTime(mSelectedNotificationDate);

                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);
                                mSelectedNotificationDate = calendar.getTime();
                                mNotificationTime.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(mSelectedNotificationDate));
                            }
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true).show();
            }

        });

        mLabel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                Fragment previous = getFragmentManager().findFragmentByTag("dialog");
                if (previous != null) {
                    fragmentTransaction.remove(previous);
                }
                fragmentTransaction.addToBackStack(null);

                DialogFragment dialogFragment = new SelectLabelDialog();
                dialogFragment.show(fragmentTransaction, "dialog");
            }

        });


        if(mEditUri != null){
            Cursor cursor = getActivity().getContentResolver().query(mEditUri,
                    new String[]{
                            AccountProvider.PAYMENT.TITLE.fullName(),               // 0
                            AccountProvider.PAYMENT.AMOUNT.fullName(),              // 1
                            AccountProvider.PAYMENT.ACCOUNT_ID.fullName(),          // 2
                            AccountProvider.ACCOUNT.TITLE.fullName(),               // 3
                            AccountProvider.PAYMENT.PAYMENT_DATE.fullName(),        // 4
                            AccountProvider.PAYMENT.DESCRIPTION.fullName(),         // 5
                            AccountProvider.PAYMENT.LABEL_ID.fullName(),            // 6
                            AccountProvider.PAYMENT_LABEL.LABEL.fullName(),         // 7
                            AccountProvider.INVOICE.AUTO_PAY_DATE.fullName(),       // 8
                            AccountProvider.INVOICE.NOTIFICATION_DATE.fullName(),   // 9
                            AccountProvider.INVOICE.REPEAT_INTERVAL.fullName(),     // 10
                            AccountProvider.INVOICE.DUE_DATE.fullName()},           // 11
                    null,
                    null,
                    null);

            if(cursor != null){
                cursor.moveToFirst();

                mTitle.setText(cursor.getString(0));
                mAmount.setText(cursor.getString(1));
                mNotes.setText(cursor.getString(5));
                mAccount.setText(cursor.getString(3));
                mLabel.setText(cursor.getString(7));

                mAccountId = cursor.getInt(2);
                mLabelId = cursor.getInt(6);

                if( !cursor.isNull(11) ){
                    mSelectedDueDate = new Date(cursor.getLong(11));
                    mDueDate.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(mSelectedDueDate));
                }

                if( !cursor.isNull(4) ){
                    mSelectedPaymentDate = new Date(cursor.getLong(4));
                    mPaymentDate.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(mSelectedPaymentDate));
                }

                if( !cursor.isNull(8) ){
                    mSelectedAutoPaymentDate = new Date(cursor.getLong(8));
                    mAutoPayDate.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(mSelectedAutoPaymentDate));
                }

                if( !cursor.isNull(10) ){
                    mSelectedRepeatInterval = new Date(cursor.getLong(10));
                    mRepeatInterval.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(mSelectedRepeatInterval));
                }

                if( !cursor.isNull(9) ){
                    mSelectedNotificationDate = new Date(cursor.getLong(9));
                    mNotificationDate.setText(DateFormat.getDateInstance(DateFormat.FULL).format(mSelectedNotificationDate));
                    mNotificationTime.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(mSelectedNotificationDate));
                }

            }

        }

        return view;
    }

    @Override
    public void saveToDatabase() {

        ContentValues payment = new ContentValues();
        ContentValues invoice = new ContentValues();

        payment.put(AccountProvider.PAYMENT.ACCOUNT_ID.toString(), mAccountId);
        payment.put(AccountProvider.PAYMENT.TITLE.toString(), mTitle.getText().toString());
        payment.put(AccountProvider.PAYMENT.AMOUNT.toString(), Double.parseDouble(mAmount.getText().toString()));

        if(mSelectedPaymentDate != null)
            payment.put(AccountProvider.PAYMENT.PAYMENT_DATE.toString(), mSelectedPaymentDate.getTime());

        payment.put(AccountProvider.PAYMENT.LABEL_ID.toString(), mLabelId);
        payment.put(AccountProvider.PAYMENT.DESCRIPTION.toString(), mNotes.getText().toString());

        invoice.put(AccountProvider.INVOICE.DUE_DATE.toString(), mSelectedDueDate.getTime());

        if(mSelectedNotificationDate != null)
            invoice.put(AccountProvider.INVOICE.NOTIFICATION_DATE.toString(), mSelectedNotificationDate.getTime());

        if(mSelectedAutoPaymentDate != null)
            invoice.put(AccountProvider.INVOICE.AUTO_PAY_DATE.toString(), mSelectedAutoPaymentDate.getTime());

        //invoice.put(AccountProvider.INVOICE.REPEAT_INTERVAL.toString(),);

        if(mEditUri != null){
            Uri paymentUri = ContentUris.withAppendedId(AccountProvider.CONTENT_URI_PAYMENT, Long.parseLong(mEditUri.getPathSegments().get(1)));
            getActivity().getContentResolver().update(paymentUri, payment, null, null);
            getActivity().getContentResolver().update(mEditUri, invoice, null, null);
        } else {
            Uri uri = getActivity().getContentResolver().insert(AccountProvider.CONTENT_URI_PAYMENT, payment);

            //Set ID of invoice record as ID of payment record
            invoice.put(AccountProvider.INVOICE.ID.toString(),Long.parseLong(uri.getPathSegments().get(1)));

            getActivity().getContentResolver().insert(AccountProvider.CONTENT_URI_INVOICE, invoice);
        }
    }

    @Override
    public boolean isFormValid() {
        boolean emptyTitle = mTitle.getText().toString().isEmpty();
        boolean emptyAmount = mAmount.getText().toString().isEmpty();

        if(emptyTitle || emptyAmount || mAccountId == -1 || mLabelId == -1 || mSelectedDueDate == null ){
            String emptyFields = Utility.createIsEmptyString(
                    new boolean[]{mAccountId == -1,emptyTitle,emptyAmount,mLabelId == -1,mSelectedDueDate == null },
                    new String[]{getString(R.string.select_account),getString(R.string.title),getString(R.string.payment_amount),getString(R.string.label),getString(R.string.due_date)},
                    getString(R.string.and));

            final Snackbar snackbar = Snackbar.make(
                    getView(),
                    emptyFields + " " + getString(R.string.must_have_value),
                    Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(R.string.snackbar_close_text_field_empty, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackbar.dismiss();
                }
            });

            snackbar.show();
            return false;
        }
        return true;
    }


    /**
     * Parent Activity will send Uri of selected row from SelectAccountDialog to this method
     * @param uri AccountFragment row uri that was selected
     */
    public void selectAccountResult(Uri uri){
        Cursor c = getActivity().getContentResolver().query(uri,new String[]{AccountProvider.ACCOUNT.ID.fullName(),AccountProvider.ACCOUNT.TITLE.fullName()},null,null,null);
        if(c != null){
            if(c.moveToFirst()){
                mAccountId = c.getInt(0);
                mAccount.setText(c.getString(1));
            }
            c.close();
        }
    }

    public void selectLabelResult(Uri uri){
        Cursor c = getActivity().getContentResolver().query(uri,new String[]{AccountProvider.PAYMENT_LABEL.ID.toString(),AccountProvider.PAYMENT_LABEL.LABEL.toString()},null,null,null);
        if(c != null){
            if(c.moveToFirst()){
                mLabelId = c.getInt(0);
                mLabel.setText(c.getString(1));
            }
            c.close();
        }
    }
}
