package joni.lehtinen.fi.simpleaccounting;

import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
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

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import joni.lehtinen.fi.simpleaccounting.Dialog.SelectAccountDialog;
import joni.lehtinen.fi.simpleaccounting.Dialog.SelectLabelDialog;

/**
 * Payment create/edit fragment
 */
public class CreateDirectPaymentFragment extends Fragment implements AddItem, AccountResult, LabelResult {

    private EditText mTitle;
    private EditText mAmount;
    private EditText mNotes;
    private TextView mAccount;
    private TextView mDate;
    private TextView mLabel;

    private Date mSelectedDate;

    private DatePickerDialog mPaymentDatePicker;

    private Uri mEditUri;

    private int mAccountId = -1;
    private int mLabelId = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_direct_payment, container, false);

        if(getArguments() != null)
            mEditUri = getArguments().getParcelable(CreateActivity.EXTRA_EDIT_URI);

        mTitle = (EditText)view.findViewById(R.id.edit_text_payment_title);
        mAmount = (EditText)view.findViewById(R.id.edit_text_payment_amount);
        mNotes = (EditText)view.findViewById(R.id.edit_text_notes);
        mAccount = (TextView)view.findViewById(R.id.text_select_account);
        mDate = (TextView)view.findViewById(R.id.text_select_date);
        mLabel = (TextView)view.findViewById(R.id.text_select_label);

        mSelectedDate = new Date(Calendar.getInstance().getTimeInMillis());
        mDate.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(mSelectedDate));

        mAccount.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                Fragment previous = getFragmentManager().findFragmentByTag("dialog");
                if (previous != null) {
                    fragmentTransaction.remove(previous);
                }
                fragmentTransaction.addToBackStack(null);

                // Create and show the dialog.
                DialogFragment dialogFragment = new SelectAccountDialog();

                dialogFragment.show(fragmentTransaction, "dialog");

            }

        });

        mDate.setOnClickListener(new View.OnClickListener() {

            final Calendar calendar = Calendar.getInstance();

            @Override
            public void onClick(View v) {
                mPaymentDatePicker = new DatePickerDialog(
                        getActivity(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                calendar.set(year,monthOfYear,dayOfMonth);
                                mSelectedDate = calendar.getTime();
                                mDate.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(mSelectedDate));
                            }
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH));
                mPaymentDatePicker.show();
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

                // Create and show the dialog.
                DialogFragment dialogFragment = new SelectLabelDialog();

                dialogFragment.show(fragmentTransaction, "dialog");
            }

        });

        if(mEditUri != null) {
            Cursor cursor = getActivity().getContentResolver().query(mEditUri,
                    new String[]{
                            AccountProvider.PAYMENT.TITLE.fullName(),               // 0
                            AccountProvider.PAYMENT.AMOUNT.fullName(),              // 1
                            AccountProvider.PAYMENT.ACCOUNT_ID.fullName(),          // 2
                            AccountProvider.ACCOUNT.TITLE.fullName(),               // 3
                            AccountProvider.PAYMENT.PAYMENT_DATE.fullName(),        // 4
                            AccountProvider.PAYMENT.DESCRIPTION.fullName(),         // 5
                            AccountProvider.PAYMENT.LABEL_ID.fullName(),            // 6
                            AccountProvider.PAYMENT_LABEL.LABEL.fullName()},        // 7
                    null,
                    null,
                    null);

            if (cursor != null) {
                cursor.moveToFirst();

                mTitle.setText(cursor.getString(0));
                mAmount.setText(cursor.getString(1));
                mNotes.setText(cursor.getString(5));
                mAccount.setText(cursor.getString(3));
                mLabel.setText(cursor.getString(7));

                mAccountId = cursor.getInt(2);
                mLabelId = cursor.getInt(6);

                if (!cursor.isNull(4)) {
                    mSelectedDate = new Date(cursor.getLong(4));
                    mDate.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(mSelectedDate));
                }

            }
        }

        return view;
    }


    @Override
    public void saveToDatabase() {

        ContentValues contentValues = new ContentValues();

        contentValues.put(AccountProvider.PAYMENT.ACCOUNT_ID.toString(), mAccountId);
        contentValues.put(AccountProvider.PAYMENT.TITLE.toString(), mTitle.getText().toString());
        contentValues.put(AccountProvider.PAYMENT.AMOUNT.toString(), Double.parseDouble(mAmount.getText().toString()));
        contentValues.put(AccountProvider.PAYMENT.PAYMENT_DATE.toString(), mSelectedDate.getTime());
        contentValues.put(AccountProvider.PAYMENT.LABEL_ID.toString(), mLabelId);
        contentValues.put(AccountProvider.PAYMENT.DESCRIPTION.toString(), mNotes.getText().toString());

        if(mEditUri != null){
            getActivity().getContentResolver().update(mEditUri, contentValues, null, null);
        } else {
            getActivity().getContentResolver().insert(AccountProvider.CONTENT_URI_PAYMENT, contentValues);
        }
    }

    @Override
    public boolean isFormValid() {
        boolean emptyTitle = mTitle.getText().toString().isEmpty();
        boolean emptyBalance = mAmount.getText().toString().isEmpty();

        if(emptyTitle || emptyBalance || mAccountId == -1 || mLabelId == -1){
            String emptyFields = Utility.createIsEmptyString(
                    new boolean[]{mAccountId == -1,emptyTitle,emptyBalance,mLabelId == -1},
                    new String[]{getString(R.string.select_account),getString(R.string.title),getString(R.string.payment_amount),getString(R.string.label)},
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
    @Override
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

    @Override
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
