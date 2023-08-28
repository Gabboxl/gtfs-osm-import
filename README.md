# gtfs-osm-import [![GitHub release](https://img.shields.io/github/release/Gabboxl/gtfs-osm-import.svg)](https://github.com/Gabboxl/gtfs-osm-import/releases)
Java tool to import/sync GTFS data into OSM 
  
*originally written by [davidef](https://github.com/davidef)*

**Please contribute!**


## What can it do?

- This tool can check what bus/subway/train stops are new or need to be removed from OSM based on GTFS data.

- The tool generates .osm files that can be opened with tools like JOSM to review the changes before uploading them to OSM.

- This tool features a GUI to compare OSM and GTFS stops that are too distant using aerial imagery.

- This tool can also generate *complete* OSM way-matched relations for any GTFS route. This is possible thanks to GraphHopper's [map-matching](https://github.com/graphhopper/graphhopper/tree/master/map-matching) module.

- The "*sync*" functionality is only implemented for bus stops, right now the relations generation is useful only in case of a clean relations import.



## Requirements
- JDK 17

## Download

You can download the [.jar file here](https://github.com/Gabboxl/gtfs-osm-import/releases/latest)! (check the *Assets* section)

## Roadmap

You can find a list of upcoming features [here](https://github.com/users/Gabboxl/projects/3)!

## How to compile
1) First of all, enter the tool's source folder.

2) Before proceeding, make sure you have installed JDK 17!

3) To compile the program and generate the executable .jar file, use the command according to your operating system:

Windows: `.\gradlew.bat shadowJar`

Linux/macOS: `./gradlew shadowJar`

4) The .jar file will be created in the folder `build/libs/`



# [IT] Come si usa

## Primo avvio

1) Assicurati di aver installato i requisiti software indicati sopra

2) Scarica il file .jar dalla sezione **Download** sopra

3) Da linea di comando, avvia il tool con il seguente comando:
```bash
java -jar gtfs-osm-import.jar
```

4) Il tool generera' un file .properties di esempio, se non già presente nella stessa posizione del file .jar.

5) Dovrai modificare questo file .properties: aprilo e assicurati di impostare un percorso valido per la generazione dei file per il nuovo import. 

(Se stai utilizzando il tool per una città diversa da Torino, dovrai modificare il link che punta ai dati GTFS della tua città. Inoltre dovrai creare una classe plugin specifica per processare i dati GTFS, leggi la sezione apposita qui sotto.)

6) Una volta modificato il file .properties in base alle proprie esigenze, puoi riavviare il tool con il comando precedente aggiungendo alla fine il nome del comando da eseguire. 
Per esempio, per generare un nuovo import delle fermate, usa la seguente sintassi:
```bash
java -jar gtfs-osm-import.jar stops
```

7) Per visualizzare tutti i comandi disponibili del tool puoi usare l'opzione `-h`:

`java -jar gtfs-osm-import.jar -h`

8) Puoi anche visualizzare le opzioni disponibili per un qualsiasi comando: 

`java -jar gtfs-osm-import.jar stops -h`

## I comandi

I comandi disponibili sono i seguenti: `stops`, `fullrels`, `conf`, `bbox`

----
Esiste anche una modalità interattiva avviabile con il comando `interactivedebug`. Essa contiene dei comandi sperimentali e di debug aggiuntivi (non documentati siccome possono cambiare da un giorno all'altro), inoltre può essere molto utile per inviare i comandi uno dopo l'altro da un IDE.

----

### Comando *stops*

Il comando *stops* è il comando principale per il controllo delle differenze tra le fermate presenti in OSM e quelle nei dati GTFS.

Una volta che il tool ha effettuato il matching con tutte le fermate di OSM e GTFS, è possibile che alcune fermate siano state spostate fisicamente.
In caso una o più fermate GTFS sono distanti più di 100 metri dalle rispettive fermate su OSM, verrà avviata una interfaccia grafica che permette di scegliere quali coordinate (OSM o GTFS) mantenere per la generazione dei file di import.

Una volta che il tool avrà eseguito correttamente il comando, verranno generati i seguenti file:

1) `gtfs_import_matched_with_updated_metadata.osm`: Questo file conterrà tutte le fermate già presenti su OSM, con i vari tag aggiornati secondo lo schema PTv2 e le coordinate scelte dall'interfaccia grafica (se utilizzata).

2) `gtfs_import_new_stops_from_gtfs.osm`: Questo file conterrà tutte le nuove fermate non ancora presenti su OSM.


3) `gtfs_import_not_matched_stops.osm`: Questo file conterrà tutte le fermate su OSM non presenti più nei dati GTFS.
Il tool marcherà ogni suddetta fermata come "*disused*", ma se non più presente fisicamente nella realtà è possibile eliminare il nodo.

*NOTA*: Si possono verificare dei falsi-positivi: alcune fermate possono venire contrassegnate per la rimozione erroneamente, per cui è ncessario rimuovere le fermate coinvolte manualmente dai file generati. Con JOSM per esempio è possibile utilizzare l'opzione "*purge*" (con combinazione `Ctrl+Shift+P`) dopo aver attivato la modalità "esperto" per rimuovere i nodi selezionati dal file senza rimuoveri durante l'upload dei dati su OSM.




### Comando *fullrels*
Il comando *fullrels* permette di generare un file contenente tutte le relazioni da importare su OSM dalle varie routes definite nei dati GTFS.

