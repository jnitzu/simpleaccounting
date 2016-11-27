package joni.lehtinen.fi.simpleaccounting;

import android.net.Uri;

/**
 * Label result interface is for activity to fragment communication.
 * Fragment must implement this to get label result data from activity when it is sent to it.
 */
public interface LabelResult {
    void selectLabelResult(Uri uri);
}
