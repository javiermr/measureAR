
package com.develop.augmentedimage.sceneform;

import android.content.Context;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;

public class AugmentedImageNode extends AnchorNode {

    private static ModelRenderable model;
    private final Vector3 moveCenter;
    private Node node;


    public AugmentedImageNode(Context context, Vector3 moveCenter, Vector3 color,Vector3 scale) {
        this.moveCenter = moveCenter;

        // Upon construction, start loading the modelFuture
        if (model == null) {


            MaterialFactory.makeOpaqueWithColor(context, new Color(color.x, color.y, color.z))
                    .thenAccept(
                            material -> {
                            /* Then, create a rectangular prism, using ShapeFactory.makeCube() and use the difference vector
                                   to extend to the necessary length.  */
                                model = ShapeFactory.makeCube(
                                        scale,
                                        Vector3.zero(), material);

                            }
                    );


        }
    }

    /**
     * Called when the AugmentedImage is detected and should be rendered. A Sceneform node tree is
     * created based on an Anchor created from the image.
     *
     * @param image captured by your camera
     */
    public void setImage(AugmentedImage image) {


        setAnchor(image.createAnchor(image.getCenterPose()));


        Pose pose = Pose.makeTranslation(moveCenter.x,moveCenter.y,moveCenter.z);

        node = new Node();
        node.setParent(this);

        node.setLocalPosition(new Vector3(pose.tx(), pose.ty(), pose.tz()));
        node.setLocalRotation(new Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw()));
        node.setRenderable(model);


    }



    public Node getNode() {
        return node;
    }


}
