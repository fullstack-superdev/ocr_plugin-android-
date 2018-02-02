package com.creative.informatics.camera;

/**
 * Created by Travis on 20/12/2017.
 */

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DocDetect extends Activity {

    private Camera mCamera;
    private CameraPreview mPreview;
    private String mFilePath;
    private File pictureFile;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private final static String TAG = "MainActivity";


    private ImageProcess imageProcess;
    private List<Point> cornerPoint,cornerPoint1;

    private double a1,b1,a2,b2,a3,b3,a4,b4;

    private double xscalefactor;
    private double yscalefactor;
    Point p1,p2,p3,p4,p5,p6;

    private TessBaseAPI mTess;
    String datapath = "";

    String Result="";

    ImageView imageView;

    private Bitmap mResultBmp,mResultBmp1;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "OpenCV loaded succeffully!!");
                    break;

                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResources().getIdentifier("doc_detect", "layout", getPackageName()));
        imageView = (ImageView)findViewById(getResources().getIdentifier("imageView", "id", getPackageName()));

//        mFilePath = getIntent().getStringExtra("image_path");
        imageProcess = new ImageProcess();

        String language = "eng";
        datapath = getFilesDir()+ "/tesseract/";
        mTess = new TessBaseAPI();
        checkFile(new File(datapath + "tessdata/"));
        mTess.init(datapath, language);


        if (!OpenCVLoader.initDebug())
		{
			Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
		}
		else
		{
			Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		}

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 1001);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1002);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1003);

        if (mPreview == null)
        {
            mCamera = getCameraInstance();
            mPreview = new CameraPreview(DocDetect.this, mCamera);

        }


        final FrameLayout preview = (FrameLayout)findViewById(getResources().getIdentifier("camera_preview", "id", getPackageName()));
        Camera.Size size = mPreview.getOptimalPreviewSize();
        float ratio = (float)size.width/size.height;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenhei = displayMetrics.heightPixels;
        int screenwid = displayMetrics.widthPixels;

        int new_width=0, new_height=0;
        if(screenwid/screenhei<ratio){
            new_width = Math.round(screenwid*ratio);
            new_height = screenwid;
        }else{
            new_width = screenwid;
            new_height = Math.round(screenwid/ratio);
        }

        RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams)preview.getLayoutParams();
        param.width = new_height;
        param.height = new_width;
        preview.setLayoutParams(param);
        preview.addView(mPreview);

        pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        mFilePath = pictureFile.getAbsolutePath();

        Button captureButton = (Button)findViewById(getResources().getIdentifier("button_capture", "id", getPackageName()));
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCamera.takePicture(null, null, mPicture);
                    }
                }
        );
    }

    private void checkFile(File dir) {
        if (!dir.exists()&& dir.mkdirs()){
            copyFiles();
        }
        if(dir.exists()) {
            String datafilepath = datapath+ "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);

            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    private void copyFiles() {
        try {
            String filepath = datapath + "/tessdata/eng.traineddata";
            AssetManager assetManager = getAssets();

            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }

            outstream.flush();
            outstream.close();
            instream.close();

            File file = new File(filepath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Camera getCameraInstance(){
        if(mCamera != null)
            return mCamera;
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return c;
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            if (pictureFile.exists()) {
                pictureFile.delete();
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);

                Bitmap realImage = BitmapFactory.decodeByteArray(data, 0, data.length);

                ExifInterface exif=new ExifInterface(pictureFile.toString());

                Log.d("EXIF value", exif.getAttribute(ExifInterface.TAG_ORIENTATION));
                if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("6")){
                    realImage= rotate(realImage, 90);
                } else if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("8")){
                    realImage= rotate(realImage, 270);
                } else if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("3")){
                    realImage= rotate(realImage, 180);
                } else if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("0")){
                    realImage= rotate(realImage, 90);
                }

                boolean bo = realImage.compress(Bitmap.CompressFormat.JPEG, 30, fos);

                fos.close();


                if(mPreview.x1 > -1){

                    a1 = mPreview.x1;
                    b1 = mPreview.y1;
                    a2 = mPreview.x2;
                    b2 = mPreview.y2;
                    a3 = mPreview.x3;
                    b3 = mPreview.y3;
                    a4 = mPreview.x4;
                    b4 = mPreview.y4;
                    xscalefactor = (double)realImage.getWidth()/mPreview.mDefaultSize.height;
                    yscalefactor = (double)realImage.getHeight()/mPreview.mDefaultSize.width;

                    Log.d("ScaleFactor", xscalefactor +":"+yscalefactor);
                    double a0 = (a1+a2+a3+a4)/4;
                    double b0 = (b1+b2+b3+b4)/4;
                    List<Point> cp = new ArrayList<Point>();
                    Point n1=new Point(a1,b1);
                    Point n2=new Point(a2,b2);
                    Point n3=new Point(a3,b3);
                    Point n4=new Point(a4,b4);
                    cp.add(n1);
                    cp.add(n2);
                    cp.add(n3);
                    cp.add(n4);

                    Map<Integer, Point> orderedPoints=ImageProcess.getOrderedPoints(cp);
                    Point pa = orderedPoints.get(0);
                    Point pb = orderedPoints.get(1);
                    Point pc = orderedPoints.get(2);
                    Point pd = orderedPoints.get(3);
                    if (pa!=null && pb !=null && pc != null && pd !=null) {
                        Point c1 = new Point((pa.x + pc.x) / 2.0, (pa.y + pc.y) / 2.0);
                        Point c2 = new Point((pb.x + pd.x) / 2.0, (pb.y + pd.y) / 2.0);
                        p1 = new Point(pa.x * xscalefactor, pa.y * yscalefactor);
                        p2 = new Point(pb.x * xscalefactor, pb.y * yscalefactor);
                        p3 = new Point(c1.x * xscalefactor, c1.y * yscalefactor);
                        p4 = new Point(c2.x * xscalefactor, c2.y * yscalefactor);
                        p5 = new Point(pc.x * xscalefactor, pc.y * yscalefactor);
                        p6 = new Point(pd.x * xscalefactor, pd.y * yscalefactor);
                        cornerPoint = new ArrayList<Point>();
                        cornerPoint.add(p1);
                        cornerPoint.add(p2);
                        cornerPoint.add(p3);
                        cornerPoint.add(p4);
                        cornerPoint1 = new ArrayList<Point>();
                        cornerPoint1.add(p3);
                        cornerPoint1.add(p4);
                        cornerPoint1.add(p5);
                        cornerPoint1.add(p6);

                        Mat startM = Converters.vector_Point2f_to_Mat(cornerPoint);
                        mResultBmp = imageProcess.warpAuto(realImage, startM);

                        Mat Upper = new Mat();
                        Utils.bitmapToMat(mResultBmp, Upper);

                        Utils.matToBitmap(Upper, mResultBmp);
                        Mat startM1 = Converters.vector_Point2f_to_Mat(cornerPoint1);
                        mResultBmp1 = imageProcess.warpAuto(realImage, startM1);
                        File fdelete = new File(mFilePath);
                        if (fdelete.exists()) {
                            if (fdelete.delete()) {
                                System.out.println("file Deleted :" + mFilePath);
                            } else {
                                System.out.println("file not Deleted :" + mFilePath);
                            }
                        }

                        //OCR using Tesseract api
                        String OCRresult = null, OCRresult1 = null;
                        mTess.setImage(mResultBmp);

                        OCRresult = mTess.getUTF8Text();
                        mTess.setImage(mResultBmp1);
                        OCRresult1 = mTess.getUTF8Text();

                        //Return Result
                        String str="";
                        if (!(checkString(OCRresult)=="" && checkString(OCRresult1) ==""))
                            str = checkString(OCRresult) + "/" + checkString(OCRresult1);
                        Log.d(TAG,str);

                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("data", str);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Config.request.results.put(obj);
                        Config.pendingRequests.resolveWithSuccess(Config.request);

                        finish();
                    }
                    // mCamera.startPreview();
                }
                else {
                    Toast.makeText(DocDetect.this, "Docs is not detected.Please try again!!", Toast.LENGTH_LONG).show();
                }

            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }


        private String checkString(String OCRstr){
            Pattern p = Pattern.compile( "([0-9]*)" );
            Matcher m ;
            String newResult = "";
            for (int i = 0 ; i < OCRstr.length(); i++){
                String str = OCRstr.substring(i,i+1);
                m = p.matcher(str);
                if (m.matches()) newResult+=str;
            }
            return newResult;
        }
    };

    private static File getOutputMediaFile(int type){

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraScan");

        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 1001) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.CAMERA)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                       Log.d("CAMERA","Permission Success");
                    }
                }
            }
            finish();
        }
        if(requestCode == 1002) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        Log.d("WRITE","SUCCESS");
                    }
                }
            }
        }
        if(requestCode == 1003) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        Log.d("READ","SUCCESS");
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.setRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }


}
