package com.develop.measurear;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.develop.augmentedimage.sceneform.Description;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.develop.augmentedimage.sceneform.AugmentedImageNode;
import com.develop.common.helpers.CameraPermissionHelper;
import com.develop.common.helpers.FullScreenHelper;
import com.develop.common.helpers.SandbarHelper;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.DpToMetersViewSizer;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private ArSceneView arSceneView;
    private boolean installRequested = false;
    private Session session;
    private final SandbarHelper messageSandbarHelper = new SandbarHelper();
    private boolean shouldConfigureSession = false;
    private List<Description> listElements;
    private Node tempLine= null;
    private static ModelRenderable model;
    private AugmentedImageNode imgNode1=null, imgNode2=null;
    private Vector3 mov2imgNode;
    private boolean autoFocus,quaternionAngles,externalImage;
    private float unitsPref;
    private int labelsSize;
    private String unitString,pathImageOne,pathImageTwo;
    private Vector3 colorLine,colorOrigin, colorElement,colorMark,scaleMarker,moveCenter;
    private Float widthImage;
    private final static String LABEL_STRING ="label";
    private final static String TEMP_LINE_STRING ="tempLine";
    private final static String LINE_STRING ="line";
    private final static String DISTANCE_STRING ="Distance: ";
    private final static String POINT_STRING ="Point: ";
    private final static String MARKER_ONE_STRING ="marker_one";
    private final static String MARKER_TWO_STRING ="marker_two";
    private final static String DATA_IMAGE_STRING ="myimages.imgdb";
    private Float alphaColor;
    private Float sizeLine;
    private Float sizeSphere;





    public MainActivity() {
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arSceneView = findViewById(R.id.surfaceview);


        arSceneView.getPlaneRenderer().setEnabled(false);
        
        initializeSceneView();


        listElements = new ArrayList<>();



        FloatingActionButton deleteButton = findViewById(R.id.delete_buttom);
        deleteButton.setOnClickListener(this::onDeleteElement);


        FloatingActionButton addButton = findViewById(R.id.add_buttom);
        addButton.setOnClickListener(this::addPoint);


        FloatingActionButton settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(this::onSettings);




        setupSharedPreferences();

        //Its created the sphere to indicates the element added.a
        MaterialFactory.makeTransparentWithColor(this, new Color(this.colorMark.x,this.colorMark.y,this.colorMark.z,this.alphaColor))
                .thenAccept(
                        material -> model = ShapeFactory.makeSphere(
                                this.sizeSphere,
                                Vector3.zero(), material)
                );





    }





    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }



                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                //It is necessary the storage read permission to read external image from gallery.
                boolean hasPermission = (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                if (!hasPermission) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            100);
                }


                session = new Session(/* context = */ this);





            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (Exception e) {
                message = "This device does not support AR";
                exception = e;
            }

            if (message != null) {
                messageSandbarHelper.showError(this, message);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }

            shouldConfigureSession = true;
        }

        if (shouldConfigureSession) {
            configureSession();
            shouldConfigureSession = false;



            arSceneView.setupSession(session);
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
            arSceneView.resume();
        } catch (CameraNotAvailableException e) {
            // In some cases (such as another camera app launching) the camera may be given to
            // a different app instead. Handle this properly by showing a message and recreate the
            // session at the next iteration.
            messageSandbarHelper.showError(this, "Camera not available. Please restart the app.");
            session = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            arSceneView.pause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        if (CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(
                    this, "Camera permissions are needed to run this application", Toast.LENGTH_LONG)
                    .show();
            // Permission denied with checking "Do not ask again".
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this))
                CameraPermissionHelper.launchPermissionSettings(this);
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    private void initializeSceneView() {
        arSceneView.getScene().addOnUpdateListener(this::onUpdateFrame);


    }


    /**
     *
     * An element is added to a list. If there are two o more points, the vector direction and the distance are calculated,  and a line is drawn.
     * The vector direction allows to obtain the angles.
     * Moreover, an label it is added to shows the distance.
     * The world coordinate system has as origin the center of a marker.
     */
    private void addPoint(View view) {

        if( null == imgNode1 || null == imgNode2)
        {

            Toast.makeText(MainActivity.this, "Scan the references", Toast.LENGTH_SHORT).show();
            return;

        }

        Toast.makeText(MainActivity.this, "Added point", Toast.LENGTH_SHORT).show();



        Node n1 = new Node();
        n1.setRenderable(model);


        n1.setWorldPosition(new Vector3(0,0,0));
        arSceneView.getScene().addChild(n1);

        Description p = new Description();
        p.setNode(n1);
        //The position of the new point is the difference between point reference and marker position. This helps to update the position whether the origin marker is changed
        p.setPos(Vector3.subtract(mov2imgNode,imgNode1.getWorldPosition()));
        listElements.add(p);
        if(listElements.size()>1) {
            Description last = listElements.get(listElements.size()-1);
            Vector3 ori = imgNode1.getWorldPosition();
            Description penultimate = listElements.get(listElements.size()-2);
            last.setBefore_pos(penultimate.getPos());
            last.setDistance(distanceTwoPoints(Vector3.add(ori, last.getPos()), Vector3.add(ori, penultimate.getPos())));
            last.setDir(directionTwoPoints(Vector3.add(ori, penultimate.getPos()), Vector3.add(ori, last.getPos())));
            drawLine(Vector3.add(ori, penultimate.getPos()), Vector3.add(ori, last.getPos()), last.getNode());
            ViewRenderable.builder().setSizer(new DpToMetersViewSizer(this.labelsSize))
                    .setView(arSceneView.getContext(), R.layout.controls)
                    .build()
                    .thenAccept(viewRenderable -> addNodeToScene( last.getNode(),viewRenderable,last.getDistance()))
                    .exceptionally(throwable -> {
                                Toast.makeText(arSceneView.getContext(), "Error:" + throwable.getMessage(), Toast.LENGTH_LONG).show();
                                return null;
                            }

                    );
        }
    }

    @SuppressLint("SetTextI18n")
    private void onUpdateFrame(FrameTime frameTime) {
        Frame frame = arSceneView.getArFrame();
        Collection<AugmentedImage> updatedAugmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);




