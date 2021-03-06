/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.itracker.android.data.roster;

import com.itracker.android.data.BaseUIListener;
import com.itracker.android.data.entity.BaseEntity;

import java.util.Collection;

/**
 * Listener for contact change.
 *
 * @author alexander.ivanov
 */
public interface OnContactChangedListener extends BaseUIListener {

    /**
     * Contacts changed.
     *
     * @param entities
     */
    void onContactsChanged(Collection<BaseEntity> entities);

}
