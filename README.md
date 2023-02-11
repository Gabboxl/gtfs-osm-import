# gtfs-osm-import
Java tool to import/sync GTFS data into OSM 
  
*originally written by [davidef](https://github.com/davidef)*

**Please contribute!**


## What can it do?

- This tool can check what bus/subway/train stops are new or need to be removed from OSM based on GTFS data.

- The tool generates .osm files that can be opened with tools like JOSM to review the changes before uploading them to OSM.

- This tool features a GUI to compare OSM and GTFS stops that are too distant using aerial imagery.

- This tool can also generate *complete* OSM way-matched relations for any GTFS route. This is possible thanks to GraphHopper's [map-matching](https://github.com/graphhopper/graphhopper/tree/master/map-matching) module.



## Requirements
- JDK 17

## Download

You can download the [.jar file here](https://github.com/Gabboxl/gtfs-osm-import/releases/latest)! (check the *Assets* section)

## Roadmap

You can find a list of upcoming features [here](https://github.com/users/Gabboxl/projects/3)!



# [IT] Come si usa

## Primo avvio

1) Assicurati di aver installato i requisiti software indicati  qua sopra

2) Scarica il file .jar dal link qua sopra

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

7) Per visualizzare tutti i comandi disponibili del tool puoi usare l'opzione `-h`: `java -jar gtfs-osm-import.jar -h`

8) Puoi anche visualizzare le opzioni disponibili per un qualsiasi comando: `java -jar gtfs-osm-import.jar stops -h`

## Disclaimer

### Way-matching per OSM
Devi tenere a mente che il matching effettuato da GraphHopper non è preciso, ma può velocizzare notevolmente l'aggiunta delle vie alle relazioni. Per cui controlla attentamente ogni relazione generata prima di caricarle su OSM! 

## [EN] How to use
WIP
