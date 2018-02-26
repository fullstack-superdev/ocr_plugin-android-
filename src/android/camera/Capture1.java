/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package com.creative.informatics.camera;

import java.io.IOException;
import android.os.Bundle;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import com.creative.informatics.camera.PendingRequests.Request;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.MediaPlayer;

public class Capture1 extends CordovaPlugin {

    private static final int RECOGNIZE_ID = 0;     // Constant for capture audio
    //private static final int CAPTURE_VIDEO = 2;     // Constant for capture video
    private static final String LOG_TAG = "Recognize";

    private static final int RECOGNIZED_FAILED = 3;
    private static final int CAPTURE_PERMISSION_DENIED = 4;

    private boolean cameraPermissionInManifest;     // Whether or not the CAMERA permission is declared in AndroidManifest.xml

    private final PendingRequests pendingRequests = new PendingRequests();

//    public void setContext(Context mCtx)
//    {
//        if (CordovaInterface.class.isInstance(mCtx))
//            cordova = (CordovaInterface) mCtx;
//        else
//            LOG.d(LOG_TAG, "ERROR: You must use the CordovaInterface for this to work correctly. Please implement it in your activity");
//    }

    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();

        // CB-10670: The CAMERA permission does not need to be requested unless it is declared
        // in AndroidManifest.xml. This plugin does not declare it, but others may and so we must
        // check the package info to determine if the permission is present.

        cameraPermissionInManifest = false;
        try {
            PackageManager packageManager = this.cordova.getActivity().getPackageManager();
            String[] permissionsInPackage = packageManager.getPackageInfo(this.cordova.getActivity().getPackageName(), PackageManager.GET_PERMISSIONS).requestedPermissions;
            if (permissionsInPackage != null) {
                for (String permission : permissionsInPackage) {
                    if (permission.equals(Manifest.permission.CAMERA)) {
                        cameraPermissionInManifest = true;
                        break;
                    }
                }
            }
        } catch (NameNotFoundException e) {
            // We are requesting the info for our package, so this should
            // never be caught
            LOG.e(LOG_TAG, "Failed checking for CAMERA permission in manifest", e);
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        JSONObject options = args.optJSONObject(0);

        if (action.equals("docRecognize")) {
            this.docRecognize(pendingRequests.createRequest(RECOGNIZE_ID, options, callbackContext));
        }
        else {
            return false;
        }

        return true;
    }

    /**
     * Sets up an intent to capture images.  Result handled by onActivityResult()
     */
    private void docRecognize(Request req) {
        boolean needExternalStoragePermission =
                !PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        boolean needExternalWStoragePermission =
                !PermissionHelper.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        boolean needCameraPermission = cameraPermissionInManifest &&
                !PermissionHelper.hasPermission(this, Manifest.permission.CAMERA);

        if (needExternalStoragePermission || needCameraPermission || needExternalWStoragePermission) {
            if(needExternalWStoragePermission && needExternalStoragePermission && needCameraPermission){
                PermissionHelper.requestPermissions(this, req.requestCode, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA});
            }
            else if (needExternalStoragePermission && needCameraPermission) {
                PermissionHelper.requestPermissions(this, req.requestCode, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA});
            } else if (needExternalStoragePermission) {
                PermissionHelper.requestPermission(this, req.requestCode, Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                PermissionHelper.requestPermission(this, req.requestCode, Manifest.permission.CAMERA);
            }
        } else {
            Intent intent = new Intent(this.cordova.getActivity(), OcrCaptureActivity.class);
//            intent.putExtra(OcrCaptureActivity.AutoFocus, autoFocus.isChecked());
//            intent.putExtra(OcrCaptureActivity.UseFlash, useFlash.isChecked());
            intent.putExtra(OcrCaptureActivity.OCR_OPTION, req.options.toString());
            this.cordova.startActivityForResult((CordovaPlugin) this, intent, req.requestCode);
        }
    }

    /**
     * Get the Image specific attributes
     *
     * @param filePath path to the file
     * @param obj represents the Media File Data
     * @param video if true get video attributes as well
     * @return a JSONObject that represents the Media File Data
     * @throws JSONException
     */
    private JSONObject getAudioVideoData(String filePath, JSONObject obj, boolean video) throws JSONException {
        MediaPlayer player = new MediaPlayer();
        try {
            player.setDataSource(filePath);
            player.prepare();
            obj.put("duration", player.getDuration() / 1000);
            if (video) {
                obj.put("height", player.getVideoHeight());
                obj.put("width", player.getVideoWidth());
            }
        } catch (IOException e) {
            LOG.d(LOG_TAG, "Error: loading video file");
        }
        return obj;
    }
    /**
     * Called when the video view exits.
     *
     * @param requestCode       The request code originally supplied to startActivityForResult(),
     *                          allowing you to identify who this result came from.
     * @param resultCode        The integer result code returned by the child activity through its setResult().
     * @param intent            An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     * @throws JSONException
     */
    public void onActivityResult(int requestCode, int resultCode, final Intent intent) {
        final Request req = pendingRequests.get(requestCode);

        // Result received okay
        if (resultCode == Activity.RESULT_OK) {
            Runnable processActivityResult = new Runnable() {
                @Override
                public void run() {
                    switch(req.action) {
                        case RECOGNIZE_ID:
                            onRecognizeActivityResult(req, intent);
                            break;
                    }
                }
            };

            this.cordova.getThreadPool().execute(processActivityResult);
        } else {
            try {
                if (req.action == RECOGNIZE_ID) {
                    pendingRequests.resolveWithFailure(req, createErrorObject(RECOGNIZED_FAILED, "Canceled Recognize."));
                } else {
                    pendingRequests.resolveWithFailure(req, createErrorObject(RECOGNIZED_FAILED, "Unknow Action."));
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void onRecognizeActivityResult(Request req, Intent intent) {
        String data = null;

        if (intent != null){
            // Get the uri of the video clip
            data = intent.getStringExtra("recognized_id_string");
        }

        if( data == null){
            data = "Not recognized";
        }
        req.results.put(data);  //createRecognizedResult(data)
        pendingRequests.resolveWithSuccess(req);
    }


    /**
     * Creates a JSONObject that represents a File from the Uri
     *
     * @param data the Uri of the audio/image/video
     * @return a JSONObject that represents a File
     * @throws IOException
     */
    private JSONObject createRecognizedResult(String data) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    private JSONObject createErrorObject(int code, String message) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("code", code);
            obj.put("message", message);
        } catch (JSONException e) {
            // This will never happen
        }
        return obj;
    }

    private void executeRequest(Request req) {
        switch (req.action) {
            case RECOGNIZE_ID:
                this.docRecognize(req);
                break;
        }
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        Request req = pendingRequests.get(requestCode);

        if (req != null) {
            boolean success = true;
            for(int r:grantResults) {
                if (r == PackageManager.PERMISSION_DENIED) {
                    success = false;
                    break;
                }
            }

            if (success) {
                executeRequest(req);
            } else {
                pendingRequests.resolveWithFailure(req, createErrorObject(CAPTURE_PERMISSION_DENIED, "Permission denied."));
            }
        }
    }

    public Bundle onSaveInstanceState() {
        return pendingRequests.toBundle();
    }

    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        pendingRequests.setLastSavedState(state, callbackContext);
    }
}