Di default il comando scarica tutte le ways (nell'area definita dalle fermate GTFS) di OSM in locale per effettuare un match con i dati dei percorsi (shape) dei file GTFS grazie alla libreria GraphHopper.

Il risultato sarà contenuto in un file nominato `gtfs_import_mergedFullRelations.osm` contenente tutte le relazioni formate da fermate e vie, da revisionare prima di caricare su OSM.

Il file conterrà inoltre le relazioni di tipo `route_master`, formate da tutte le varianti (i trip) di ogni route definita nei dati GTFS.

> **Warning**: A differenza del comando *stops*, la generazione delle relazioni con il comando *fullrels* è utile soltanto in caso su OSM non siano presenti alcune relazioni nella zona di interesse. In altre parole, il comando *fullrels* non tiene conto delle relazioni già presenti su OSM, per cui se si tentasse di caricare le nuove relazioni generate accadrebbe un disastro a causa di relazioni doppie. 

> **Note**: Prima di generare le relazioni assicurati di aver caricato e aggiornato tutte le fermate GTFS su OSM, altrimenti non potrai generarle!

### Disclaimer Way-matching per OSM
Devi tenere a mente che il matching effettuato da GraphHopper non è preciso, ma può velocizzare notevolmente l'aggiunta delle vie alle relazioni. Per cui controlla attentamente ogni relazione generata prima di caricarle su OSM! 


### Comando *conf*

Stampa la configurazione del tool attuale.


### Comando *bbox*

Stampa le coordinate del bounding box date dall'analisi delle fermate presenti nei dati GTFS.



# [EN] How to use

## Initial Startup

1. Make sure you have installed the software requirements indicated above

1. Download the .jar file from the **Download** section above

1. From the command line, start the tool with the following command:

```bash
java -jar gtfs-osm-import.jar
```

4. The tool will generate an example .properties file, if not already present in the same location as the .jar file.

1. You will need to modify this .properties file: open it and make sure to set a valid path for generating the files for the new import.

(If you are using the tool for a city other than Torino, you will need to modify the link that points to the GTFS data for your city. You will need to create a specific plugin class to process the GTFS data, also.)

6. Once the .properties file has been modified according to your needs, you can restart the tool with the previous command by adding the name of the command to execute at the end.
   For example, to generate a new import of stops, use the following syntax:

```bash
java -jar gtfs-osm-import.jar stops
```

7. To view all the available commands of the tool you can use the `-h` option:

`java -jar gtfs-osm-import.jar -h`

8. You can also view the options available for any command:

`java -jar gtfs-osm-import.jar stops -h`

## The Commands

The available commands are the following: `stops`, `fullrels`, `conf`, `bbox`

______________________________________________________________________

There is also an interactive mode that can be started with the `interactivedebug` command. It contains additional experimental and debugging commands (not documented as they can change from day to day), and it can be very useful for sending commands one after another from an IDE.

______________________________________________________________________

### *Stops* Command

The *stops* command is the main command for controlling the differences between the stops present in OSM and those in the GTFS data.

Once the tool has matched all the OSM and GTFS stops, some stops may have been physically moved.
In case one or more GTFS stops are more than 100 meters away from their respective stops on OSM, a graphical interface will be launched that allows you to choose which coordinates (OSM or GTFS) to keep for the generation of import files.

Once the tool has successfully executed the command, the following files will be generated:

1. `gtfs_import_matched_with_updated_metadata.osm`: This file will contain all the stops already present on OSM, with various tags updated according to the PTv2 schema and the coordinates chosen from the graphical interface (if used).

1. `gtfs_import_new_stops_from_gtfs.osm`: This file will contain all the new stops not yet present on OSM.

1. `gtfs_import_not_matched_stops.osm`: This file will contain all the stops on OSM no longer present in the GTFS data.
   The tool will mark each of these stops as "*disused*", but if it is no longer physically present in reality, the node can be deleted.

*NOTE*: False positives can occur: some stops may be marked for removal erroneously, so it is necessary to manually remove the involved stops from the generated files. With JOSM for example, you can use the "*purge*" option (with the combination `Ctrl+Shift+P`) after activating the "expert" mode to remove the selected nodes from the file without removing them during data upload to OSM.

### *Fullrels* Command

The *fullrels* command allows you to generate a file containing all the relationships to import to OSM from the various routes defined in the GTFS data.

By default, the command downloads all the ways (in the area defined by the GTFS stops) of OSM locally to match with the route data (shape) of the GTFS files using the GraphHopper library.

The result will be contained in a file named `gtfs_import_mergedFullRelations.osm` containing all the relationships formed by stops and ways, to be reviewed before uploading to OSM.

The file will also contain `route_master` type relationships, formed by all the variants (the trips) of each route defined in the GTFS data.

> **Warning**: Unlike the *stops* command, generating relationships with the *fullrels* command is useful only if some relationships are not present on OSM in the area of interest. In other words, the *fullrels* command does not take into account the relationships already present on OSM, so if you tried to upload the newly generated relationships, it would be a disaster due to duplicate relationships.

> **Note**: Before generating relationships, ensure you have uploaded and updated all GTFS stops on OSM, otherwise you will not be able to generate them!


### Way-matching Disclaimer for OSM

You should keep in mind that the matching performed by GraphHopper is not precise, but it can significantly speed up the addition of roads to relations. Therefore, carefully check each generated relation before uploading them to OSM!

### *conf* Command

Prints the current tool's configuration.

### *bbox* Command

Prints the coordinates of the bounding box given by the analysis of the stops present in the GTFS data.
