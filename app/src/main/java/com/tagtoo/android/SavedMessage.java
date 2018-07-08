package com.tagtoo.android;

import java.io.Serializable;

public class SavedMessage implements Serializable {

    public final String name;
    public final String serialNbr;
    public final String dateCreated;
    public final String dateSaved;
    public final int    thumbnailId;
    public final String messageText;
    public final boolean audioFile;
    public final boolean pictureFile;
    public final boolean videoFile;

    public static int KITCHEN_THUMB = 1;
    public static int LIVING_ROOM_THUMB = 2;
    public static int HALLWAY_THUMB = 3;
    public static int BEDROOM_THUMB = 4;
    public static int BATHROOM_THUMB = 5;
    public static int GARAGE_THUMB = 6;
    public static int OTHER_THUMB = 7;


    public SavedMessage(String serialNbr, String name, String dateCreated, int thumbnailId){
        this(serialNbr, name, dateCreated, thumbnailId, null, null, false, false, false);
    }

    public SavedMessage(String serialNbr, String name, String dateCreated, int thumbnailId, String dateSaved, String messageText, boolean audioFile, boolean pictureFile, boolean videoFile){
        this.serialNbr      = serialNbr;
        this.name           = name;
        this.dateCreated    = dateCreated;
        this.dateSaved      = dateSaved;
        this.thumbnailId    = thumbnailId;
        this.messageText    = messageText;
        this.audioFile      = audioFile;
        this.pictureFile    = pictureFile;
        this.videoFile      = videoFile;
    }
}