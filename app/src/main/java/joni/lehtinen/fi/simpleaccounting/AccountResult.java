package joni.lehtinen.fi.simpleaccounting;

import android.net.Uri;

/**
 * Account result interface is for activity to fragment communication.
 * Fragment must implement this to get account result data from activity when it is sent to it.
 */
public interface AccountResult {
    void selectAccountResult(Uri uri);
}
