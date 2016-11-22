package com.itracker.android.ui.listener;

import com.itracker.android.data.BaseUIListener;
import com.itracker.android.ui.widget.CollectionView;


public interface OnPhotoInventoryUpdatedListener extends BaseUIListener {
    void onPhotoInventoryUpdated(CollectionView.Inventory photoInventory);
}
