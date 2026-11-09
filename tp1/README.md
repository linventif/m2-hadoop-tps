# TP1 – Hadoop et HDFS sous Docker

> Master MIAGE – Ingénierie des Processus Métier
> Sujet : [M2 IPM 1 - TP1 Hadoop.pdf](M2%20IPM%201%20-%20TP1%20Hadoop.pdf)
> Aide-mémoire des commandes : [../commandes-hadoop.md](../commandes-hadoop.md)

**Objectifs**
- monter un cluster Hadoop avec Docker ;
- installer Spark sur le nœud maître (namenode) ;
- manipuler le système de fichiers distribué HDFS avec `hadoop fs`.

**Environnement utilisé :** Ubuntu (Linux), Docker Compose v5, Hadoop 3.2.1, Spark 3.1.2 (Scala 2.12.10, Java 8).

> Le sujet est écrit pour **Windows** (PowerShell, WSL, Docker Desktop). Ce README donne les commandes **Linux**. Les différences sont signalées à chaque étape.

---

## Sommaire

1. [Architecture du cluster](#architecture-du-cluster)
2. [Partie 1 – Installation](#partie-1--installation)
3. [Partie 2 – Manipulations HDFS](#partie-2--manipulations-hdfs)
4. [Toutes les commandes d'un coup](#toutes-les-commandes-dun-coup)
5. [Problèmes rencontrés et solutions](#problèmes-rencontrés-et-solutions)

---

## Architecture du cluster

```
Machine hôte (Ubuntu)
│   ~/m2/m2-hadoop-tps/tp1/weblog_entries.txt
│          │
│          │ docker cp
│          ▼
├── Docker ──────────────────────────────────────────────────────────
│   ┌─ namenode (maître) ───────────┐          ┌─ datanode ──────────┐
│   │ disque Linux : /root, /opt    │ ─ put ─► │ blocs HDFS          │
│   │ annuaire HDFS (métadonnées)   │ ◄─ get ─ │ /hadoop/dfs/data    │
│   └───────────────────────────────┘          └─────────────────────┘
│   resourcemanager (YARN) · nodemanager · historyserver
```

| Conteneur | Rôle | Port accessible depuis la machine |
|---|---|---|
| `namenode` | Maître HDFS : connaît l'arborescence et l'emplacement de chaque bloc. C'est là qu'on travaille. | 9870 (web), 9000 (HDFS) |
| `datanode` | Stocke le contenu des fichiers, découpé en blocs | – |
| `resourcemanager` | Maître YARN : distribue le CPU et la RAM aux jobs | – |
| `nodemanager` | Exécute les tâches MapReduce / Spark | – |
| `historyserver` | Garde l'historique des jobs terminés | – |

### Les trois « endroits » à ne pas confondre

| Endroit | Invite du terminal | Commandes | Contenu |
|---|---|---|---|
| Machine hôte | `toybox@toybox-ub26:~$` | `docker …` | ton disque |
| Bash du namenode | `root@18c714fbc39b:/#` | `ls`, `cd`, `cat`… | disque Linux du conteneur |
| HDFS | même bash que ci-dessus | `hadoop fs -…` | système de fichiers distribué |

`ls` ne montre **jamais** HDFS, et `hadoop fs -ls` ne montre **jamais** le disque du conteneur.

---

## Partie 1 – Installation

### Étapes 1 à 3 – Installer et lancer Docker

Sous Windows, le sujet fait installer WSL puis Docker Desktop. Sous Linux, il suffit d'avoir Docker Engine et le plugin Compose :

```bash
docker --version
docker compose version
```

> Sous Linux, on écrit `docker compose` (avec un espace). La commande `docker-compose` (avec un tiret) du sujet est l'ancienne version et n'est pas installée.

### Étapes 4 et 5 – Récupérer Docker-Hadoop et se placer dedans

```bash
unzip Docker-Hadoop.zip
cd Docker-Hadoop
```

Cette configuration vient de [big-data-europe/docker-hadoop](https://github.com/big-data-europe/docker-hadoop).

> **Spark n'est pas versionné dans ce dépôt** : il pèse ~250 Mo et est exclu par le `.gitignore`. S'il manque, retélécharge-le depuis le site d'Apache :
> ```bash
> wget https://archive.apache.org/dist/spark/spark-3.1.2/spark-3.1.2-bin-hadoop3.2.tgz
> tar xzf spark-3.1.2-bin-hadoop3.2.tgz
> ```

### Étape 6 – Démarrer le cluster

```bash
docker compose up -d
docker ps          # les 5 conteneurs doivent être "Up (healthy)" au bout d'une minute
```

| Commande | Effet |
|---|---|
| `docker compose down` | arrête le cluster, **les fichiers HDFS sont conservés** |
| `docker compose down -v` | arrête **et efface** HDFS (supprime les volumes) |

### Étapes 7 à 9 – Entrer dans le namenode et vérifier Hadoop

```bash
docker exec -it namenode bash     # l'invite devient root@<id>:/#
hadoop version                    # → Hadoop 3.2.1
exit                              # retour sur la machine hôte
```

### Étape 10 – Copier Spark dans le namenode

Depuis le dossier `Docker-Hadoop`, **sur la machine hôte** :

```bash
docker cp spark-3.1.2-bin-hadoop3.2 namenode:/opt/spark-3.1.2
```

Comme `/opt/spark-3.1.2` n'existe pas encore dans le conteneur, Docker copie le dossier et le renomme `spark-3.1.2`.

> **La commande du PDF ne marche pas sous Linux :** `docker cp .\spark-3.1.2-bin-hadoop3.2\ namenode:/opt/spark-3.1.2 .`
> - `\` est le séparateur Windows. Sous Linux, c'est un caractère d'échappement, il faut donc écrire `/`.
> - Le ` .` final est le point de la phrase, pas un argument. `docker cp` prend exactement deux arguments, `SOURCE DESTINATION`.
>
> Résultat de la commande telle quelle : `must specify at least one container source`.

**Rendre les scripts Spark exécutables.** Le zip a perdu les droits d'exécution (il a sans doute été créé sous Windows). Sans cette étape, on obtient `Permission denied` à l'étape 12 :

```bash
docker exec namenode bash -c 'chmod +x /opt/spark-3.1.2/bin/* /opt/spark-3.1.2/sbin/*'
```

### Étapes 11 et 12 – Lancer Spark

```bash
docker exec -it namenode bash
/opt/spark-3.1.2/bin/spark-shell
```

Résultat attendu :

```
Welcome to
      ____              __
     / __/__  ___ _____/ /__
    _\ \/ _ \/ _ `/ __/  '_/
   /___/ .__/\_,_/_/ /_/\_\   version 3.1.2
      /_/

Using Scala version 2.12.10 (OpenJDK 64-Bit Server VM, Java 1.8.0_232)
...
scala>
```

- **Pour quitter :** `:quit` ou Ctrl+D. Le Ctrl+C du sujet marche aussi, mais il coupe le programme brutalement.
- **`./opt/…` ou `/opt/…` :** le sujet écrit `./opt/...`, qui ne marche que si l'on est à la racine `/`. `/opt/...` (chemin absolu) marche depuis n'importe quel dossier.

> `spark-shell` est une console **Scala**. Hadoop est écrit en Java et Spark en Scala, et les deux tournent sur la JVM. Spark existe aussi en Python (`pyspark`) et en SQL (`spark-sql`).

---

## Partie 2 – Manipulations HDFS

Toutes les commandes HDFS ont la forme `hadoop fs -<commande> [options]`. Pour trouver de l'aide :

```bash
hadoop fs                 # liste des commandes
hadoop fs -help <cmd>     # aide détaillée d'une commande
```

Les commandes se tapent **dans le bash du namenode** (`docker exec -it namenode bash`), sauf les `docker cp`, qui se tapent sur la machine hôte.

> Des lignes `INFO sasl.SaslDataTransferClient: SASL encryption trust check…` peuvent s'afficher : ce sont des messages d'information, pas des erreurs.

### Exercice 1 – Créer le répertoire de l'utilisateur root sous HDFS

```bash
hadoop fs -mkdir -p /user/root
```

Ce dossier est le « home » de root dans HDFS. Tout chemin **relatif** (sans `/` au début) part de là : `data` veut dire `/user/root/data`.

- **`-p` :** crée les dossiers parents s'ils manquent, et n'affiche pas d'erreur si le dossier existe déjà.
- **Vérification :**
  ```bash
  hadoop fs -ls /user
  # drwxr-xr-x   - root supergroup          0 2026-11-09 09:06 /user/root
  ```

### Exercice 2 – Récupérer le fichier `weblog_entries.txt`

Le fichier est fourni avec le sujet. Il est dans ce dépôt : [`tp1/weblog_entries.txt`](weblog_entries.txt).

C'est un journal de visites d'un site web : **3 000 lignes** (254 130 octets), avec 5 colonnes séparées par des tabulations.

| Identifiant (hash) | Page visitée | Date | Heure | Adresse IP |
|---|---|---|---|---|
| `abba15edcd0c8042a14bf216c5` | `/jcwbtvnkkujo.html` | `2012-05-10` | `21:25:44` | `148.113.13.214` |

### Exercice 3 – Lister le contenu du répertoire par défaut du bash

```bash
pwd       # → /   (docker exec nous place à la racine)
ls        # contenu du dossier courant, sur le disque du conteneur
ls ~      # contenu du home de root : /root
```

`docker exec` ouvre le bash à la racine `/`, mais le dossier personnel de root est `/root` (`~`).

### Exercice 4 – Lister le répertoire par défaut de root sous HDFS

```bash
hadoop fs -ls             # sans argument = /user/root
hadoop fs -ls /user/root  # équivalent
```

À ce stade, le dossier est vide : la commande n'affiche rien.

### Exercice 5 – Créer `data` contenant `weblogs` sous HDFS

```bash
hadoop fs -mkdir -p data/weblogs
hadoop fs -ls -R data
# drwxr-xr-x   - root supergroup          0 ... data/weblogs
```

Le chemin étant relatif, le dossier créé est `/user/root/data/weblogs`.

> **Piège :** `hadoop fs -ls /user` ne montre **pas** `data`, seulement `root`, car `-ls` n'affiche qu'un niveau. Il faut `hadoop fs -ls` (ou `-ls -R /user`).

### Exercice 6 – Copier `weblog_entries.txt` dans `weblogs`

Il faut deux étapes, parce que le fichier doit d'abord arriver sur le disque du conteneur avant de pouvoir entrer dans HDFS.

**a) Machine hôte → conteneur** (sur la machine hôte, depuis la racine du dépôt) :

```bash
docker cp tp1/weblog_entries.txt namenode:/root/
```

**b) Conteneur → HDFS** (dans le bash du namenode) :

```bash
hadoop fs -put /root/weblog_entries.txt data/weblogs/
```

> **Piège :** `hadoop fs -cp` ne sert **pas** à ça. `-cp` copie d'un endroit de HDFS à un autre. Avec un fichier local, elle répond `` cp: `/root/weblog_entries.txt': No such file or directory ``, car elle cherche le fichier dans HDFS.
>
> | Commande | De | Vers |
> |---|---|---|
> | `-put` | disque du conteneur | HDFS |
> | `-get` | HDFS | disque du conteneur |
> | `-cp`, `-mv` | HDFS | HDFS |

### Exercice 7 – Lister le contenu de `weblogs`

```bash
hadoop fs -ls data/weblogs
```

```
Found 1 items
-rw-r--r--   3 root supergroup     254130 2026-11-09 09:40 data/weblogs/weblog_entries.txt
│            │ │    │               │      │                └ chemin
│            │ │    │               │      └ date de modification
│            │ │    │               └ taille en octets
│            │ │    └ groupe
│            │ └ propriétaire
│            └ facteur de réplication (nombre de copies voulues)
└ droits
```

> La réplication vaut 3, mais le cluster n'a qu'**un** datanode. HDFS ne peut donc stocker qu'une copie et signale des blocs *under replicated*. C'est normal dans ce TP.

### Exercice 8 – Copier vers `data/web/log.txt` et afficher la fin du fichier

```bash
hadoop fs -mkdir -p data/web
hadoop fs -cp data/weblogs/weblog_entries.txt data/web/log.txt
hadoop fs -tail data/web/log.txt
```

- **`-mkdir -p` :** `data/web` n'existe pas encore. Sans ce dossier, `-cp` échoue.
- **`-cp … data/web/log.txt` :** copie **dans HDFS** et renomme au passage.
- **`-tail` :** affiche le **dernier kilo-octet** du fichier, et non un nombre de lignes. La première ligne affichée peut donc être coupée.

Fin du résultat :

```
22b2549649dcc284ba8bf7d4993ac62	/e.html	2012-05-10	21:12:05	105.57.100.182
3ab7888ffe27c2f98d48eb296449d5	/khvc.html	2012-05-10	21:25:58	111.147.83.42
65827078a9f7ccce59632263294782db	/c.html	2012-05-10	21:34:28	137.157.65.89
```

Pour un nombre de lignes précis, on redirige la sortie de `-cat` vers `tail` :

```bash
hadoop fs -cat data/web/log.txt | tail -n 10
```

### Exercice 9 – Effacer le répertoire `weblogs`

```bash
hadoop fs -rm -r data/weblogs
# Deleted data/weblogs

hadoop fs -ls -R data
# drwxr-xr-x   - root supergroup          0 ... data/web
# -rw-r--r--   3 root supergroup     254130 ... data/web/log.txt
```

- **`-r` :** obligatoire pour supprimer un dossier.
- **Suppression définitive :** la corbeille HDFS est désactivée sur ce cluster (`fs.trash.interval = 0`).

### Exercice 10 – Copier `log.txt` dans le bash du namenode

```bash
cd ~
hadoop fs -get data/web/log.txt .
ls -l
# -rw-r--r-- 1 root root 254130 ... log.txt
```

Ici, le `.` est un vrai argument : il désigne le dossier courant, `/root`.

*Bonus :* pour récupérer le fichier jusque sur la machine hôte, lance sur la machine hôte :

```bash
docker cp namenode:/root/log.txt .
```

### Exercice 11 – Interface web du namenode

Ouvrir **http://localhost:9870**, puis **Utilities → Browse the file system**, et aller dans `/user/root/data/web`.

L'interface permet de naviguer, créer des dossiers et supprimer des fichiers, comme les commandes précédentes.

- **Onglet *Datanodes* :** montre le datanode et l'espace utilisé.
- **Upload et téléchargement :** ils peuvent échouer. Le navigateur est redirigé vers `datanode:9864`, qui n'est pas accessible depuis la machine hôte dans cette configuration Docker.

---

## Toutes les commandes d'un coup

```bash
# ───── Machine hôte ─────
cd Docker-Hadoop
docker compose up -d
docker cp spark-3.1.2-bin-hadoop3.2 namenode:/opt/spark-3.1.2
docker exec namenode bash -c 'chmod +x /opt/spark-3.1.2/bin/* /opt/spark-3.1.2/sbin/*'
docker cp ../tp1/weblog_entries.txt namenode:/root/
docker exec -it namenode bash

# ───── Bash du namenode ─────
hadoop version
/opt/spark-3.1.2/bin/spark-shell                 # puis :quit

hadoop fs -mkdir -p /user/root                   # ex. 1
ls                                               # ex. 3
hadoop fs -ls                                    # ex. 4
hadoop fs -mkdir -p data/weblogs                 # ex. 5
hadoop fs -put /root/weblog_entries.txt data/weblogs/   # ex. 6
hadoop fs -ls data/weblogs                       # ex. 7
hadoop fs -mkdir -p data/web                     # ex. 8
hadoop fs -cp data/weblogs/weblog_entries.txt data/web/log.txt
hadoop fs -tail data/web/log.txt
hadoop fs -rm -r data/weblogs                    # ex. 9
cd ~ && hadoop fs -get data/web/log.txt .        # ex. 10
```

---

## Problèmes rencontrés et solutions

| Symptôme | Cause | Solution |
|---|---|---|
| `must specify at least one container source` | syntaxe Windows (`\`) et ` .` final recopié du PDF | `docker cp spark-3.1.2-bin-hadoop3.2 namenode:/opt/spark-3.1.2` |
| `bash: …/spark-shell: Permission denied` | droits d'exécution perdus dans le zip | `chmod +x /opt/spark-3.1.2/bin/* /opt/spark-3.1.2/sbin/*` |
| `…/spark-submit: Permission denied` après un `chmod` sur `spark-shell` seul | `spark-shell` appelle d'autres scripts, eux aussi non exécutables | même `chmod` sur **tous** les scripts |
| `docker-compose: command not found` | ancienne syntaxe | `docker compose` (avec un espace) |
| Le dossier créé n'apparaît pas avec `ls` | `ls` regarde le disque du conteneur, pas HDFS | `hadoop fs -ls` |
| `data` n'apparaît pas dans `hadoop fs -ls /user` | `data` est dans `/user/root`, et `-ls` n'affiche qu'un niveau | `hadoop fs -ls` ou `hadoop fs -ls -R /user` |
| `` cp: `/home/weblog_entries.txt': No such file or directory `` | `-cp` cherche la source dans HDFS | `hadoop fs -put` pour envoyer un fichier local |
| `hadoop: command not found` | commande tapée sur la machine hôte | `docker exec -it namenode bash` d'abord |