//This while allows to update the elements as the line and labels position and orientation.
        ListIterator<Description> i = listElements.listIterator();
        while (i.hasNext())
        {
            Description next = i.next();
            Description n= updatePosition(next);
            i.set(n);

        }



 // This IF allows to update a temporal line that indicates the distance between either the origin (the marker) or with the new point.
 //The label of this line shows the current distance with different background color.
if(tempLine!=null) {

    for (Node j : tempLine.getChildren()) {


        if (j.getName().equals(LABEL_STRING)) {


            updateLabels(arSceneView, j);


//After third points, the angle is shown


           if(listElements.size() > 1) {
              Description last = listElements.get(listElements.size() - 1);
              Vector3 ori = imgNode1.getWorldPosition();
              Vector3 dst = Vector3.add(ori, last.getPos());


              Vector3 tempDir = directionTwoPoints(dst, mov2imgNode);
              float angle = Math.abs(slope(ori,dst)+Vector3.angleBetweenVectors(tempDir, last.getDir()));

              float localD = distanceTwoPoints(dst, mov2imgNode);

              ViewRenderable r = (ViewRenderable) j.getRenderable();
              View v = r.getView();
              TextView tva = v.findViewById(R.id.textView2);
              tva.setBackgroundColor(0xff00ff00);
               LinearLayout ll = v.findViewById(R.id.layout2);
               ll.setAlpha(this.alphaColor);

               if(this.quaternionAngles)
               {
                   Quaternion q1 = Quaternion.lookRotation(tempDir, last.getDir());

                   double sqw = q1.w*q1.w;
                   double sqx = q1.x*q1.x;
                   double sqy = q1.y*q1.y;
                   double sqz = q1.z*q1.z;
                   double aX = Math.atan2(2.0 * (q1.x*q1.y + q1.z*q1.w),(sqx - sqy - sqz + sqw));
                   double aZ = Math.atan2(2.0 * (q1.y*q1.z + q1.x*q1.w),(-sqx - sqy + sqz + sqw));
                   double aY = Math.asin(-2.0 * (q1.x*q1.z - q1.y*q1.w));



                   tva.setText(DISTANCE_STRING + roundNumber(localD) + this.unitString + "\n ⊾: " + roundNumber(angle)+"\n quaternion Ø: "+roundNumber(Math.toDegrees(aX))+"\n quaternion θ: "+roundNumber(Math.toDegrees(aY))+"\n quaternion Ψ: "+roundNumber(Math.toDegrees(aZ)));

               }
               else {
                   tva.setText(DISTANCE_STRING + roundNumber(localD) + this.unitString + "\n ⊾: " + roundNumber(angle));
               }


          }
            else if(listElements.size() ==1)
            {
                Description last = listElements.get(listElements.size() - 1);
                Vector3 ori = imgNode1.getWorldPosition();
                Vector3 dst = Vector3.add(ori, last.getPos());

                float localD = distanceTwoPoints(dst, mov2imgNode);

                ViewRenderable r = (ViewRenderable) j.getRenderable();
                View v = r.getView();
                TextView tva = v.findViewById(R.id.textView2);
                tva.setText(DISTANCE_STRING + (localD) + this.unitString);
                tva.setBackgroundColor(0xff00ff00);
                LinearLayout ll = v.findViewById(R.id.layout2);
                ll.setAlpha(this.alphaColor);


            }
          else {
                Vector3 dst = imgNode1.getWorldPosition();
                float localD = distanceTwoPoints(dst, mov2imgNode);


                ViewRenderable r = (ViewRenderable) j.getRenderable();
                View v = r.getView();
                TextView tva = v.findViewById(R.id.textView2);
                tva.setText(DISTANCE_STRING + roundNumber(localD) + this.unitString);
                tva.setBackgroundColor(0xff00ff00);
               LinearLayout ll = v.findViewById(R.id.layout2);
               ll.setAlpha(this.alphaColor);

            }
            j.setWorldPosition(imgNode2.getWorldPosition());


        }

    }

}

        for (AugmentedImage augmentedImage : updatedAugmentedImages) {
            if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
                // Check camera image matches our reference image

                if (augmentedImage.getName().equals(MARKER_ONE_STRING)) {

                    //The marker as origin is recognized.
                    imgNode1 = new AugmentedImageNode(this,new Vector3(0,0,0.0f),this.colorOrigin, this.scaleMarker);
                    imgNode1.setImage(augmentedImage);
                    arSceneView.getScene().addChild(imgNode1);


                }

                else if (augmentedImage.getName().equals(MARKER_TWO_STRING)) {
                    //The marker to add new element is recognized. The reference can be changed.

                    imgNode2 = new AugmentedImageNode(this, this.moveCenter,this.colorElement,this.scaleMarker);
                    imgNode2.setImage(augmentedImage);
                    mov2imgNode = imgNode2.getNode().getWorldPosition();
                    arSceneView.getScene().addChild(imgNode2);
                }
/*
* This section draws temporal line to helps to measure the distance between either the origin or new point.
*
*
* */
            //If the line doesn't exist, the line is created.

              if(imgNode1!=null && imgNode2!=null)
              {
                      if(null == tempLine) {

                          tempLine = new Node();
                          //First, find the vector extending between the two points and define a look rotation
                          //in terms of this Vector.
                          final Vector3 difference = Vector3.subtract(imgNode1.getWorldPosition(), mov2imgNode);
                          final Vector3 directionFromTopToBottom = difference.normalized();
                          final Quaternion rotationFromAToB =
                                  Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());
                          MaterialFactory.makeTransparentWithColor(getApplicationContext(), new Color(this.colorLine.x, this.colorLine.y, this.colorLine.z,this.alphaColor))
                                  .thenAccept(
                                          material -> {
                                /* Then, create a rectangular prism, using ShapeFactory.makeCube() and use the difference vector
                                       to extend to the necessary length.  */
                                              ModelRenderable model = ShapeFactory.makeCube(
                                                      new Vector3(this.sizeLine, this.sizeLine, difference.length()),
                                                      Vector3.zero(), material);
                                /* Last, set the world rotation of the node to the rotation calculated earlier and set the world position to
                                       the midpoint between the given points . */

                                              tempLine.setParent(arSceneView.getScene());
                                              tempLine.setName(TEMP_LINE_STRING);

                                              tempLine.setRenderable(model);
                                              tempLine.setWorldPosition(Vector3.add(imgNode1.getWorldPosition(), mov2imgNode).scaled(.5f));
                                              tempLine.setWorldRotation(rotationFromAToB);
                                          }
                                  );

                          float localD = distanceTwoPoints(imgNode1.getWorldPosition(), mov2imgNode);

                          ViewRenderable.builder().setSizer(new DpToMetersViewSizer(this.labelsSize))
                                  .setView(arSceneView.getContext(), R.layout.controls)
                                  .build()
                                  .thenAccept(viewRenderable -> addNodeToScene( tempLine,viewRenderable,localD))
                                  .exceptionally(throwable -> {
                                              Toast.makeText(arSceneView.getContext(), "Error:" + throwable.getMessage(), Toast.LENGTH_LONG).show();
                                              return null;
                                          }

                                  );

                      }

                        else
                        {

  //If the line exists, the line position and label are updated.

                              Vector3 dst;
                              if(listElements.size()>0) {
                                  Description last = listElements.get(listElements.size()-1);
                                  Vector3 ori = imgNode1.getWorldPosition();
                                  dst= Vector3.add(ori,last.getPos());
                              }
                              else
                              {
                                  dst=imgNode1.getWorldPosition();
                              }




                              final Vector3 difference = Vector3.subtract(dst, mov2imgNode);
                              final Vector3 directionFromTopToBottom = difference.normalized();
                              final Quaternion rotationFromAToB =
                                      Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());

                              MaterialFactory.makeTransparentWithColor(getApplicationContext(), new Color(this.colorLine.x, this.colorLine.y, this.colorLine.z,this.alphaColor))
                                      .thenAccept(
                                              material -> {
                                    /* Then, create a rectangular prism, using ShapeFactory.makeCube() and use the difference vector
                                           to extend to the necessary length.  */
                                                  ModelRenderable model = ShapeFactory.makeCube(
                                                          new Vector3(this.sizeLine, this.sizeLine, difference.length()),
                                                          Vector3.zero(), material);
                                    /* Last, set the world rotation of the node to the rotation calculated earlier and set the world position to
                                           the midpoint between the given points . */



                                                  tempLine.setRenderable(model);
                                                  tempLine.setWorldPosition(Vector3.add(dst, mov2imgNode).scaled(.5f));
                                                  tempLine.setWorldRotation(rotationFromAToB);
                                              }
                                      );

                          }





              }






            }
        }
    }



    public static Vector3 directionTwoPoints(Vector3 u,Vector3 v)
    {

        return Vector3.subtract(v, u);
    }

    public float slope(Vector3 p1, Vector3 p2) {

        float x1 = p1.x;
        float x2 = p2.x;
        float y1 = p1.y;
        float y2 = p2.y;
        float z1 = p1.z;
        float z2 = p2.z;


        double a = (z2 - z1) / Math.sqrt((Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2)));

        if (a > 0)
            return 0.f;
        else if (a < 0)
            return -180.f;


        return 0;
    }



    public float distanceTwoPoints(Vector3 startPose, Vector3 endPose)
    {



        float dx = startPose.x - endPose.x;
        float dy = startPose.y - endPose.y;
        float dz = startPose.z - endPose.z;

        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz) * this.unitsPref;


    }

