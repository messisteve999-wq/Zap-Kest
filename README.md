# Zap Kest — prototype Android

Application Android Kotlin de questions de conversation hors ligne avec bulle flottante activée par l'utilisateur.

## Fonctions
- Questions en français dans cinq catégories : faire connaissance, amitié, drague amicale, personnalité et relations à distance.
- Sélection quotidienne stable selon la date locale.
- Copie manuelle dans le presse-papiers.
- Bulle flottante déplaçable et panneau avec Autre question, Copier, Changer de catégorie et Fermer.
- Pas de compte, serveur, abonnement ou API payante.
- Workflow GitHub Actions pour compiler l'APK de test.

## Compiler
Pousser le projet sur GitHub puis ouvrir Actions > Build Zap Kest APK > Run workflow. Télécharger ensuite l'artefact `Zap-Kest-debug-APK`.

## Tester
1. Installer l'APK sur Android.
2. Ouvrir Zap Kest et activer la fenêtre flottante.
3. Autoriser « Afficher par-dessus les autres applications ».
4. Revenir dans l'application et activer la bulle.
5. Tester au-dessus de WhatsApp, Messenger et SMS ; copier une question puis la coller manuellement.

## Limites
C'est un prototype à compiler et tester sur appareil réel. Android ou certaines applications peuvent masquer les fenêtres flottantes. La bibliothèque initiale contient 50 questions d'exemple et peut être enrichie. Les suggestions ne constituent pas un diagnostic psychologique ni une garantie d'attirer quelqu'un.

## Version prototype
Le prototype comprend 50 questions d'exemple. Le choix du jour est stable pour une même date locale ; « Autre question » avance dans les suggestions disponibles. Les favoris et l'historique complet restent à ajouter dans une version suivante.
