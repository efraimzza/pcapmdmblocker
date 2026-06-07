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
 * Copyright 2022 - Emanuele Faranda
 */

package com.emanuelef.remote_capture.fragments.mitmwizard;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.view.View;

import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.MitmAddon;
import com.emanuelef.remote_capture.Utils;
import android.content.Context;
import android.app.Activity;

public class InstallAddon extends StepFragment {
    @Override
    public void onViewCreated( View view,  Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Utils.setTextUrls(mStepLabel, R.string.install_the_mitm_addon, MitmAddon.REPOSITORY);

        String new_ver = MitmAddon.getNewVersionAvailable(requireContext());
        if(new_ver.isEmpty() && MitmAddon.isInstalled(requireContext()))
            addonOk();
        else
            installAddon(new_ver);
    }

    //@Override
    public void onResume() {
       // super.onResume();

        if(MitmAddon.getNewVersionAvailable(requireContext()).isEmpty() &&
                MitmAddon.isInstalled(requireContext()))
            addonOk();
    }

    private void addonOk() {
        //nextStep(R.id.navto_install_cert);
    }

    private void installAddon(final String new_ver) {
        String installed_ver = MitmAddon.getInstalledVersionName(requireContext());
/*
        if(installed_ver.isEmpty()) {
            mStepLabel.setText(R.string.install_the_mitm_addon);
            mStepButton.setText(R.string.install_action);
        } else if(Utils.isSemanticVersionCompatible(installed_ver, new_ver)) {
            mStepLabel.setText(R.string.mitm_addon_update_available);
            mStepButton.setText(R.string.update_action);
          //  showSkipButton(view -> gotoStep(R.id.navto_install_cert));
        } else if(MitmAddon.getInstalledVersion(requireContext()) < MitmAddon.PACKAGE_VERSION_CODE) {
            mStepLabel.setText(R.string.mitm_addon_new_version);
            mStepButton.setText(R.string.update_action);
        } else {
            mStepLabel.setText(getString(R.string.mitm_addon_bad_version, MitmAddon.PACKAGE_VERSION_NAME));
            mStepIcon.setColorFilter(mDangerColor);
            mStepButton.setText(R.string.install_action);
            mStepButton.setEnabled(false);
            return;
        }*/

        mStepButton.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View p1) {
                
                
            String target_ver = new_ver.isEmpty() ? MitmAddon.PACKAGE_VERSION_NAME : new_ver;

            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(MitmAddon.getGithubReleaseUrl(target_ver)));
            Utils.startActivity(requireContext(), browserIntent);
        }});
    }
    private Context requireContext() {
        return null;
    }
    private Activity requireActivity() {
        return null;
    }
}
