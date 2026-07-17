# Détecteur de plagiat

Application Java EE (JSP / Servlets / JDBC / MySQL / Tomcat 9) permettant à un utilisateur
de soumettre un TP sous forme d'archive ZIP contenant des PDF, puis de lancer une analyse
de plagiat (comparaison inter-étudiants, recherche GitHub, recherche web) via un script Python.

## Prérequis

- JDK (8+)
- Apache Tomcat 9
- MySQL / MariaDB
- Python 3 avec les dépendances : `pip install requests pymupdf`

## 1. Base de données

Créer une base `plagiarism_db` avec les tables suivantes :

```sql
CREATE DATABASE IF NOT EXISTS plagiarism_db;
USE plagiarism_db;

CREATE TABLE users (
    id       INT(11) AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    email    VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL   -- hash BCrypt, jamais du texte en clair
);

CREATE TABLE submissions (
    id          INT(11) AUTO_INCREMENT PRIMARY KEY,
    user_id     INT(11) NOT NULL,
    tp_name     VARCHAR(100) NOT NULL,
    file_path   VARCHAR(255),
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE analyses (
    id            INT(11) AUTO_INCREMENT PRIMARY KEY,
    submission_id INT(11) NOT NULL,
    analysis_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (submission_id) REFERENCES submissions(id)
);
```

> Note : le fichier `dao/ResultDAO.java` référence une table `similarity_results` qui n'est
> pas utilisée dans le flux actuel (les résultats sont stockés en JSON dans `results/`,
> pas en base). Cette classe est du code mort, elle peut être ignorée ou supprimée.

## 2. Configuration des chemins (`PLAGIARISM_HOME`)

L'application a besoin de connaître l'emplacement réel du projet sur le disque (pour
trouver `uploads/`, `results/` et `python/plagiarism_detector.py`), car Eclipse/Tomcat
déploie l'application dans un dossier temporaire différent du dossier du projet.

Définir une variable d'environnement (recommandé, survit aux redémarrages) :

- **Windows** : Panneau de configuration → Variables d'environnement → Nouvelle variable
  utilisateur `PLAGIARISM_HOME` = chemin complet du projet (ex. `D:\Dev\detecteur_de_plagiarisme2`)
  → redémarrer Eclipse.

- **Ou**, dans Eclipse : Servers → double-clic sur le serveur → *Open launch configuration*
  → onglet *Arguments* → ajouter dans **VM arguments** :
  ```
  -DPLAGIARISM_HOME="D:\Dev\detecteur_de_plagiarisme2"
  ```

Sans cette variable, `AppConfig` tente de déduire le dossier automatiquement à partir des
classes compilées, ce qui échoue dans certaines configurations de déploiement Eclipse WTP.

## 3. Secrets (GitHub token, SerpAPI, identifiants BDD)

Aucun secret n'est codé en dur dans le code. Deux options, au choix :

**Option A — variables d'environnement**
```
GITHUB_TOKEN=...
SERPAPI_KEY=...
DB_URL=jdbc:mysql://localhost:3306/plagiarism_db
DB_USER=root
DB_PASSWORD=...
```

**Option B — fichiers de configuration locaux (non versionnés, voir `.gitignore`)**
- `config.properties` à la racine du projet (copier `config.properties.example`) :
  identifiants BDD + chemins.
- `python/config.properties` (copier `python/config.properties.example`) :
  `GITHUB_TOKEN` et `SERPAPI_KEY`.

Sans configuration, les valeurs par défaut sont : `root` / mot de passe vide /
`localhost:3306/plagiarism_db`, et pas de token GitHub/SerpAPI (les recherches
correspondantes sont simplement ignorées avec un message d'avertissement).

## 4. Lancer le projet

1. Importer le projet dans Eclipse (Dynamic Web Project).
2. Configurer `PLAGIARISM_HOME` (étape 2).
3. Démarrer le serveur Tomcat depuis Eclipse.
4. Ouvrir `http://localhost:8080/<nom-du-projet>/` → redirige vers `auth.jsp`.

## 5. Politique de mot de passe

À l'inscription, le mot de passe doit contenir au moins 8 caractères, une majuscule,
une minuscule, un chiffre et un caractère spécial. Les mots de passe sont hachés avec
BCrypt avant stockage.

## 6. Structure des résultats

Chaque analyse produit un fichier indépendant : `results/result_<id_soumission>.json`.
Un utilisateur ne peut analyser ou consulter que ses propres soumissions.
