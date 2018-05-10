package com.tagtoo.android;

public class SavedMessage {
    public final String content;
    public final String serialNbr;
    public final String dateSaved;
    public final String fileName;


    public SavedMessage(String content, String serialNbr, String dateSaved){
        this.content = content;
        this.serialNbr = serialNbr;
        this.dateSaved = dateSaved;
        this.fileName = null;
    }

    public SavedMessage(String content, String serialNbr, String dateSaved, String fileName){
        this.content = content;
        this.serialNbr = serialNbr;
        this.dateSaved = dateSaved;
        this.fileName = fileName;
    }
}