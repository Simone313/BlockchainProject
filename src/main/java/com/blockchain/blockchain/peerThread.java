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
    public peerThread(String name){
        this.name=name;
        this.start();
    }
    
    public void run(){
        Blockchain.executeWSLCommand("cd Prova &&"
                + "docker compose down");
        Blockchain.executeWSLCommand("cd Prova &&"
                + "docker compose up -d");
        
         
    }
}
