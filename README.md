# MoviesApp Azizy 🎬

**MoviesApp Azizy** est une application Android moderne développée en Java permettant de découvrir les derniers films, de gérer un profil utilisateur avec détection faciale intelligente et de trouver des cinémas à proximité grâce à la géolocalisation.

## 🚀 Fonctionnalités Clés

### 1. Découverte de Films
*   Intégration de l'API **TMDB (The Movie Database)** pour récupérer les informations en temps réel.
*   Affichage des détails : Titre, synopsis, note moyenne et date de sortie.
*   Chargement fluide des posters avec la bibliothèque **Glide**.

### 2. Profil Utilisateur Intelligent (IA)
*   **Capture HD :** Prise de photo de profil en haute résolution via `FileProvider`.
*   **Détection Faciale :** Intégration de **Google Vision / ML Kit** pour s'assurer qu'un visage est présent sur la photo.
*   **Correction Auto :** Gestion de l'orientation de l'image via les métadonnées **EXIF**.

### 3. Services de Localisation (GPS)
*   **Google Maps SDK :** Affichage d'une carte interactive dans les détails du film.
*   **Recherche Contextuelle :** Trouve instantanément les cinémas les plus proches de votre position actuelle grâce au `FusedLocationProviderClient` et aux Intents Google Maps.

### 4. Lecteur Vidéo
*   Lecture des bandes-annonces de films directement dans l'application via un lecteur vidéo intégré.

## 🛠️ Stack Technique

*   **Langage :** Java
*   **Réseau :** Volley (Requêtes JSON asynchrones)
*   **Images :** Glide (Cache et optimisation mémoire)
*   **Cartographie :** Google Maps API & Google Play Services Location
*   **IA :** Google Vision API (Face Detection)
*   **Stockage :** SharedPreferences & Stockage Interne

## 📦 Installation

1. Cloner le dépôt :
   ```bash
   git clone https://github.com/azizyy1/MoviesApp-DevMobile-Azizy.git
   ```
2. Ouvrir le projet dans **Android Studio**.
3. S'assurer que les clés API sont bien configurées dans `TmdbConfig.java` et `AndroidManifest.xml`.
4. Compiler et lancer sur un émulateur ou un appareil physique (Android 7.0+).

## 📄 Documentation et Ressources
*   **Ressources du Projet :** [Rapport, Simulation et Présentation (Google Drive)]([https://drive.google.com/drive/folders/1taq-nja_0ZYav1f9nGSdFcqIpeRuBS5K](https://drive.google.com/drive/folders/1taq-nja_0ZYav1f9nGSdFcqIpeRuBS5K?usp=sharing))

## 📄 Auteur
*   **Azizy** - [GitHub](https://github.com/azizyy1)

---
*Projet réalisé dans le cadre du module de Développement Mobile.*
