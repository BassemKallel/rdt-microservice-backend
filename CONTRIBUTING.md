🤝 Guide de Contribution - Backend Replate
Bienvenue dans l'équipe ! Ce guide explique comment cloner, installer et contribuer à l'architecture microservices du projet Replate.

1. Clonage et Configuration Initiale
Avant de pouvoir coder, vous devez mettre en place l'environnement de développement complet, qui inclut l'infrastructure Docker et les microservices Spring Boot.

Étape 1 : Prérequis
Assurez-vous d'avoir installé :

Git

Docker Desktop

Java 17 (ou supérieur)

IntelliJ IDEA (recommandé) ou un IDE équivalent

Étape 2 : Cloner le Dépôt (Monorepo)
Clonez le dépôt principal sur votre machine locale.

Bash

git clone https://github.com/[VOTRE_ORGANISATION]/rdt-microservice-backend.git
cd rdt-microservice-backend
Étape 3 : Lancer l'Infrastructure Externe
Tous nos services (PostgreSQL, Kafka, MinIO, MongoDB) sont gérés par Docker.

Bash

docker compose up -d
Attendez que tous les conteneurs soient en statut "running".

Étape 4 : Ouvrir le Projet dans IntelliJ
Ce projet est un Monorepo. Vous devez ouvrir le dossier racine et importer tous les sous-projets.

Dans IntelliJ, choisissez File > Open et sélectionnez le dossier racine rdt-microservice-backend.

Ouvrez l'onglet Maven (sur la droite de l'IDE).

Cliquez sur l'icône "Reload All Maven Projects" (flèches circulaires).

Note : IntelliJ va maintenant télécharger les dépendances pour les 8+ microservices (UMS, Gateway, Eureka, etc.). Cela peut prendre quelques minutes.

Étape 5 : Lancer les Microservices (Ordre Important)
L'ordre de lancement est crucial pour que la découverte de services fonctionne.

Serveur de Découverte : Lancez EurekaServerApplication.

Infrastructure Spring : Lancez ApiGatewayApplication et FileServiceApplication.

Services Métier : Lancez UserManagementServiceApplication (et les autres services sur lesquels vous travaillez).

Étape 6 : Validation
Ouvrez votre navigateur et vérifiez le tableau de bord Eureka : http://localhost:8761. Vous devriez voir tous les services que vous avez lancés (API-GATEWAY, FILE-SERVICE, USER-MANAGEMENT-SERVICE) avec le statut UP.

2. Processus de Contribution (Workflow de Développement)
Suivez ces étapes pour ajouter de nouvelles fonctionnalités ou corriger des bugs.

Étape 1 : Créer une Branche
Ne travaillez jamais directement sur la branche main !

Assurez-vous d'être à jour :

Bash

git checkout main
git pull origin main
Créez votre branche de fonctionnalité. Utilisez un nom descriptif (ex: feature/OMS-crud-annonces ou fix/UMS-bug-validation) :

Bash

git checkout -b feature/OMS-crud-annonces
Étape 2 : Coder et Tester
Implémentez votre logique dans le microservice approprié (ex: offer-management-service).

Assurez-vous que le service démarre.

Utilisez la collection Postman du projet pour tester vos nouveaux endpoints avant de commiter.

Étape 3 : Commiter vos Changements
Faites des commits atomiques (petits et ciblés). Lorsque vous commitez, ajoutez uniquement le dossier du service que vous avez modifié (ou les fichiers pertinents) depuis la racine du monorepo.

Ajouter les changements :

Bash

# Exemple si vous avez modifié le service OMS
git add offer-management-service/
Commiter (Standard "Conventional Commits") : Utilisez des préfixes pour indiquer le service (scope) et le type de changement (type).

Type : feat (nouvelle fonctionnalité), fix (correction de bug), refactor (nettoyage de code), docs (documentation).

Scope : Le nom du microservice (ex: ums, oms, gateway, docker).

Bash

git commit -m "feat(oms): Ajout des endpoints CRUD pour les annonces"
Bash

git commit -m "fix(ums): Correction de l'exception lors du login"
Étape 4 : Pousser et Créer une Pull Request (PR)
Poussez votre branche vers le dépôt distant :

Bash

git push origin feature/OMS-crud-annonces
Allez sur GitHub et créez une Pull Request (PR) de votre branche vers la branche main.

Dans la description de la PR, expliquez ce que vous avez fait et (si possible) comment le tester.

Étape 5 : Revue de Code
Attendez que vos collaborateurs examinent votre code, fassent des commentaires, et approuvent la PR avant de la fusionner (Merge).
