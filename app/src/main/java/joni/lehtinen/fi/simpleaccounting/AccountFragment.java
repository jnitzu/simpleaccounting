package joni.lehtinen.fi.simpleaccounting;


import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Account fragment is fragment that ties together account data and payment data.
 */
public class AccountFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ACCOUNT_ID = "joni.lehtinen.fi.simpleaccounting.account_fragment_id";
    public static final String ACCOUNT_TITLE = "joni.lehtinen.fi.simpleaccounting.accountfragment_title";
    public static final String ACCOUNT_TYPE_TITLE = "joni.lehtinen.fi.simpleaccounting.accountfragment_type_title";

    private static final String[] PROJECTION = new String[]{
            AccountProvider.ACCOUNT.ID.fullName(),
            AccountProvider.ACCOUNT.BALANCE.fullName()};

    private FrameLayout mToolbarFramelayout;
    private TextView mBalanceTextView;

    private int mID;
    private String mTitle;
    private String mTypeTitle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Main view that payment list fragment is tied to
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // Top view that has account info in it
        View infoView = inflater.inflate(R.layout.fragment_account_info, container, false);

        mID = getArguments().getInt(ACCOUNT_ID);
        mTitle = getArguments().getString(ACCOUNT_TITLE);
        mTypeTitle = getArguments().getString(ACCOUNT_TYPE_TITLE);


        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if(actionBar != null)
            actionBar.setTitle(mTitle);

        mBalanceTextView = (TextView)infoView.findViewById(R.id.account_balance);

        TextView type = (TextView)infoView.findViewById(R.id.account_type);
        type.setText(mTypeTitle + "");

        Fragment fragment = new PaymentListFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(PaymentListFragment.PAYMENT_LIST_ACCOUNT_ID, mID);
        fragment.setArguments(bundle);

        getFragmentManager().beginTransaction()
                .add(R.id.account_fragment_container, fragment)
                .commit();

        mToolbarFramelayout = (FrameLayout)getActivity().findViewById(R.id.toolbar_extra_container);
        mToolbarFramelayout.addView(infoView);

        getLoaderManager().initLoader(0, null, this);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mToolbarFramelayout.removeAllViews();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), AccountProvider.CONTENT_URI_ACCOUNT, PROJECTION, AccountProvider.ACCOUNT.ID.fullName() + " IS " + mID, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update balance then loader has finished loading cursor
        data.moveToFirst();
        mBalanceTextView.setText(data.getDouble(1)+"");
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mBalanceTextView.setText("");
    }

}
