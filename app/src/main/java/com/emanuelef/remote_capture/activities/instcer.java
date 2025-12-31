package com.emanuelef.remote_capture.activities;

import android.app.Activity;
import android.os.Bundle;
import com.emanuelef.remote_capture.interfaces.MitmListener;
import android.content.Context;
import android.widget.Toast;
import com.emanuelef.remote_capture.MitmAddon;
import java.security.cert.X509Certificate;
import android.annotation.Nullable;
import com.emanuelef.remote_capture.Log;
import com.emanuelef.remote_capture.Utils;
import android.content.Intent;
import android.security.KeyChain;
import android.content.ActivityNotFoundException;
import com.emanuelef.remote_capture.R;
import android.os.Build;
import java.nio.charset.StandardCharsets;
import android.net.Uri;
import java.io.PrintWriter;
import java.io.IOException;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;

public class instcer extends Activity implements MitmListener{
    private static final String TAG = "InstallCertificate";
    private MitmAddon mAddon;
    private String mCaPem;
    private X509Certificate mCaCert;
    private boolean mFallbackExport;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setTheme(this);
        LinearLayout linl= new LinearLayout(this);
        linl.setOrientation(LinearLayout.VERTICAL);
        linl.setGravity(Gravity.CENTER);
        TextView tv= new TextView(this);
        tv.setGravity(Gravity.CENTER);
        tv.setText("export ca certificate for tls decryption if isnt installd");
        linl.addView(tv);
        setContentView(linl);
        mAddon = new MitmAddon(requireContext(), this);
        if(!Utils.isCAInstalled(mCaCert)) {
            if(!mAddon.isConnected()) {
                if (!mAddon.connect(0)) {
                    //LogUtil.logToFile("not con");
                }
                //LogUtil.logToFile("not con1");
            }
            //LogUtil.logToFile("not con2");
        }
    }

    @Override
    public void onMitmGetCaCertificateResult(@Nullable String ca_pem) {
        mAddon.disconnect();

        // NOTE: this may be called when context is null
        Context context = getContext();
        if(context == null) {
            Log.d(TAG, "null context");
            return;
        }

        mCaPem = ca_pem;

        // NOTE: onMitmGetCaCertificateResult can be called by fallbackToCertExport
       // mStepButton.setText(canInstallCertViaIntent() ? R.string.install_action : R.string.export_action);

        if(mCaPem != null) {
            Log.d(TAG, "Got certificate");
            //Log.d(TAG, "certificate: " + cert_str);
            mCaCert = Utils.x509FromPem(mCaPem);

            if(mCaCert != null) {
                if(Utils.isCAInstalled(mCaCert))
                    certOk();
                else {
                    // Cert not installed
                    MitmAddon.setDecryptionSetupDone(context, false);
                   /* mStepIcon.setColorFilter(mWarnColor);
                    mStepButton.setEnabled(true);

                    if(canInstallCertViaIntent())
                        mStepLabel.setText(R.string.install_ca_certificate);
                    else
                        mStepLabel.setText(R.string.export_ca_certificate);
                    mStepButton.setOnClickListener(new View.OnClickListener() {
*
                            @Override
                            public void onClick(View p1) {
*/

                                if(canInstallCertViaIntent())
                                    installCaCertificate();
                                else
                                    exportCaCertificate();
                           // }});
                }
            } else {
                Toast.makeText(getApplicationContext(), "addon did not return certificate", Toast.LENGTH_LONG).show();
                certFail();
            }
        }
    }

    private Context getContext() {
        return this;
    }
    private void certOk() {
       // mStepLabel.setText(R.string.cert_installed_correctly);
        MitmAddon.setCAInstallationSkipped(requireContext(), false);
        // nextStep(R.id.navto_done);
    }

    private void certFail() {
       // mStepLabel.setText(R.string.ca_cert_export_failed);
        //Utils.setTextUrls(mStepLabel, R.string.ca_cert_export_failed, "https://dontkillmyapp.com/xiaomi#app-battery-saver");
       // mStepIcon.setColorFilter(mDangerColor);
        MitmAddon.setDecryptionSetupDone(requireContext(), false);
    }

    private Context requireContext() {
        return getContext();
    }

    private boolean canInstallCertViaIntent() {
        // On Android < 11, an intent can be used for cert installation
        // On Android 11+, users must manually install the certificate from the settings
        return((Build.VERSION.SDK_INT < Build.VERSION_CODES.R) && !mFallbackExport);
    }

    private void fallbackToCertExport() {
        // If there are problems with the cert installation via Intent, fallback to export+install
        mFallbackExport = true;
        onMitmGetCaCertificateResult(mCaPem);
    }

    private void exportCaCertificate() {
        String fname = "PCAPdroid_CA.crt";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/x-x509-ca-cert");
        intent.putExtra(Intent.EXTRA_TITLE, fname);
        startActivityForResult(intent,56);
        //if(!Utils.launchFileDialog(requireContext(), intent, certExportLauncher))
        //certFail();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==Activity.RESULT_OK){
            if(requestCode==56){
                Log.e("c","reso");
                certExportResult(data);
            }else if(requestCode==57){
                certInstallResult(resultCode);
            }
        }else{
            if(requestCode==56){
                certFail();
            }else if(requestCode==57){
                certInstallResult(resultCode);
            }
        }
    }
    private void certExportResult(Intent data) {
       // if((result.getResultCode() == Activity.RESULT_OK) && (result.getData() != null)) {
            Context ctx = requireContext();
            Uri cert_uri = data.getData();
            boolean written = false;

            try{
                PrintWriter writer = new PrintWriter(ctx.getContentResolver().openOutputStream(cert_uri, "rwt"));
                writer.print(mCaPem);
                writer.flush();
                written = true;
                //LogUtil.logToFile("written..."+mCaPem);
            } catch (IOException e) {
                LogUtil.logToFile(e.toString());
                e.printStackTrace();
            }

            if(written)
                Utils.showToastLong(ctx, R.string.cert_exported_now_installed);
        //}
    }

    private void certInstallResult(int res) {
        if((res == Activity.RESULT_OK) && Utils.isCAInstalled(mCaCert))
            certOk();
        else
            fallbackToCertExport();
    }
    private void installCaCertificate() {
        Intent intent = KeyChain.createInstallIntent();
        intent.putExtra(KeyChain.EXTRA_NAME, "PCAPdroid CA");
        intent.putExtra(KeyChain.EXTRA_CERTIFICATE, mCaPem.getBytes(StandardCharsets.UTF_8));

        try {
            startActivityForResult(intent,57);
            // certInstallLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Utils.showToastLong(getApplicationContext(), R.string.no_intent_handler_found);
            fallbackToCertExport();
        }
    }
    @Override
    public void onMitmServiceConnect() {
        Context ctx = getContext();
        if(ctx == null)
            return;

        if(!mAddon.requestCaCertificate()) {
            Toast.makeText(getApplicationContext(), "requestCaCertificate failed", Toast.LENGTH_LONG).show();
            certFail();
        }
    }

    @Override
    public void onMitmServiceDisconnect() {
        Context ctx = getContext();
        if(ctx == null)
            return;

        if(mCaPem == null) {
            Toast.makeText(getApplicationContext(), "addon disconnected", Toast.LENGTH_LONG).show();
            certFail();
        }
    }
}
