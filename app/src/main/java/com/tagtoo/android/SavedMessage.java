package com.tagtoo.android;

public class SavedMessage {

    public final String name;
    public final String serialNbr;
    public final String dateSaved;
    public final String messageText;
    public final String audioFile;
    public final String pictureFile;
    public final String videoFile;

    public SavedMessage(String serialNbr, String name, String dateSaved){
        this.serialNbr      = serialNbr;
        this.name           = name;
        this.dateSaved      = dateSaved;
        this.messageText    = null;
        this.audioFile      = null;
        this.pictureFile    = null;
        this.videoFile      = null;
    }

    public SavedMessage(String serialNbr, String name, String dateSaved, String messageText, String audioFile, String pictureFile, String videoFile){
        this.serialNbr      = serialNbr;
        this.name           = name;
        this.dateSaved      = dateSaved;
        this.messageText    = messageText;
        this.audioFile      = audioFile;
        this.pictureFile    = pictureFile;
        this.videoFile      = videoFile;
    }
}