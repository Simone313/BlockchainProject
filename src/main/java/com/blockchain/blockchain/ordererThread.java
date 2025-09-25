/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.blockchain.blockchain;

/**
 *
 * @author simo0
 */
public class ordererThread extends Thread {
    String mainDir;
    public ordererThread(String mainDir){
        this.mainDir=mainDir;
        this.start();
    }
    
    public void run(){
        Blockchain.executeWSLCommand("cd "+mainDir+" &&"
                + "docker compose down");
        Blockchain.executeWSLCommand("cd "+mainDir+" &&"
                + "docker compose up -d");
    }
}
