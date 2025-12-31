/*
 * This file is part of PCAPdroid.
 *
 * PCAPdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PCAPdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PCAPdroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2020-21 - Emanuele Faranda
 */

package com.emanuelef.remote_capture.activities;

import android.os.Bundle;

import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.fragments.ConnectionsFragment;
import com.emanuelef.remote_capture.Utils;

public class ConnectionsActivity extends BaseActivity {
    private static final String TAG = "ConnectionsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setTheme(this);
        setTitle(R.string.connections_view);
        displayBackAction(); // Use explicit back to restore the AppDetailsActivity state
        setContentView(R.layout.fragment_activity);

       getFragmentManager().beginTransaction()
                .replace(R.id.linfra, new ConnectionsFragment())
                .commit();
    }
}
