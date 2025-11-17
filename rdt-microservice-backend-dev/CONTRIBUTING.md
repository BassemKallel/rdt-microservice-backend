# Replate - Backend Microservices

Ce dépôt contient l'architecture microservices backend pour la plateforme Replate. Le système est construit avec Spring Boot et géré via Docker Compose pour l'infrastructure.

## 🧭 Vue d'ensemble de l'Architecture

Le projet suit une architecture microservices complète incluant :
* **Service Discovery** (`eureka-server`): Pour que les services puissent se trouver.
* **API Gateway** (`api-gateway`): Le point d'entrée unique pour toutes les requêtes front-end.
* **Services Métier** (`user-management-service`, `file-service`, etc.) : Gèrent la logique spécifique.
* **Communication Asynchrone** (`Kafka`): Pour les événements (ex: inscription utilisateur).
* **Infrastructure de Persistance** (`PostgreSQL`, `MinIO`, `MongoDB`): Bases de données et stockage de fichiers.

## 🛠️ Prérequis

Avant de commencer, assurez-vous d'avoir installé les outils suivants sur votre machine :
* Java 17 (ou une version compatible)
* Docker Desktop (en cours d'exécution)
* Votre IDE Java (IntelliJ IDEA est recommandé)
* Postman (pour les tests API)
* Maven (généralement inclus dans IntelliJ)

## 🐳 1. Lancement de l'Infrastructure (Docker)

Toutes nos bases de données et brokers sont gérés par Docker.

1.  Ouvrez un terminal à la racine du projet.
2.  Lancez tous les services d'infrastructure (PostgreSQL, Kafka, MinIO, MongoDB) :
    ```bash
    docker compose up -d
    ```
3.  Vérifiez que tout est en cours d'exécution :
    ```bash
    docker compose ps
    ```
    (Tous les services doivent être en statut `running`).

## ▶️ 2. Lancement des Microservices (Spring Boot)

Vous devez lancer les applications Spring Boot dans l'ordre suivant depuis IntelliJ :

1.  **`eureka-server`** (Attendre qu'il soit démarré)
2.  **`api-gateway`**
3.  **`file-service`**
4.  **`user-management-service`**
5.  *(...les autres services comme `offer-management-service`...)*

### Validation du Lancement

Ouvrez le tableau de bord **Eureka** dans votre navigateur pour confirmer que tous les services sont enregistrés et `UP` :
* **URL :** `http://localhost:8761`

## 📍 Répertoire des Endpoints (Localhost)

Voici les adresses locales pour accéder aux différents services :

| Service | Port (Local) | Usage |
| :--- | :--- | :--- |
| **API Gateway** | `http://localhost:8081` | **Point d'entrée principal pour tous les tests Postman.** |
| Eureka Dashboard | `http://localhost:8761` | Tableau de bord de la découverte de services. |
| MinIO Console | `http://localhost:9001` | Interface web pour voir les fichiers uploadés (Login: `minioadmin` / `miniopassword`). |
| PostgreSQL | `localhost:5432` | Accès DB (via DBeaver/pgAdmin) (Login: `rdtuser` / `rdtpassword`, DB: `rdt_db`). |
| MongoDB | `localhost:27017` | Accès DB (via Compass). |

---

## 🚀 3. Tests des Scénarios d'Usage (Postman)

Utilisez la collection Postman fournie pour tester les flux. Toutes les requêtes doivent passer par l'**API Gateway (port 8081)**.

### Scénario 1 : Inscription d'un nouveau Marchand

Ce scénario teste le `file-service` et le `user-management-service`.

1.  **Uploader l'image de profil (File Service)**
    * **Méthode :** `POST`
    * **URL :** `http://localhost:8081/api/v1/files/upload`
    * **Body (form-data) :**
        * `file` : [Choisir un fichier image.jpg]
        * `type` : `profiles`
    * **Réponse :** Copiez l'URL de MinIO (ex: `http://localhost:9000/replate-bucket/profiles/...`).

2.  **Créer le compte (UMS)**
    * **Méthode :** `POST`
    * **URL :** `http://localhost:8081/api/v1/users/register`
    * **Body (JSON) :**
        ```json
        {
            "email": "new_merchant@test.com",
            "password": "Password123!",
            "role": "MERCHANT",
            "registrationNumber": "REG-123",
            "profileImageUrl": "COPIEZ_L_URL_DE_L_ETAPE_1_ICI"
        }
        ```
    * **Réponse :** `201 Created` avec un message de succès.

### Scénario 2 : Connexion et Validation Admin (RDT-4)

Ce scénario teste l'authentification (JWT) et l'autorisation par rôle (`hasRole("ADMIN")`).

1.  **Connexion Admin** (L'admin est créé au démarrage par le `AdminSeeder`)
    * **Méthode :** `POST`
    * **URL :** `http://localhost:8081/api/v1/users/login`
    * **Body (JSON) :**
        ```json
        {
            "email": "admin@replate.com",
            "password": "admin12345"
        }
        ```
    * **Réponse :** Copiez le `jwtToken` de la réponse.

2.  **Consulter les comptes en attente (Admin)**
    * **Méthode :** `GET`
    * **URL :** `http://localhost:8081/api/v1/admin/pending`
    * **Authentification (Auth) :** Type `Bearer Token`, collez le token Admin.
    * **Réponse :** `200 OK` avec la liste des utilisateurs (y compris le "new_merchant" créé à l'étape 1).

3.  **Valider le compte (Admin)**
    * **Méthode :** `POST`
    * **URL :** `http://localhost:8081/api/v1/admin/validate/1` (Remplacez `1` par l'ID du marchand à valider).
    * **Authentification (Auth) :** Type `Bearer Token`, collez le token Admin.
    * **Réponse :** `200 OK`.

---

## 🧬 Pile Technologique

* Java 17
* Spring Boot 3+
* Spring Cloud Gateway (Routage)
* Spring Cloud Eureka (Découverte)
* Spring Security (JWT)
* Spring Data JPA (PostgreSQL)
* Spring Data MongoDB
* Spring Kafka (Broker de messages)
* MinIO (Stockage S3)
* Docker Compose
* Maven
