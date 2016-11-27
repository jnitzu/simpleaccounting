package joni.lehtinen.fi.simpleaccounting;

import android.app.Fragment;
import android.content.DialogInterface;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import joni.lehtinen.fi.simpleaccounting.Dialog.SelectAccountDialog;
import joni.lehtinen.fi.simpleaccounting.Dialog.SelectLabelDialog;

/**
 * CreateActivity class is a host class for fragments that add and edit database
 */
public class CreateActivity extends AppCompatActivity implements SelectAccountDialog.OnAccountSelectedListener, SelectLabelDialog.OnLabelSelectedListener {

    /**
     * Fragment types that CreateActivity hosts
     */
    public enum ADD_FRAGMENT_TYPE{ACCOUNT,PAYMENT,INVOICE};

    public static final String EXTRA_FRAGMENT_TYPE = "joni.lehtinen.fi.simpleaccounting.fragment_type";
    public static final String EXTRA_EDIT_URI = "joni.lehtinen.fi.simpleaccounting.edit_uri";

    // Current fragment type
    private ADD_FRAGMENT_TYPE mType;

    // Content provider uri for database record that is edited
    private Uri mEditUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        Fragment fragment = null;

        // Get fragment type to be instantiated
        mType = (ADD_FRAGMENT_TYPE)getIntent().getSerializableExtra(EXTRA_FRAGMENT_TYPE);
        mEditUri = getIntent().getParcelableExtra(EXTRA_EDIT_URI);

        switch(mType){
            case ACCOUNT:
                fragment = new CreateAccountFragment();
                if(mEditUri == null){
                    getSupportActionBar().setTitle(R.string.title_add_account);
                } else {
                    getSupportActionBar().setTitle(R.string.title_edit_account);
                }
                break;
            case PAYMENT:
                fragment = new CreateDirectPaymentFragment();
                if(mEditUri == null){
                    getSupportActionBar().setTitle(R.string.title_add_payment);
                } else {
                    getSupportActionBar().setTitle(R.string.title_edit_payment);
                }
                break;
            case INVOICE:
                fragment = new CreateInvoiceFragment();
                if(mEditUri == null){
                    getSupportActionBar().setTitle(R.string.title_add_invoice);
                } else {
                    getSupportActionBar().setTitle(R.string.title_edit_invoice);
                }
                break;
        }

        if( fragment != null ){
            // Set edit uri to fragments bundle
            if(mEditUri != null){
                Bundle bundle = new Bundle();
                bundle.putParcelable(EXTRA_EDIT_URI,mEditUri);
                fragment.setArguments(bundle);
            }

            // Start fragment
            getFragmentManager().beginTransaction()
                    .add(R.id.create_fragment_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_create, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_save:

                // Find current fragment
                Fragment fragment = getFragmentManager().findFragmentById(R.id.create_fragment_container);

                // All create fragments must implement AddItem, but check for it anyway
                if( fragment instanceof AddItem){
                    AddItem addItem = (AddItem)fragment;

                    // Check if form is valid and request fragment to save its data to database
                    // Fragment should highlight if fields are not valid when calling isFormValid
                    if(addItem.isFormValid()){
                        addItem.saveToDatabase();

                        // Send result ok so that main activity can update its values accordingly
                        setResult(RESULT_OK, null);
                        finish();
                    }
                }
                break;
            case android.R.id.home:
                // When user presses back button in actionbar/toolbar show confirm exit dialog
                showConfirmExitDialog();
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    /**
     *  Method for transferring messages from SelectAccountDialog to Fragments
     * @param uri Uri for account row that was selected
     */
    @Override
    public void onAccountSelected(Uri uri) {

        Fragment fragment = getFragmentManager().findFragmentById(R.id.create_fragment_container);

        if(fragment instanceof AccountResult){
            ((AccountResult)fragment).selectAccountResult(uri);
        }
    }

    /**
     * Method for transferring messages from SelectLabelDialog to Fragments
     * @param uri Uri for Label row that was selected
     */
    @Override
    public void onLabelSelected(Uri uri) {

        Fragment fragment = getFragmentManager().findFragmentById(R.id.create_fragment_container);

        if(fragment instanceof LabelResult){
            ((LabelResult)fragment).selectLabelResult(uri);
        }
    }

    @Override
    public void onBackPressed() {
        showConfirmExitDialog();
    }

    /**
     * Exit dialog when user exits CreateActivity
     */
    private void showConfirmExitDialog(){

        int type_text = 0;
        switch(mType){
            case ACCOUNT:
                type_text = R.string.type_account;
                break;
            case PAYMENT:
                type_text = R.string.type_payment;
                break;
            case INVOICE:
                type_text = R.string.type_invoice;
                break;
        }

        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.alert_dialog_discard_text) + " " + getString(type_text) + "?")
                .setPositiveButton(R.string.keep_editing, null)
                .setNegativeButton(R.string.discard, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CreateActivity.this.finish();
                    }

                })
                .show();
    }
}
