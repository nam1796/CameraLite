package com.intelligorithm.cameralite;



import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CameraFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CameraFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CameraFragment extends android.support.v4.app.Fragment implements SurfaceHolder.Callback {

    private FrameLayout cameraFrame;
    private CameraTextureView cameraTextureView;
    private int currentCameraId;
    private int numOfCameras;
    private int frontCameraId = Integer.MAX_VALUE;
    private int backCameraId = Integer.MAX_VALUE;
    private boolean isFacingBack = true;
    public final int MEDIA_TYPE_IMAGE = 1;
    public final int MEDIA_TYPE_VIDEO = 2;

    private static  final int FOCUS_AREA_SIZE= 300;

    private SurfaceView transparentView;
    private SurfaceHolder holderTransparent;

    private Paint paint;
    private Canvas canvas;
    private float RectLeft;
    private float RectTop;
    private float RectRight;
    private float RectBottom;


    private float mDist;

    public String TAG = "CameraLite";



    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CameraFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CameraFragment newInstance(String param1, String param2) {
        CameraFragment fragment = new CameraFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public CameraFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        numOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        for (int i = 0; i < numOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                backCameraId = i;
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                frontCameraId = i;
            }
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_camera, container, false);

        cameraFrame = (FrameLayout) rootView.findViewById(R.id.cameraFrame);
        cameraTextureView = new CameraTextureView(MainApplication.getAppContext(), getCameraInstance(backCameraId));

        cameraFrame.addView(cameraTextureView);



        transparentView = new SurfaceView(getContext());
        transparentView.setZOrderOnTop(true);



        holderTransparent = transparentView.getHolder();
       // holderTransparent.setFormat(PixelFormat.TRANSPARENT);
        holderTransparent.setFormat(PixelFormat.TRANSLUCENT);
        holderTransparent.addCallback(this);
      //  holderTransparent.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


        cameraFrame.addView(transparentView);

        View.OnTouchListener onTouchListner = new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {


                // Get the pointer ID
                Camera.Parameters params = cameraTextureView.getmCamera().getParameters();
                int action = event.getAction();


                if (event.getPointerCount() > 1) {
                    // handle multi-touch events
                    if (action == MotionEvent.ACTION_POINTER_DOWN) {

                        Log.v(TAG, "Action Pointer Down");

                        mDist = getFingerSpacing(event);

                    } else if (action == MotionEvent.ACTION_MOVE && params.isZoomSupported()) {

                        Log.v(TAG, "ACTION MOVE");

                        cameraTextureView.getmCamera().cancelAutoFocus();
                        handleZoom(event, params);

                    }
                } else {
                    // handle single touch events
                    if (action == MotionEvent.ACTION_UP) {

                        Log.v(TAG, "Action UP :v ");



                        handleFocus(event, params);
                    }
                }







                return true;
            }
        };

        transparentView.setOnTouchListener(onTouchListner);



        final Button switchCameraButton = (Button) rootView.findViewById(R.id.switchCameraButton);
        final Button captureButton = (Button) rootView.findViewById(R.id.captureButton);
        final Button crossButton = (Button) rootView.findViewById(R.id.crossButton);
        final Button sendButton = (Button) rootView.findViewById(R.id.sendButton);

        // set the width and height of the button to the height of the screen divided by 14
        int switchCameraButtonWidthHeight = getResources().getDisplayMetrics().heightPixels / 14;
        ViewGroup.LayoutParams switchCameraButtonLayoutParams = switchCameraButton.getLayoutParams();
        switchCameraButtonLayoutParams.height = switchCameraButtonWidthHeight;
        switchCameraButtonLayoutParams.width = switchCameraButtonWidthHeight;
        switchCameraButton.setLayoutParams(switchCameraButtonLayoutParams);
        switchCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        int captureButtonWidthHeight = getResources().getDisplayMetrics().heightPixels / 7;
        ViewGroup.LayoutParams captureButtonLayoutParams = captureButton.getLayoutParams();
        captureButtonLayoutParams.height = captureButtonWidthHeight;
        captureButtonLayoutParams.width = captureButtonWidthHeight;
        captureButton.setLayoutParams(captureButtonLayoutParams);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Runnable takePictureRunnable = new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bitmap = cameraTextureView.getBitmap();
                        cameraTextureView.getmCamera().stopPreview();

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
                        byte[] byteArray = stream.toByteArray();

                        //If the user wants to save the photo on their phone
                        /*File pictureFile = getOutputMediaFile();
                        pictureFile.createNewFile();
                        FileOutputStream fos = new FileOutputStream(pictureFile);
                        fos.write(byteArray);
                        fos.close();*/

                        // do this when the user presses the send button, otherwise keep bitmap offline until he sends
                        /*ParseFile imageFile = new ParseFile("snap.jpg", byteArray);
                        imageFile.saveInBackground();
                        ParseObject snap = new ParseObject("Snap");
                        snap.put("imageFile", imageFile);
                        snap.saveInBackground();*/
                    }
                };
                Thread takePictureThread = new Thread(takePictureRunnable);
                takePictureThread.start();

                captureButton.getStateListAnimator().jumpToCurrentState();
                doAnimation(R.animator.scale_in, crossButton, sendButton);
                getCircleAnimator(captureButton).start();
                getCircleAnimator(switchCameraButton).start();



                /*cameraTextureView.getmCamera().takePicture(new Camera.ShutterCallback() {
                    @Override
                    public void onShutter() {
                        //cameraTextureView.getmCamera().stopPreview();
                    }
                }, null, new Camera.PictureCallback() { //instead of take picture, take a screenshot of the cameraframe
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        try {
                            File pictureFile = getOutputMediaFile();
                            pictureFile.createNewFile();
                            FileOutputStream fos = new FileOutputStream(pictureFile);
                            fos.write(data);
                            fos.close();
                        } catch (FileNotFoundException e) {
                            Log.d("ERROR ", "File not found: " + e.getMessage());
                        } catch (IOException e) {
                            Log.d("ERROR ", "Error accessing file: " + e.getMessage());
                        }
                        camera.stopPreview();
                        captureButton.setVisibility(View.GONE);
                        switchCameraButton.setVisibility(View.GONE);
                        crossButton.setVisibility(View.VISIBLE);
                    }
                });*/
            }
        });

        int crossButtonWidthHeight = getResources().getDisplayMetrics().heightPixels / 20;
        ViewGroup.LayoutParams crossButtonLayoutParams = crossButton.getLayoutParams();
        crossButtonLayoutParams.height = crossButtonWidthHeight;
        crossButtonLayoutParams.width = crossButtonWidthHeight;
        crossButton.setLayoutParams(crossButtonLayoutParams);
        crossButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        cameraTextureView.getmCamera().startPreview();
                    }
                });
                thread.start();

                crossButton.getStateListAnimator().jumpToCurrentState();
                getCircleAnimator(crossButton).start();
                getCircleAnimator(sendButton).start();
                doAnimation(R.animator.scale_in, switchCameraButton, captureButton);



            }
        });

        int sendButtonWidthHeight = getResources().getDisplayMetrics().heightPixels / 12;
        ViewGroup.LayoutParams sendButtonLayoutParams = sendButton.getLayoutParams();
        sendButtonLayoutParams.height = sendButtonWidthHeight;
        sendButtonLayoutParams.width = sendButtonWidthHeight;
        sendButton.setLayoutParams(sendButtonLayoutParams);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               /* FragmentManager fm = getParentFragment().getActivity().getSupportFragmentManager();
                fm.beginTransaction()
                        .replace(R.id.mainFrame, new SendFragment())
                        .commit(); */
            }
        });





        return rootView;
    }

    private void doAnimation(int animationId, View... myObjects) {

        ArrayList<AnimatorSet> animatorSets = new ArrayList<AnimatorSet>();

        for (View myObject : myObjects) {
            AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(MainApplication.getAppContext(), animationId);
            set.setTarget(myObject);
            animatorSets.add(set);
        }

        for (AnimatorSet animatorSet : animatorSets) {
            animatorSet.start();
        }
    }

    private Animator getCircleAnimator(final View myView) {
        // get the center for the clipping circle
        int cx = myView.getWidth() / 2;
        int cy = myView.getHeight() / 2;

        // get the initial radius for the clipping circle
        int initialRadius = myView.getWidth();

        // create the animation (the final radius is zero)
        Animator anim =
                ViewAnimationUtils.createCircularReveal(myView, cx, cy, initialRadius, 0);

        anim.setInterpolator(new DecelerateInterpolator(3));

        // make the view invisible when the animation is done
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                myView.setVisibility(View.GONE);
            }
        });
        return anim;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (cameraTextureView.getmCamera() == null) {
            cameraTextureView.setmCamera(getCameraInstance(backCameraId));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        cameraTextureView.getmCamera().release();
        cameraTextureView.setmCamera(null);
    }

    public Camera getCameraInstance(int cameraId){
        Camera c = null;
        try {
            c = Camera.open(cameraId); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }
    private void DrawFocusRect(float RectLeft, float RectTop, float RectRight, float RectBottom, int color)
    {

        Log.v("CameraLite", "on DrawFocusRect");

        canvas = holderTransparent.lockCanvas();
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        //border's properties
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(3);
        canvas.drawRect(RectLeft, RectTop, RectRight, RectBottom, paint);





        holderTransparent.unlockCanvasAndPost(canvas);
    }

    private Rect calculateTapArea(float x, float y) {
        int left = clamp(Float.valueOf((x / cameraTextureView.getWidth()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);
        int top = clamp(Float.valueOf((y / cameraTextureView.getHeight()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);

        return new Rect(left, top, left + FOCUS_AREA_SIZE, top + FOCUS_AREA_SIZE);
    }

    private int clamp(int touchCoordinateInCameraReper, int focusAreaSize) {
        int result;
        if (Math.abs(touchCoordinateInCameraReper)+focusAreaSize/2>1000){
            if (touchCoordinateInCameraReper>0){
                result = 1000 - focusAreaSize/2;
            } else {
                result = -1000 + focusAreaSize/2;
            }
        } else{
            result = touchCoordinateInCameraReper - focusAreaSize/2;
        }
        return result;
    }


    private void handleZoom(MotionEvent event, Camera.Parameters params) {
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        float newDist = getFingerSpacing(event);

        if (newDist > mDist) {
            //zoom in
            if ((zoom + 2) < maxZoom)
                zoom += 2;
        } else if (newDist < mDist) {
            //zoom out
            if ((zoom - 2) > 0)
                zoom -= 2;
        }
        mDist = newDist;
        params.setZoom(zoom);
        cameraTextureView.getmCamera().setParameters(params);
    }

    public void handleFocus(MotionEvent event, Camera.Parameters params) {



        RectLeft = event.getX() - 100;
        RectTop = event.getY() - 100 ;
        RectRight = event.getX() + 100;
        RectBottom = event.getY() + 100;
        DrawFocusRect(RectLeft , RectTop , RectRight , RectBottom , Color.GREEN);



        if(cameraTextureView.getmCamera() != null){
            Camera camera = cameraTextureView.getmCamera();

            camera.cancelAutoFocus();

            Rect focusRect = calculateTapArea(event.getX(), event.getY());


            Camera.Parameters parameters = camera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
            if (parameters.getMaxNumFocusAreas() > 0) {
                List<Camera.Area> mylist = new ArrayList<Camera.Area>();
                mylist.add(new Camera.Area(focusRect, 1000));
                parameters.setFocusAreas(mylist);
            }



            try {
                camera.cancelAutoFocus();
                camera.setParameters(parameters);
                camera.startPreview();
                camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (camera.getParameters().getFocusMode() != Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) {
                            Camera.Parameters parameters = camera.getParameters();
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                            if (parameters.getMaxNumFocusAreas() > 0) {
                                parameters.setFocusAreas(null);

                            }



                            camera.setParameters(parameters);
                            camera.startPreview();
                        }

                        Log.v(TAG, "Should remove canvas from here");
                        canvas = holderTransparent.lockCanvas();
                        canvas.drawColor(0, PorterDuff.Mode.CLEAR);

                        holderTransparent.unlockCanvasAndPost(canvas);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }


        }






    }

    /** Determine the space between the first two fingers */
    private float getFingerSpacing(MotionEvent event) {
        // ...
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);


        return (float)Math.sqrt(x * x + y * y);
    }



    void switchCamera() {
        if (numOfCameras < 1) {
            return;
        }


        cameraTextureView.getmCamera().stopPreview();
        cameraTextureView.getmCamera().release();



        if (isFacingBack) {
            cameraTextureView.setmCamera(getCameraInstance(frontCameraId));
        } else {
            cameraTextureView.setmCamera(getCameraInstance(backCameraId));
        }

        isFacingBack = !isFacingBack;

    }

    private File getOutputMediaFile() throws IOException {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File mediaDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) +
                File.separator + "PhotoBabble" + File.separator);
        mediaDirectory.mkdirs();

        File mediaFile = new File(mediaDirectory, "IMG_"+ timeStamp + ".jpg");
        Log.d("A", "DIRECTORY: " + mediaFile.getPath());



/*      implement this when adding video
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }
*/

        return mediaFile;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // TODO: mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder){

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder){

    }

}
