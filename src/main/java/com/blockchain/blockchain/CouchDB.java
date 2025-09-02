/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.blockchain.blockchain;

import static com.blockchain.blockchain.Blockchain.mainDirectory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * 
 * @author simo0
 */
public class CouchDB extends Thread{
    private static String directory= Blockchain.mainDirectory;
    
    public CouchDB(){
        this.start();
    }
    
    /**
     * Method that updates the docker-compose.yaml file to include CouchDB
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private static void addCouchDBToDocker() throws IOException {
        File file = new File(mainDirectory + "/docker-compose.yaml");

        Map<String, Object> data;
        Yaml yaml = new Yaml();

        // Carica il contenuto del file, o crea una mappa vuota se il file è vuoto
        try (FileInputStream fis = new FileInputStream(file)) {
            data = yaml.load(fis);
            if (data == null) { // Se il file YAML è vuoto
                data = new HashMap<>();
            }
        } catch (FileNotFoundException e) {
            // Se il file non esiste, crea una nuova mappa vuota
            data = new HashMap<>();
            System.out.println("Creando un nuovo docker-compose.yaml poiché non trovato.");
        }


        // --- Gestione della sezione 'services' ---
        Map<String, Object> services = (Map<String, Object>) data.get("services");
        if (services == null) {
            services = new HashMap<>();
            data.put("services", services);
        }

        // Ottieni la mappa per 'couchdb' o creala se non esiste
        Map<String, Object> couchDB = (Map<String, Object>) services.get("couchdb");
        if (couchDB == null) {
            couchDB = new HashMap<>();
            services.put("couchdb", couchDB); 
        }

        // --- Configurazione del servizio CouchDB ---
        couchDB.put("container_name", "couchdb");
        couchDB.put("image", "couchdb:3.3.2");
        couchDB.put("environment", Arrays.asList(
                "COUCHDB_USER=admin",      
                "COUCHDB_PASSWORD=adminPsw" 
        ));
        couchDB.put("ports", Arrays.asList("5984:5984")); 
        couchDB.put("networks", Arrays.asList("fabric-network")); 
        couchDB.put("volumes", Arrays.asList("couchdb-data:/opt/couchdb/data")); 

        // --- Gestione della sezione 'networks' di primo livello ---
        // 'networks' è un fratello di 'services', non al suo interno.
        Map<String, Object> networks = (Map<String, Object>) data.get("networks");
        if (networks == null) {
            networks = new HashMap<>();
            data.put("networks", networks);
        }

        // Ottieni la mappa per 'fabric-network' o creala se non esiste
        Map<String, Object> fabricNetwork = (Map<String, Object>) networks.get("fabric-network");
        if (fabricNetwork == null) {
            fabricNetwork = new HashMap<>();
            networks.put("fabric-network", fabricNetwork); 
        }
        fabricNetwork.put("name", "fabric-network");
        fabricNetwork.put("driver", "bridge");

        // --- Gestione della sezione 'volumes' di primo livello ---
        Map<String, Object> volumes = (Map<String, Object>) data.get("volumes");
        if (volumes == null) {
            volumes = new HashMap<>();
            data.put("volumes", volumes);
        }

        // Aggiungi la definizione del volume 'couchdb-data'
        // Spesso è una mappa vuota per i volumi nominati
        Map<String, Object> couchDBVolume = (Map<String, Object>) volumes.get("couchdb-data");
        if (couchDBVolume == null) {
            couchDBVolume = new HashMap<>();
            volumes.put("couchdb-data", couchDBVolume);
        }
        
        //Scrittura sul file
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); // Usa lo stile a blocco per una migliore leggibilità
        options.setPrettyFlow(true);                               // Formatta l'output in modo leggibile (indentazione, ecc.)
        options.setIndent(2);                                      // Imposta l'indentazione a 2 spazi (standard YAML)
        options.setIndicatorIndent(0);                             // Nessuna indentazione aggiuntiva per gli indicatori di lista
        options.setLineBreak(DumperOptions.LineBreak.UNIX);        // Garantisce terminazioni di riga consistenti (Unix-style)

        
        yaml = new Yaml(options);

        
        // Usiamo un try-with-resources per garantire che il FileWriter sia chiuso automaticamente
        try (FileWriter writer = new FileWriter(file)) {
            yaml.dump(data, writer); // Questo serializza la 'data' Map nel 'writer'
        }
    }
    
    /**
     * Database startup method
     * @throws java.io.IOException
     */
    @Override
    public void run(){
        try {
            addCouchDBToDocker();
        } catch (IOException ex) {
            Logger.getLogger(CouchDB.class.getName()).log(Level.SEVERE, null, ex);
        }
        Blockchain.executeWSLCommand("cd "+directory+" &&"
                + "docker compose down");
        Blockchain.executeWSLCommand("cd "+directory+" &&"
                + "docker compose up -d");
    }
    
    public static boolean exists(){
        String output= Blockchain.executeWSLCommandToString("cd "+mainDirectory+" && docker ps");
        if(output.contains("couchdb")){
            return true;
        }else{
            return false;
        }
    }
}
