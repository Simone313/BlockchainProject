/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.blockchain.blockchain;

/**
 *
 * @author simo0
 */
public class peerThread extends Thread {
    String name;
    boolean couchDB;
    public peerThread(String name, boolean couchDB){
        this.name=name;
        this.couchDB=couchDB;
        this.start();
    }
    
    public void run(){
        Blockchain.executeWSLCommand("cd Prova && docker compose down");

        // Avvia solo CouchDB
        Blockchain.executeWSLCommand("cd Prova && docker compose up -d couchdb");

        if (couchDB) {
            Blockchain.waitForContainer("couchdb"); // deve controllare lo stato health
            // Ora avvia il peer
            Blockchain.executeWSLCommand("cd Prova && docker compose up -d ");
        }
    }
    
    

}
