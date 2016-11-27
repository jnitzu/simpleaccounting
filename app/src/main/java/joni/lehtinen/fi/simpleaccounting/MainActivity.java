package joni.lehtinen.fi.simpleaccounting;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.widget.PopupWindow;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import joni.lehtinen.fi.simpleaccounting.Dialog.CreateLabelDialog;

/**
 * Main Activity that holds Drawers, main fragment container and drive content
 */
public class MainActivity
        extends
        AppCompatActivity
        implements
        NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,CreateLabelDialog.OnLabelCreatedListener {

    private static final String TAG = "MainActivity";
    private static final String DATABASE_FILE_TITLE_ON_DRIVE = "simple_accounting.db";
    private static final String SQLITE_MIME_TYPE = "application/x-sqlite3";

    // onActivityResult request codes
    // Used codes:
    // 5 Used by AccountListFragment
    // 6 Used by InvoiceListFragment
    // 7 Used bu PaymentListFragment
    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 1;
    private static final int ADD_ACCOUNT_RESULT = 2;
    private static final int ADD_PAYMENT_RESULT = 3;
    private static final int ADD_INVOICE_RESULT = 4;

    private static final String BACKSTACK_FIRST = "Start";
    private static final String BACKSTACK_INVOICE = "Invoice";
    private static final String BACKSTACK_LABELS = "Labels";

    private GoogleApiClient mGoogleApiClient;
    private PopupWindow mPopupWindow;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                /* Doesn't look good yet

                LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                View layout = inflater.inflate(R.layout.popupwindow_fab, (ViewGroup) findViewById(R.id.popup_layout));
                layout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

                TextView popupAccount = (TextView)layout.findViewById(R.id.popup_fab_add_account);
                TextView popupPayment = (TextView)layout.findViewById(R.id.popup_fab_add_payment);
                TextView popupInvoice = (TextView)layout.findViewById(R.id.popup_fab_add_invoice);
                TextView popupLabel = (TextView)layout.findViewById(R.id.popup_fab_add_label);

                View.OnClickListener onClick = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupMenuOnClick(v.getId());
                        mPopupWindow.dismiss();
                    }
                };

                popupAccount.setOnClickListener(onClick);
                popupPayment.setOnClickListener(onClick);
                popupInvoice.setOnClickListener(onClick);
                popupLabel.setOnClickListener(onClick);

                mPopupWindow = new PopupWindow(layout, layout.getMeasuredWidth(), layout.getMeasuredHeight(), true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mPopupWindow.showAsDropDown(view, 100, 0, Gravity.BOTTOM);
                }
                */
                PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        popupMenuOnClick(item.getItemId());
                        return true;
                    }
                });
                popupMenu.inflate(R.menu.popup_fab);
                popupMenu.show();

            }

        });

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(mDrawerToggle);

        // Set up listener for situation where drawer indicator is disabled
        mDrawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getFragmentManager().getBackStackEntryCount() > 1) {
                    // First entry in the backstack is from empty screen to account list view
                    // that is why we will always keep that state there to go back to it.

                    getFragmentManager().popBackStack();

                    // If we are at last transaction then enable drawer indicator
                    if (getFragmentManager().getBackStackEntryCount() == 2) {
                        mDrawerToggle.setDrawerIndicatorEnabled(true);
                    }
                }
            }
        });

        mDrawerToggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        FragmentManager fragmentManager = getFragmentManager();

        // Listener that enables/disables drawer indicator depending if we are on account list or not
        fragmentManager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                mDrawerToggle.setDrawerIndicatorEnabled(getFragmentManager().getBackStackEntryCount() <= 1);
            }
        });

        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, new AccountListFragment())
                .addToBackStack(BACKSTACK_FIRST)
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_APPFOLDER)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }

        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Exception while starting resolution activity", e);
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                }
                break;
            case ADD_ACCOUNT_RESULT:
                if (resultCode == RESULT_OK) {
                }
                break;
            case ADD_PAYMENT_RESULT:
                if (resultCode == RESULT_OK) {
                }
                break;
            case ADD_INVOICE_RESULT:
                if (resultCode == RESULT_OK) {
                }
                break;
            case AccountListFragment.EDIT_ACCOUNT_RESULT:
                if (resultCode == RESULT_OK) {
                    Log.d("resultCode","MainActivity");
                }
                break;

        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (getFragmentManager().getBackStackEntryCount() > 1 ){
            // First entry in the backstack is from empty screen to account list view
            // that is why we will always keep that state there to go back to it.

            getFragmentManager().popBackStack();
        } else {
            // Exit application dialog
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.alert_dialog_exit_application_title))
                    .setMessage(getString(R.string.alert_dialog_exit_application_msg))
                    .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Looks better when dialog is dismissed first
                            dialog.dismiss();

                            MainActivity.super.onBackPressed();
                        }

                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /* Debug menu buttons

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_debug_print:
                getContentResolver().query(AccountProvider.CONTENT_URI_DEBUG, null, null, null, null);
                break;
            case R.id.action_debug_delete_payments:
                getContentResolver().delete(AccountProvider.CONTENT_URI_PAYMENT,null,null);
                getContentResolver().delete(AccountProvider.CONTENT_URI_INVOICE,null,null);
                break;
            case R.id.action_debug_copy_database:
                File from = new File(getDatabasePath(AccountProvider.DATABASE_NAME).getPath());
                File to = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()+"/database");

                OutputStream out;
                InputStream in;
                try {
                    to.createNewFile();
                    out = new FileOutputStream(to);
                    in = new FileInputStream(from);

                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }
    */

    /**
     * Popup Menu onClick handler for fab popup menu.
     * This is used inside listener that handles menuitem clicked or textview clicked events
     * @param id Resource id for menuitem or textview
     */
    private void popupMenuOnClick(int id){
        Intent intent;
        switch (id){
            case R.id.popup_menu_fab_account:
            case R.id.popup_fab_add_account:
                intent = new Intent(this, CreateActivity.class);
                intent.putExtra(CreateActivity.EXTRA_FRAGMENT_TYPE, CreateActivity.ADD_FRAGMENT_TYPE.ACCOUNT);
                startActivityForResult(intent, ADD_ACCOUNT_RESULT);
                break;
            case R.id.popup_menu_fab_payment:
            case R.id.popup_fab_add_payment:
                intent = new Intent(this, CreateActivity.class);
                intent.putExtra(CreateActivity.EXTRA_FRAGMENT_TYPE, CreateActivity.ADD_FRAGMENT_TYPE.PAYMENT);
                startActivityForResult(intent, ADD_PAYMENT_RESULT);
                break;
            case R.id.popup_menu_fab_invoice:
            case R.id.popup_fab_add_invoice:
                intent = new Intent(this, CreateActivity.class);
                intent.putExtra(CreateActivity.EXTRA_FRAGMENT_TYPE, CreateActivity.ADD_FRAGMENT_TYPE.INVOICE);
                startActivityForResult(intent, ADD_INVOICE_RESULT);
                break;
            case R.id.popup_menu_fab_label:
            case R.id.popup_fab_add_label:
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                Fragment previous = getFragmentManager().findFragmentByTag("dialog");
                if (previous != null) {
                    fragmentTransaction.remove(previous);
                }
                fragmentTransaction.addToBackStack(null);

                DialogFragment dialogFragment = new CreateLabelDialog();

                dialogFragment.show(fragmentTransaction, "dialog");
                break;
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.nav_accounts:
                getFragmentManager().popBackStack(BACKSTACK_FIRST, 0);
                break;
            case R.id.nav_invoices:
                getFragmentManager().popBackStack(BACKSTACK_FIRST, 0);
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new InvoiceListFragment())
                        .addToBackStack(BACKSTACK_INVOICE)
                        .commit();
                break;
            case R.id.nav_labels:
                getFragmentManager().popBackStack(BACKSTACK_FIRST, 0);
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new LabelListFragment())
                        .addToBackStack(BACKSTACK_LABELS)
                        .commit();
                break;
            case R.id.nav_import_database:
                if(mGoogleApiClient.isConnected()){
                    importFromDrive();
                }
                break;
            case R.id.nav_export_database:
                if(mGoogleApiClient.isConnected()) {
                    exportToDrive();
                }
                break;
            case R.id.nav_settings:
                break;
            case R.id.nav_about:
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient is connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    /**
     * Export database to drive
     */
    private void exportToDrive(){
        Query query = new Query.Builder()
                .addFilter(Filters.and(
                        Filters.eq(SearchableField.MIME_TYPE, SQLITE_MIME_TYPE),
                        Filters.eq(SearchableField.TITLE, DATABASE_FILE_TITLE_ON_DRIVE)))
                .build();

        Drive.DriveApi.getAppFolder(mGoogleApiClient).queryChildren(mGoogleApiClient,query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override
            public void onResult(DriveApi.MetadataBufferResult result) {
                MetadataBuffer metadataBuffer = result.getMetadataBuffer();
                switch (metadataBuffer.getCount()) {
                    case 0:
                        Drive.DriveApi.newDriveContents(mGoogleApiClient).setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

                            @Override
                            public void onResult(DriveApi.DriveContentsResult result) {
                                if (!result.getStatus().isSuccess()) {
                                    return;
                                }

                                final DriveContents driveContents = result.getDriveContents();

                                // Perform I/O off the UI thread.
                                new Thread() {
                                    @Override
                                    public void run() {
                                        // write content to DriveContents
                                        OutputStream out = driveContents.getOutputStream();
                                        InputStream in;
                                        try {
                                            in = new FileInputStream(new File(getDatabasePath(AccountProvider.DATABASE_NAME).getPath()));

                                            byte[] buf = new byte[1024];
                                            int len;
                                            while ((len = in.read(buf)) > 0) {
                                                out.write(buf, 0, len);
                                            }
                                            in.close();
                                            out.close();
                                        } catch (IOException e) {
                                            Log.e(TAG, e.getMessage());
                                        }

                                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                                .setTitle(DATABASE_FILE_TITLE_ON_DRIVE)
                                                .setMimeType(SQLITE_MIME_TYPE)
                                                .setStarred(true).build();

                                        // create a file on root folder
                                        Drive.DriveApi.getAppFolder(mGoogleApiClient)
                                                .createFile(mGoogleApiClient, changeSet, driveContents)
                                                .setResultCallback(new ResultCallback<DriveFolder.DriveFileResult>() {

                                                    @Override
                                                    public void onResult(DriveFolder.DriveFileResult result) {
                                                        if (!result.getStatus().isSuccess()) {
                                                            //showMessage("Error while trying to create the file");
                                                            return;
                                                        }
                                                        showSnackbar(R.string.database_exported);
                                                    }
                                                });
                                    }
                                }.start();
                            }
                        });
                        break;
                    case 1:
                        Log.d(TAG, metadataBuffer.get(0).getTitle());
                        DriveFile file = metadataBuffer.get(0).getDriveId().asDriveFile();
                        file.open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null).setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

                            @Override
                            public void onResult(DriveApi.DriveContentsResult result) {
                                if (!result.getStatus().isSuccess()) {
                                    return;
                                }

                                final DriveContents driveContents = result.getDriveContents();

                                // Perform I/O off the UI thread.
                                new Thread() {
                                    @Override
                                    public void run() {
                                        // write content to DriveContents
                                        OutputStream out = driveContents.getOutputStream();
                                        InputStream in;
                                        try {
                                            in = new FileInputStream(new File(getDatabasePath(AccountProvider.DATABASE_NAME).getPath()));

                                            byte[] buf = new byte[1024];
                                            int len;
                                            while ((len = in.read(buf)) > 0) {
                                                out.write(buf, 0, len);
                                            }
                                            in.close();
                                            out.close();
                                        } catch (IOException e) {
                                            Log.e(TAG, e.getMessage());
                                        }

                                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                                .setTitle(DATABASE_FILE_TITLE_ON_DRIVE)
                                                .setMimeType(SQLITE_MIME_TYPE)
                                                .setStarred(true).build();

                                        driveContents.commit(mGoogleApiClient, changeSet);

                                        showSnackbar(R.string.database_exported);
                                    }
                                }.start();
                            }
                        });
                        break;
                    default:

                }
                metadataBuffer.release();
            }
        });
    }

    /**
     * Import database from drive
     */
    private void importFromDrive(){
        Query query = new Query.Builder()
                .addFilter(Filters.and(
                        Filters.eq(SearchableField.MIME_TYPE, SQLITE_MIME_TYPE),
                        Filters.eq(SearchableField.TITLE, DATABASE_FILE_TITLE_ON_DRIVE)))
                .build();

        Drive.DriveApi.getAppFolder(mGoogleApiClient).queryChildren(mGoogleApiClient,query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {

            @Override
            public void onResult(DriveApi.MetadataBufferResult result) {
                MetadataBuffer metadataBuffer = result.getMetadataBuffer();

                if(metadataBuffer.getCount() == 1){

                    DriveFile file = metadataBuffer.get(0).getDriveId().asDriveFile();
                    file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

                        @Override
                        public void onResult(DriveApi.DriveContentsResult result) {

                            if (!result.getStatus().isSuccess()) return;

                            final DriveContents driveContents = result.getDriveContents();

                            // Perform I/O off the UI thread.
                            new Thread() {
                                @Override
                                public void run() {
                                    // write content to DriveContents
                                    InputStream in = driveContents.getInputStream();
                                    OutputStream out;
                                    try {
                                        out = new FileOutputStream(new File(getDatabasePath(AccountProvider.DATABASE_NAME).getPath()),false);

                                        byte[] buf = new byte[1024];
                                        int len;
                                        while ((len = in.read(buf)) > 0) {
                                            out.write(buf, 0, len);
                                        }
                                        in.close();
                                        out.close();

                                        showSnackbar(R.string.database_imported);

                                    } catch (IOException e) {
                                        Log.e(TAG, e.getMessage());
                                    }

                                    // Notify that database has changed
                                    MainActivity.this.getContentResolver().notifyChange(AccountProvider.CONTENT_URI_ACCOUNT, null);
                                    MainActivity.this.getContentResolver().notifyChange(AccountProvider.CONTENT_URI_PAYMENT, null);
                                    MainActivity.this.getContentResolver().notifyChange(AccountProvider.CONTENT_URI_PAYMENT_LABEL, null);
                                    MainActivity.this.getContentResolver().notifyChange(AccountProvider.CONTENT_URI_INVOICE, null);
                                }
                            }.start();
                        }
                    });
                }
                metadataBuffer.release();
            }
        });
    }

    /**
     * Helper method to use Snackbar from outside UI thread.
     * @param resId Resource id for string
     */
    private void showSnackbar(final int resId){

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(findViewById(R.id.fragment_container), resId, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onLabelCreated(Uri uri) {

    }
}
