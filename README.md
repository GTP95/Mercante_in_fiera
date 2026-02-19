# Mercante in Fiera

Un videogioco Android per il classico gioco da tavolo "Mercante in Fiera".

## Descrizione
Implementazione del gioco "Mercante in Fiera" (asta, eliminazione, premi) utilizzando Kotlin e Jetpack Compose.

## Tecnologie utilizzate
- Kotlin
- Jetpack Compose
- Architecture Components (ViewModel, StateFlow)
- Coroutines

## Struttura del progetto
- `models/` - Contiene i modelli dati come CardModel
- `data/` - Contiene la logica di gestione dei dati e dei mazzi
- `ui/` - Contiene i composables dell'interfaccia utente
- `viewmodel/` - Contiene i ViewModel per la gestione dello stato

## Funzionalità
- Due mazzi da 40 carte ciascuno (Mercante e Giocatore)
- Sistema di placeholder per le carte fino all'aggiunta degli asset grafici
- Animazioni di flip per mostrare/nascondere le carte
- Architettura MVVM

## Dipendenze
- Jetpack Compose
- ViewModel
- StateFlow
- Coroutines

## Come compilare il progetto

### Prerequisiti
- Android SDK con API level 34
- Android build-tools versione 34.0.0 o superiore
- Java Development Kit (JDK) 11 o superiore
- Gradle 8.0 o superiore

### Installazione delle dipendenze
Assicurarsi di avere installato tutti i componenti necessari tramite Android SDK Manager:
```bash
sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

### Compilazione ed Esecuzione per Test
Per installare ed eseguire la versione di debug sul dispositivo collegato:

```bash
cd mercanteinfiera
./gradlew installDebug
```

### Compilazione per la distribuzione
Per creare un APK firmato per la distribuzione:

```bash
./gradlew assembleRelease
```

L'APK finale sarà disponibile in `app/build/outputs/apk/release/`.

### Ambiente di sviluppo consigliato
- Android Studio Flamingo o versione successiva
- Oppure qualsiasi editor di testo con supporto per Kotlin e Gradle