//This function helps to move the labels with respect to camera view.
    private void updateLabels(ArSceneView sceneView,Node node)
    {

        Vector3 camera = sceneView.getScene().getCamera().getWorldPosition();
        Vector3 uiPosition  = node.getWorldPosition();
        Vector3 direction = Vector3.subtract(camera, uiPosition);
        Quaternion lookRotation = Quaternion.lookRotation(direction, Vector3.up());
        node.setLocalPosition(new Vector3(0,0.02f,0));
        node.setWorldRotation(lookRotation);
    }



    private Description updatePosition(Description p)
    {



        p.getNode().setWorldPosition(Vector3.add(imgNode1.getWorldPosition(),p.getPos()));

        for(Node i:p.getNode().getChildren())
        {
            if(i.getName().equals(LABEL_STRING))
            {
                updateLabels(arSceneView,i);

            }

            else  if(i.getName().equals(LINE_STRING))
            {


                    Vector3 dst1 = Vector3.add(imgNode1.getWorldPosition(),p.getPos());
                    Vector3 dst2= Vector3.add(imgNode1.getWorldPosition(),p.getBefore_pos());




                    final Vector3 difference = Vector3.subtract(dst1, dst2);
                    final Vector3 directionFromTopToBottom = difference.normalized();
                    final Quaternion rotationFromAToB =
                            Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());

                    MaterialFactory.makeTransparentWithColor(getApplicationContext(), new Color(this.colorLine.x, this.colorLine.y, this.colorLine.z))
                            .thenAccept(
                                    material -> {

                                        ModelRenderable model = ShapeFactory.makeCube(
                                                new Vector3(this.sizeLine, this.sizeLine, difference.length()),
                                                Vector3.zero(), material);

                                        i.setRenderable(model);
                                        i.setWorldPosition(Vector3.add(dst1, dst2).scaled(.5f));
                                        i.setWorldRotation(rotationFromAToB);
                                    }
                            );

            }

        }

        return p;
    }

    @SuppressLint("SetTextI18n")
    private void addNodeToScene( Node n, Renderable renderable, float localD) {
        Node node = new Node();
        node.setRenderable(renderable);
        node.setParent(n);
        node.setName(LABEL_STRING);
        ViewRenderable r = (ViewRenderable)node.getRenderable();
        View v = r.getView();
        TextView tva = v.findViewById(R.id.textView2);
        tva.setText(POINT_STRING+(listElements.size())+"\n"+DISTANCE_STRING + roundNumber(localD) + this.unitString);
        LinearLayout ll = v.findViewById(R.id.layout2);
        ll.setAlpha(this.alphaColor);
        //calculate the angle when there are more than 3 points
        if(listElements.size()>2)
        {
            Description last = listElements.get(listElements.size()-1);

            Description penultimate = listElements.get(listElements.size()-2);


            float angle=Vector3.angleBetweenVectors(last.getDir(),penultimate.getDir());

            penultimate.setAngle(Math.abs(slope(last.getDir(),penultimate.getDir())+angle));
            ViewRenderable r_before = (ViewRenderable) penultimate.getNode().findByName(LABEL_STRING).getRenderable();
            View v_before = r_before.getView();
            TextView tv = v_before.findViewById(R.id.textView2);



            if(this.quaternionAngles)
            {
                Quaternion q1 = Quaternion.lookRotation(penultimate.getDir(), last.getDir());

                double sqw = q1.w*q1.w;
                double sqx = q1.x*q1.x;
                double sqy = q1.y*q1.y;
                double sqz = q1.z*q1.z;
                double aX = Math.atan2(2.0 * (q1.x*q1.y + q1.z*q1.w),(sqx - sqy - sqz + sqw));
                double aZ = Math.atan2(2.0 * (q1.y*q1.z + q1.x*q1.w),(-sqx - sqy + sqz + sqw));
                double aY = Math.asin(-2.0 * (q1.x*q1.z - q1.y*q1.w));



                tv.setText(POINT_STRING + (listElements.size()-1) + "\n"+DISTANCE_STRING + roundNumber(penultimate.getDistance()) + this.unitString + "\n ⊾: " + roundNumber(penultimate.getAngle())+"\n quaternion Ø: "+roundNumber(Math.toDegrees(aX))+"\n quaternion θ: "+roundNumber(Math.toDegrees(aY))+"\n quaternion Ψ: "+roundNumber(Math.toDegrees(aZ)));

            }
            else {
                tv.setText(POINT_STRING + (listElements.size()-1) + "\n"+DISTANCE_STRING + roundNumber(penultimate.getDistance()) + this.unitString + "\n ⊾: " + roundNumber(penultimate.getAngle()));
            }

        }



    }
