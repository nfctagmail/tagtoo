package com.tagtoo.android;

// Objet contenant toutes les informations pour un objet de la liste de l'onglet accueil
public class SavedMessage {

    // Initialisation des variables
    public final String content;    // Contenu (=message texte) du tag
    public final String serialNbr;  // Numéro de série
    public final String dateSaved;  // La date où il a été scanné
    public final String fileName;   // Le nom du fichier du message audio sur l'appareil

    // Constructeur du fichier dans le cas où il y a seulement un message texte
    public SavedMessage(String content, String serialNbr, String dateSaved){
        this.content = content;
        this.serialNbr = serialNbr;
        this.dateSaved = dateSaved;
        this.fileName = null;
    }

    // Constructeur du fichier dans le cas où il y a un message texte et un message audio
    public SavedMessage(String content, String serialNbr, String dateSaved, String fileName){
        this.content = content;
        this.serialNbr = serialNbr;
        this.dateSaved = dateSaved;
        this.fileName = fileName;
    }
}