package com.localytics.android.itracker.data.extension.vcard;


import com.localytics.android.itracker.data.BaseUIListener;

public interface OnVCardSaveListener extends BaseUIListener {
    void onVCardSaveSuccess(String account);
    void onVCardSaveFailed(String account);
}