/*
    float[] quaternion2euler(Quaternion q)
    {

        float x = q.x;
        float y = q.y;
        float z = q.z;
        float w = q.w;

        float t0= 2.0f* (w*x+y*x);
        float t1 = 1.0f - 2.0f * (x * x + y * y);
        float roll = (float) Math.atan2(t0, t1);
        float t2 = 2.0f * (w * y - z * x);

        if (t2 > 1.0f)
        {
            t2=1.0f;

        }
        else if (t2 < -1.0f )
        {
            t2= -1.0f;

        }
         float   pitch = (float)Math.asin(t2);
        float t3 = 2.0f * (w * z + x * y);
        float t4 = 1.0f - 2.0f * (y * y + z * z);
        double yaw = Math.atan2(t3, t4);

        float[] out=new float[3];

        out[0]= (float) Math.toDegrees(yaw);
        out[1]=  (float) Math.toDegrees(pitch);
        out[2]=  (float) Math.toDegrees(roll);

        return out;


    }
*/



    private void configureSession() {
        Config config = new Config(session);


        if (!setupAugmentedImageDb(config)) {
            messageSandbarHelper.showError(this, "Could not setup augmented image database");
        }

        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        if ( this.autoFocus) {
            config.setFocusMode(Config.FocusMode.AUTO);
        } else {
            config.setFocusMode(Config.FocusMode.FIXED);
        }


        session.configure(config);
    }
