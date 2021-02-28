package com.develop.augmentedimage.sceneform;


import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;

public class Description {

    private Node node;
    private Vector3 pos;
    private  Vector3 dir;
    private Vector3 before_pos;
    private float angle;
    private float distance;




    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Vector3 getPos() {
        return pos;
    }

    public void setPos(Vector3 pos) {
        this.pos = pos;
    }

    public Vector3 getDir() {
        return dir;
    }

    public void setDir(Vector3 dir) {
        this.dir = dir;
    }



    public Vector3 getBefore_pos() {
        return before_pos;
    }

    public void setBefore_pos(Vector3 before_pos) {
        this.before_pos = before_pos;
    }


    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }


}
