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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
/*
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
*/
import com.emanuelef.remote_capture.R;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Activity;

public class StepFragment  {
    protected TextView mStepLabel;
    protected ImageView mStepIcon;
    protected Button mStepButton;
    protected Button mSkipButton;
   // protected NavController mNavController;
    protected int mOkColor;
    protected int mWarnColor;
    protected int mDangerColor;

    public StepFragment() {}
    private Context requireContext() {
        return null;
    }
    private Context getContext() {
        return null;
    }
    private Activity requireActivity() {
        return null;
    }
    //@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mitm_wizard, container, false);
    }

    //@Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
       // super.onViewCreated(view, savedInstanceState);

      //  mNavController = Navigation.findNavController(view);
/*
        mStepLabel = view.findViewById(R.id.step_label);
        mStepIcon = view.findViewById(R.id.step_status);
        mStepButton = view.findViewById(R.id.step_button);
        mSkipButton = view.findViewById(R.id.skip_button);

        Context ctx = requireContext();
        mOkColor = ctx.getColor( R.color.ok);
        mWarnColor = ctx.getColor( R.color.warning);
        mDangerColor = ctx.getColor( R.color.danger);
        */
    }

    protected void gotoStep(int action_or_dest) {
        boolean is_last_step = (action_or_dest <= 0);

   /*     if(!is_last_step)
            mNavController.navigate(action_or_dest);
        else
            requireActivity().finish();*/
    }

    protected void nextStep(int action_or_dest) {
        /*
        Context ctx = requireContext();
        boolean is_last_step = (action_or_dest <= 0);
        mStepIcon.setImageDrawable(ctx.getDrawable( R.drawable.ic_check_solid));
        mStepIcon.setColorFilter(mOkColor);

        mSkipButton.setVisibility(View.GONE);
        mStepButton.setEnabled(true);
        mStepButton.setText(!is_last_step ? R.string.app_intro_next_button : R.string.app_intro_done_button);
        //mStepButton.setOnClickListener((view) -> gotoStep(action_or_dest));
        */
    }

    protected void showSkipButton(View.OnClickListener l) {
        /*
        mSkipButton.setVisibility(View.VISIBLE);
        mSkipButton.setOnClickListener(l);
        */
    }
}