/*
* An Imagedatabase was created with arcore tool. If the markers are changed, the new markers are loaded.
* https://github.com/google-ar/arcore-android-sdk/tree/master/tools/arcoreimg
* */
    private boolean setupAugmentedImageDb(Config config) {

        AugmentedImageDatabase augmentedImageDatabase = null;



        if(this.externalImage) {


            Bitmap augmentedImageBitmap = loadAugmentedImage(this.pathImageOne);
            if (augmentedImageBitmap == null) {
                return false;
            }

            Bitmap augmentedImageBitmap2 = loadAugmentedImage(this.pathImageTwo);
            if (augmentedImageBitmap2 == null) {
                return false;
            }


            augmentedImageDatabase = new AugmentedImageDatabase(session);
            augmentedImageDatabase.addImage(MARKER_ONE_STRING, augmentedImageBitmap, this.widthImage);
            augmentedImageDatabase.addImage(MARKER_TWO_STRING, augmentedImageBitmap2, this.widthImage);
        }
        else
        {
            try (InputStream is = getAssets().open(DATA_IMAGE_STRING)) {
                augmentedImageDatabase = AugmentedImageDatabase.deserialize(session, is);
            } catch (IOException e) {
                Log.e(TAG, "IO exception loading augmented image database.", e);
            }
        }

        config.setAugmentedImageDatabase(augmentedImageDatabase);



        return true;
    }



    private Bitmap loadAugmentedImage(String file) {


        File imgFile = new File(file);
        if(imgFile.exists()){

            return BitmapFactory.decodeFile(imgFile.getAbsolutePath());

        }
        return null;
    }

    private void drawLine(Vector3 point1, Vector3 point2, Node n) {
        //Draw a line between two AnchorNodes (adapted from https://stackoverflow.com/a/52816504/334402)

        Node nodeForLine= new Node();
        nodeForLine.setName(LINE_STRING);
        //First, find the vector extending between the two points and define a look rotation
        //in terms of this Vector.
        final Vector3 difference = Vector3.subtract(point1, point2);
        final Vector3 directionFromTopToBottom = difference.normalized();
        final Quaternion rotationFromAToB =
                Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());
        MaterialFactory.makeTransparentWithColor(getApplicationContext(), new Color(this.colorLine.x, this.colorLine.y, this.colorLine.z,this.alphaColor))
                .thenAccept(
                        material -> {
                            /* Then, create a rectangular prism, using ShapeFactory.makeCube() and use the difference vector
                                   to extend to the necessary length.  */
                            ModelRenderable model = ShapeFactory.makeCube(
                                    new Vector3(this.sizeLine, this.sizeLine, difference.length()),
                                    Vector3.zero(), material);
                            /* Last, set the world rotation of the node to the rotation calculated earlier and set the world position to
                                   the midpoint between the given points . */

                            nodeForLine.setParent(n);


                            nodeForLine.setRenderable(model);
                            nodeForLine.setWorldPosition(Vector3.add(point1, point2).scaled(.5f));
                            nodeForLine.setWorldRotation(rotationFromAToB);
                        }
                );

    }


    private void onDeleteElement(View view) {
        //Delete the Anchor if it exists


        Node exist = arSceneView.getScene().findByName(TEMP_LINE_STRING);
        if(exist !=null)

        {
            arSceneView.getScene().removeChild(exist);

            tempLine = null;

        }

        if(listElements.size()<1) {
            Toast.makeText(MainActivity.this, "List empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Description r = listElements.get(listElements.size() - 1);
        if (r != null) {
            arSceneView.getScene().removeChild(r.getNode());
            r.getNode().setParent(null);
            listElements.remove(listElements.size() - 1);

        }








    }

    private double roundNumber(double x) {
        return Math.round(x * 100.0) / 100.0;
    }


    private void onSettings(View view) {

            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);

    }


    /*The saved preferences are gotten. Defaults values are initialized
*/
    private void setupSharedPreferences() {

        this.colorLine = new Vector3(255,255,0);
        this.colorOrigin= new Vector3(255,255,255);
        this.colorElement= new Vector3(255,255,255);
        this.colorMark= new Vector3(0,0,255);
        this.labelsSize = 5120; //5cm in dp
        this.unitsPref = 1000.f;
        this.autoFocus = true;
        this.quaternionAngles = false;
        this.unitString=getString(R.string.pref_units_mm_value);
        this.externalImage = false;
        this.widthImage = 50.f;
        this.scaleMarker = new Vector3(0.01f, 0.01f, 0.01f);
        this.moveCenter = new Vector3(0,0.0f,0.05f);
        this.alphaColor = 1.0f;
        this.sizeLine = .001f;
        this.sizeSphere = 0.001f;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        this.autoFocus = prefs.getBoolean(getString(R.string.pref_focus_key),true);
        this.quaternionAngles = prefs.getBoolean(getString(R.string.pref_quaternion_key),false);

        String valueTemp= prefs.getString(getString(R.string.pref_measure_units_key),getString(R.string.pref_units_mm_value));
        this.unitString=valueTemp;

        if(valueTemp.equals(getString(R.string.pref_units_mm_value)))
        {
            this.unitsPref = 1000.f;

        }
        else if(valueTemp.equals(getString(R.string.pref_units_cm_value)))
        {
            this.unitsPref = 100.f;

        }
        else if(valueTemp.equals(getString(R.string.pref_units_meter_value)))
        {
            this.unitsPref = 1.f;

        }



        valueTemp= prefs.getString(getString(R.string.pref_color_line_key),getString(R.string.pref_color_yellow_value));
        if(valueTemp.equals(getString(R.string.pref_color_red_value)))
        {
            this.colorLine = new Vector3(255, 0, 0);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_blue_value)))
        {
            this.colorLine = new Vector3(0, 0, 255);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_green_value)))
        {
            this.colorLine = new Vector3(0, 255, 0);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_yellow_value)))
        {
            this.colorLine = new Vector3(255, 255, 0);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_white_value)))
        {
            this.colorLine = new Vector3(255, 255, 255);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_black_value)))
        {
            this.colorLine = new Vector3(0, 0, 0);

        }

        valueTemp= prefs.getString(getString(R.string.pref_color_origin_key),getString(R.string.pref_color_white_value));
        if(valueTemp.equals(getString(R.string.pref_color_red_value)))
        {
            this.colorOrigin = new Vector3(255, 0, 0);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_blue_value)))
        {
            this.colorOrigin = new Vector3(0, 0, 255);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_green_value)))
        {
            this.colorOrigin = new Vector3(0, 255, 0);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_yellow_value)))
        {
            this.colorOrigin = new Vector3(255, 255, 0);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_white_value)))
        {
            this.colorOrigin = new Vector3(255, 255, 255);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_black_value)))
        {
            this.colorOrigin = new Vector3(0, 0, 0);

        }

        valueTemp= prefs.getString(getString(R.string.pref_color_element_key),getString(R.string.pref_color_white_value));
        if(valueTemp.equals(getString(R.string.pref_color_red_value)))
        {
            this.colorElement = new Vector3(255, 0, 0);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_blue_value)))
        {
            this.colorElement = new Vector3(0, 0, 255);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_green_value)))
        {
            this.colorElement = new Vector3(0, 255, 0);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_yellow_value)))
        {
            this.colorElement = new Vector3(255, 255, 0);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_white_value)))
        {
            this.colorElement = new Vector3(255, 255, 255);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_black_value)))
        {
            this.colorElement = new Vector3(0, 0, 0);

        }


        valueTemp= prefs.getString(getString(R.string.pref_color_mark_sphere_key),getString(R.string.pref_color_blue_value));

        if(valueTemp.equals(getString(R.string.pref_color_red_value)))
        {
            this.colorMark = new Vector3(255, 0, 0);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_blue_value)))
        {
            this.colorMark = new Vector3(0, 0, 255);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_green_value)))
        {
            this.colorMark = new Vector3(0, 255, 0);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_yellow_value)))
        {
            this.colorMark = new Vector3(255, 255, 0);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_white_value)))
        {
            this.colorMark = new Vector3(255, 255, 255);

        }
        else if(valueTemp.equals(getString(R.string.pref_color_black_value)))
        {
            this.colorMark = new Vector3(0, 0, 0);

        }



        valueTemp= prefs.getString(getString(R.string.pref_labels_size_key),getString(R.string.pref_size_label_five_value));
        if(valueTemp.equals(getString(R.string.pref_size_label_two_value)))
        {
            this.labelsSize = 10240; //2.5cm

        }
        else if(valueTemp.equals(getString(R.string.pref_size_label_five_value)))
        {
            this.labelsSize = 5120; //5 cm

        }
        else if(valueTemp.equals(getString(R.string.pref_size_label_ten_value)))
        {
            this.labelsSize = 2560; //10cm

        }

        valueTemp= prefs.getString(getString(R.string.pref_ref_marker_size_key),getString(R.string.pref_ref_marker_one_value));

        if(valueTemp.equals(getString(R.string.pref_ref_marker_one_value)))
        {
            this.scaleMarker = new Vector3(.01f, .01f, 0.01f);

        }
        else if(valueTemp.equals(getString(R.string.pref_ref_marker_half_value)))
        {
            this.scaleMarker = new Vector3(.02f, .02f, 0.02f);

        }
        else if(valueTemp.equals(getString(R.string.pref_ref_marker_normal_value)))
        {
            this.scaleMarker = new Vector3(.05f, .05f, 0.05f);

        }



        valueTemp= prefs.getString(getString(R.string.pref_move_center_key),getString(R.string.pref_move_center_half_value));

        if(valueTemp.equals(getString(R.string.pref_move_center_zero_value)))
        {
            this.moveCenter = new Vector3(0.0f, 0.0f, 0.0f);

        }
        else if(valueTemp.equals(getString(R.string.pref_move_center_half_value)))
        {
            this.moveCenter = new Vector3(0.0f, 0.0f, 0.05f);

        }
        else if(valueTemp.equals(getString(R.string.pref_move_center_one_value)))
        {
            this.moveCenter = new Vector3(0.0f, 0.0f, 0.1f);

        }


        valueTemp= prefs.getString(getString(R.string.pref_sphere_size_key),getString(R.string.pref_ref_marker_one_value));

        if(valueTemp.equals(getString(R.string.pref_ref_marker_one_value)))
        {
        this.sizeSphere = 0.001f;
        }
        else if(valueTemp.equals(getString(R.string.pref_ref_marker_half_value)))
        {
            this.sizeSphere = 0.005f;

        }
        else if(valueTemp.equals(getString(R.string.pref_ref_marker_normal_value)))
        {
            this.sizeSphere = 0.01f;

        }


        valueTemp= prefs.getString(getString(R.string.pref_line_size_key),getString(R.string.pref_ref_marker_one_value));

        if(valueTemp.equals(getString(R.string.pref_ref_marker_one_value)))
        {
            this.sizeLine = 0.001f;
        }
        else if(valueTemp.equals(getString(R.string.pref_ref_marker_half_value)))
        {
            this.sizeLine = 0.005f;

        }
        else if(valueTemp.equals(getString(R.string.pref_ref_marker_normal_value)))
        {
            this.sizeLine = 0.01f;

        }


        this.externalImage = prefs.getBoolean(getString(R.string.pref_external_image_key),false);

        this.pathImageOne = prefs.getString(getString(R.string.pref_external_path_one_key),"");

        this.pathImageTwo = prefs.getString(getString(R.string.pref_external_path_two_key),"");

        this.widthImage = Float.parseFloat(prefs.getString(getString(R.string.pref_size_key),getString(R.string.pref_size_default))) / 1000.f;

        this.alphaColor = Float.parseFloat(prefs.getString(getString(R.string.pref_alpha_key),getString(R.string.pref_alpha_default))) / 100.f;

    }

}
