# gtfs-osm-import
Java tool to import/sync GTFS data into OSM 
  
*originally written by [davidef](https://github.com/davidef)*

## Requirements for building
- JDK 17


## [it] 1) Configurazione preliminare

Prima di tutto è necessario clonare i file sorgenti del progetto in locale:

Se hai installato Git usa questo comando: `git clone https://github.com/gabboxl/gtfs-osm-import`, altrimenti [clicca qui per scaricare il sorgente](https://github.com/Gabboxl/gtfs-osm-import/archive/refs/heads/main.zip).

- Se hai scaricato i sorgenti come file .zip, estraili in una cartella a tuo piacimento.
-----

Prima di compilare e avviare lo strumento bisogna configurare alcune cosette:
1) Scarica i file GTFS dell'azienda di trasporti interessata (al momento è stato sviluppato solo un plugin per gestire i dati del Gruppo Torinese Trasporti).
2) Crea una cartella vuota con nome e posizione a tuo piacimento e estrai i file GTFS al suo interno dal file .zip
3) Crea un'altra cartella vuota a tuo piacimento dove saranno salvati i file generati da questo tool
4) Apri il file `gtfs-import.properties` situato in `src\main\resources`
5) Modifica le variabili `gtfs-path`, `osm-path`, `output-path` con i percorsi alle cartelle create in precedenza come da esempio.
6) Le variabili `osm-path` e `output-path` devono contenere lo stesso percorso.


## 2) Compilazione
1)  Prima di tutto entra nella cartella dei sorgenti del tool

2) *Prima di procedere assicurati di aver installato JDK 17!*

3) Per compilare il programma e generare il file .jar eseguibile usa il comando in base al tuo sistema operativo:

  *Windows*: `.\gradlew.bat shadowJar`

  *Linux/macOS*: `./gradlew shadowJar`

  ----------




## [it] Come si usa
*Attualmente questo tool è ancora in via di sviluppo preliminare, per cui i comandi al momento presenti potrebbero subire modifiche importanti da un giorno all'altro!*


### 1) Avvio del tool
- Il file .jar eseguibile generato si trova nella cartella `build\libs`.

- Per eseguirlo usa questo comando:
```bash
java -jar shadow.jar
```
(se il comando java non è trovato devi aggiungere i file binari Java alla variabile d'ambiente PATH del tuo sistema operativo... Usa Google :P)


Questi sono i comandi disponibili nel tool al momento:
```
Available commands:
update  Generate/update osm data from api server
check   Check and validate OSM relations
conf    Display current configuration
help    Display available commands
updates Generate/update single relation from api server
bbox    Get the Bounding Box of the GTFS File and xapi links
geojson Generate a geojson file containg osm relations
sqlite  Generate a sqlite db containg osm relations
gpx     Generate .gpx file for all GTFS Trips
stops   Generate files to import bus stops into osm merging with existing stops
stops   Generate files to import bus stops into osm merging with existing stops (export to small file)
reldiffx        Analyze the diff between osm relations and gtfs trips
reldiff Analyze the diff between osm relations and gtfs trips
rels    Generate the base relations (including only stops) to be used only when importing without any existing relation in osm
exit    Exit from GTFSImport
```

Attualmente, per come il tool è stato originariamente sviluppato, bisogna eseguire alcuni step obbligatori al primo avvio del tool *nel seguente ordine*:
1) `update` per scaricare i dati da OSM
2) `stops` per aggiornare i dati delle fermate con i dati GTFS
3) Unisci tutti i file delle fermate nuove e modificate creati: (con Josm devi aprire i file `gtfs_import_osm_with_gtfsid_not_found.osm`, `gtfs_import_paired_with_different_gtfsid.osm`, `gtfs_import_unpaired_in_gtfs.0.osm` e `stops.osm`)
4) Una volta aperti i file su Josm, selezionali tutti nella sezione Livelli a destra, poi fai *click destro* -> *unisci/merge* -> *imposta come livello di destinazione il file stops.osm*
5) Conferma l'unificazione dei file/livelli, Salva il file stops.osm e chiudi Josm SENZA eseguire l'upload dei dati.
6) Ora puoi eseguire tutti gli altri comandi disponibili nel tool




## [en] How to use
WIP
