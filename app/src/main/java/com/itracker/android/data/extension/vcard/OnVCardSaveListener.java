package com.itracker.android.data.extension.vcard;


import com.itracker.android.data.BaseUIListener;

public interface OnVCardSaveListener extends BaseUIListener {
    void onVCardSaveSuccess(String account);
    void onVCardSaveFailed(String account);
}
