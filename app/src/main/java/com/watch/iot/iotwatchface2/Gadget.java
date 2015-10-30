package com.watch.iot.iotwatchface2;

/**
 * Created by who on 2015-10-29.
 * type=1 för lampa med på/av
 * id ska vara unikt för gadget
 * xPosInRoom och yPosInRoom är angivet i meter från origo,
 */
public class Gadget {
    public int type,id;
    private double xPosInRoom,yPosInRoom;
    public double angle;

    public Gadget(int type, int id, double xPosInRoom, double yPosInRoom) {
        this.type=type;
        this.id=id;
        this.xPosInRoom=xPosInRoom;
        this.yPosInRoom=yPosInRoom;
        this.angle=Math.atan(yPosInRoom/xPosInRoom);
    }


}
