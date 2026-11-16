# BAH Academy — par Bassouda Haïdara

Application Android d'apprentissage avec une interface sombre, moderne et arrondie. Le projet est préparé pour être ouvert et compilé dans Android Studio.

## Fonctions présentes

- **Accueil** : tableau de bord, accès rapide, aperçu des leçons et signature « Créée par Bassouda Haïdara ».
- **Carnet numérique** : créer, modifier, rechercher, consulter, partager et supprimer des leçons. Enregistrement local.
- **Scanner de cours** : sélection de plusieurs photos/pages, reconnaissance de texte OCR avec ML Kit Latin, vérification et modification du texte avant sauvegarde.
- **Compréhension** : résumé assisté par IA, zone personnelle « ce que j'ai compris », comparaison pédagogique avec le cours.
- **Évaluation** : entraînement avec questions courtes, quiz généré par IA, création de sujets et correction commentée par IA.
- **Révision** : accès aux fiches de révision et méthode de révision espacée.
- **Cours et groupes** : création de groupes locaux, invitations à partager, espaces d'étude, ajout de liens de visioconférence et ouverture des réunions.
- **Dictée vocale** : saisie d’une explication par la reconnaissance vocale Android en français (la disponibilité hors ligne dépend du moteur et du pack de langue installés).
- **Messagerie** : discussions et messages conservés localement, partage de documents via les applications Android.
- **Services** : assistant pédagogique IA, recherche web Android, aide en mathématiques, physique-chimie, informatique et préparation de schémas.
- **Réglages** : choix Groq/OpenRouter, modèle configurable, stockage local des clés, export et effacement des données.

## Ouvrir et compiler

1. Décompresse `BAH_Academy.zip`.
2. Ouvre le dossier `BAH_Academy` dans Android Studio (Open).
3. Utilise JDK 17 et laisse Android Studio télécharger les dépendances Gradle/AndroidX/ML Kit.
4. Installe Android SDK Platform 35 et Build Tools compatibles avec Android Gradle Plugin 8.7.3.
5. Lance **Sync Project with Gradle Files**, puis **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. Installe l'APK sur ton téléphone Android.

Le projet utilise `minSdk 26` et `targetSdk 35`. Une connexion Internet est nécessaire au premier téléchargement des dépendances et aux fonctions IA.

## Configurer l'IA

Dans l'application : Réglages → choisir Groq ou OpenRouter → coller la clé API correspondante → enregistrer.

- Groq : https://console.groq.com/keys
- OpenRouter : https://openrouter.ai/settings/keys

La clé reste dans les préférences privées de l'application sur le téléphone. Ne la partage pas et ne l'ajoute pas au code source. Modèle par défaut Groq : `llama-3.3-70b-versatile`. Modèle par défaut OpenRouter : `openrouter/free`. Tu peux remplacer le modèle dans Réglages si ton fournisseur propose un autre modèle.

## Fonctionnement hors ligne / en ligne

- **Hors ligne** : ouvrir et modifier les leçons déjà enregistrées, écrire ses explications, créer des groupes et messages locaux, consulter les données conservées sur l'appareil. La reconnaissance OCR ML Kit Latin fonctionne sur l'appareil après installation de ses composants.
- **En ligne** : assistant IA, résumés et comparaison IA, quiz générés par IA, recherche web et réunions ouvertes par lien.

## Limites techniques à connaître (transparence)

Cette archive contient un projet Android complet pour les fonctions locales et les appels IA. Les écrans de groupes et messagerie gèrent actuellement des espaces **locaux à l'appareil**. Ils ne synchronisent pas encore les messages entre téléphones et ne fournissent pas un serveur de visioconférence intégré. Pour rendre les conversations multi-utilisateurs réellement temps réel, il faut créer et sécuriser un backend (par exemple Firebase Authentication + Firestore/Realtime Database + stockage) et configurer un fournisseur WebRTC/visioconférence. Ces services nécessitent des identifiants et règles de sécurité propres au propriétaire du service ; ils ne peuvent pas être inventés ni préconfigurés sans ton projet serveur. Les liens de réunion existants peuvent être ouverts depuis l'application.

L'OCR reconnaît le texte imprimé/manuscrit de façon variable selon la photo ; les équations complexes, tableaux et schémas doivent être vérifiés et corrigés avant d'enregistrer le cours. La dictée vocale utilise le service de reconnaissance installé sur Android ; son fonctionnement sans Internet n’est pas garanti.

## Structure

- `app/src/main/java/com/bassouda/bahacademy/MainActivity.kt` : interface, navigation, stockage, OCR et fonctions IA.
- `app/src/main/AndroidManifest.xml` : permissions et activité principale.
- `app/build.gradle.kts` : dépendances et paramètres Android.

**Marque : BAH Academy — créée par Bassouda Haïdara.**
