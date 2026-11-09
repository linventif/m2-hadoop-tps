# Aide-mémoire des commandes Hadoop

Testé sur le cluster Docker du TP (Hadoop 3.2.1, conteneurs `namenode`, `datanode`, `resourcemanager`…).

---

## 0. Où taper quoi ?

| Où | Invite du terminal | Commandes | Ce que tu manipules |
|---|---|---|---|
| Ta machine | `toybox@toybox-ub26:~$` | `docker cp`, `docker exec`, `docker compose` | ton disque |
| Bash du namenode | `root@18c714fbc39b:/#` | `ls`, `cd`, `cat`, `cp`… | le disque Linux du conteneur |
| HDFS | même bash que ci-dessus | `hadoop fs -…`, `hdfs …`, `yarn …` | le système de fichiers distribué |

- **Entrer dans le namenode :** `docker exec -it namenode bash`.
- **En sortir :** `exit` (ou Ctrl+D).
- **Différence entre `ls` et `hadoop fs -ls` :** `ls` montre le disque du conteneur, `hadoop fs -ls` montre HDFS. Ce sont deux mondes séparés.

> **Pièges du PDF**
> - Le ` .` à la fin des commandes est le point de la phrase : ne le tape pas.
> - Les chemins Windows `.\dossier\` s'écrivent `./dossier/` sous Linux.

---

## 1. Les chemins dans HDFS

| Forme | Exemple | Pointe vers |
|---|---|---|
| Relatif | `notes.txt` | `/user/root/notes.txt` (ton dossier « maison » dans HDFS) |
| Absolu | `/user/root/notes.txt` | ce chemin exact |
| URL complète | `hdfs://namenode:9000/user/root/notes.txt` | ce chemin exact (utile dans Spark) |
| Disque local | `file:///root/notes.txt` | le disque du conteneur, pas HDFS |

- **Dossier maison :** `/user/root` doit exister pour que les chemins relatifs marchent. On le crée avec `hadoop fs -mkdir -p /user/root`.
- **`hdfs dfs` :** c'est exactement la même chose que `hadoop fs`.

---

## 2. Obtenir de l'aide

```bash
hadoop fs                 # liste toutes les commandes
hadoop fs -help ls        # explication détaillée d'une commande
hadoop fs -usage ls       # juste la syntaxe
```

---

## 3. Lister et inspecter

| Commande | Ce que ça fait |
|---|---|
| `hadoop fs -ls` | liste ton dossier maison `/user/root` |
| `hadoop fs -ls /` | liste la racine de HDFS |
| `hadoop fs -ls -R /user` | liste tout récursivement |
| `hadoop fs -ls -h dossier` | tailles lisibles (K, M, G) |
| `hadoop fs -ls -t dossier` | trié par date (le plus récent en premier) |
| `hadoop fs -ls -S dossier` | trié par taille |
| `hadoop fs -du -h dossier` | taille de chaque élément du dossier |
| `hadoop fs -du -s -h dossier` | taille totale du dossier |
| `hadoop fs -df -h` | espace libre et utilisé dans HDFS |
| `hadoop fs -count dossier` | nb de dossiers, nb de fichiers, taille totale |
| `hadoop fs -stat "%n %b octets %y" fichier` | nom, taille, date de modif |
| `hadoop fs -find / -name "*.txt"` | cherche des fichiers par nom |
| `hadoop fs -test -e chemin ; echo $?` | `0` si le chemin existe, `1` sinon (`-d` dossier, `-f` fichier) |

Lecture d'une ligne de `-ls` :

```
-rw-r--r--   3 root supergroup     254130 2026-11-09 10:12 /user/root/notes.txt
 droits      │ propriétaire groupe  taille  date               chemin
             └ facteur de réplication (nb de copies ; `-` pour un dossier)
```

---

## 4. Créer, copier, déplacer, supprimer (dans HDFS)

| Commande | Ce que ça fait |
|---|---|
| `hadoop fs -mkdir dossier` | crée un dossier (le parent doit exister) |
| `hadoop fs -mkdir -p a/b/c` | crée toute l'arborescence d'un coup |
| `hadoop fs -touchz vide.txt` | crée un fichier vide |
| `hadoop fs -cp source destination` | copie **dans HDFS** |
| `hadoop fs -cp -f source destination` | copie en écrasant si ça existe |
| `hadoop fs -mv source destination` | déplace ou **renomme** dans HDFS |
| `hadoop fs -rm fichier` | supprime un fichier |
| `hadoop fs -rm -r dossier` | supprime un dossier et tout son contenu |
| `hadoop fs -rmdir dossier` | supprime un dossier **vide** |

- **Copier en renommant :** `hadoop fs -cp a/notes.txt b/copie.txt` crée la copie sous le nom `copie.txt`. Le dossier `b` doit exister.
- **Aucune corbeille :** sur ce cluster, la corbeille est désactivée (`fs.trash.interval = 0`). Un `-rm` est donc **définitif**.

---

## 5. Faire passer des fichiers entre ta machine, le conteneur et HDFS

```
            docker cp                       hadoop fs -put
Ta machine ───────────►  disque du namenode ──────────────►  HDFS
           ◄───────────                     ◄──────────────
            docker cp                       hadoop fs -get
```

### Étape A : ta machine ⇄ conteneur (à taper **sur ta machine**, pas dans le bash)

```bash
docker cp notes.txt namenode:/root/              # machine → conteneur
docker cp namenode:/root/resultat.txt .          # conteneur → machine (ici le "." = dossier courant, il est utile !)
docker cp mon-dossier namenode:/opt/mon-dossier  # un dossier entier
```

### Étape B : conteneur ⇄ HDFS (à taper **dans le bash du namenode**)

| Commande | Ce que ça fait |
|---|---|
| `hadoop fs -put notes.txt dossier/` | envoie un fichier local dans HDFS |
| `hadoop fs -put -f notes.txt dossier/` | idem, en écrasant s'il existe déjà |
| `hadoop fs -put notes.txt dossier/autre-nom.txt` | envoie en renommant |
| `hadoop fs -copyFromLocal notes.txt dossier/` | synonyme de `-put` |
| `hadoop fs -moveFromLocal notes.txt dossier/` | envoie puis **supprime** l'original local |
| `hadoop fs -get dossier/notes.txt .` | récupère un fichier HDFS dans le dossier courant |
| `hadoop fs -get dossier/notes.txt /root/copie.txt` | récupère en renommant |
| `hadoop fs -copyToLocal dossier/notes.txt .` | synonyme de `-get` |
| `hadoop fs -getmerge dossier resultat.txt` | fusionne tous les fichiers d'un dossier HDFS en un seul fichier local (pratique pour les `part-r-00000`, `part-r-00001`… d'un job) |
| `hadoop fs -appendToFile local.txt fichier-hdfs.txt` | ajoute le contenu d'un fichier local à la fin d'un fichier HDFS |

---

## 6. Lire un fichier HDFS

| Commande | Ce que ça fait |
|---|---|
| `hadoop fs -cat fichier` | affiche tout le fichier |
| `hadoop fs -head fichier` | affiche le **premier Ko** |
| `hadoop fs -tail fichier` | affiche le **dernier Ko** |
| `hadoop fs -tail -f fichier` | suit le fichier en direct (Ctrl+C pour arrêter) |
| `hadoop fs -text fichier.gz` | affiche en décompressant (gz, SequenceFile…) |

`-head` et `-tail` comptent en Ko, pas en lignes. Pour un nombre de lignes précis, on redirige la sortie de `-cat` vers les outils Linux :

```bash
hadoop fs -cat fichier | head -n 5      # 5 premières lignes
hadoop fs -cat fichier | tail -n 5      # 5 dernières lignes
hadoop fs -cat fichier | wc -l          # nombre de lignes
hadoop fs -cat fichier | grep "404"     # lignes contenant "404"
```

---

## 7. Droits et réplication

| Commande | Ce que ça fait |
|---|---|
| `hadoop fs -chmod 644 fichier` | change les droits (comme `chmod` Linux) |
| `hadoop fs -chmod -R 755 dossier` | récursivement |
| `hadoop fs -chown user:groupe fichier` | change le propriétaire |
| `hadoop fs -setrep 1 fichier` | change le nombre de copies d'un fichier |
| `hadoop fs -setrep -R 1 dossier` | idem pour tout un dossier |

- **Droits :** sur ce cluster, ils ne sont pas vérifiés (`dfs.permissions.enabled = false`). `chmod` change l'affichage mais ne bloque rien.
- **Réplication :** elle vaut 3 par défaut, mais il n'y a qu'**un seul datanode**. HDFS signale donc des blocs *under replicated*. C'est normal ici. `-setrep 1` fait disparaître l'alerte.

---

## 8. État du cluster (commande `hdfs`)

| Commande | Ce que ça fait |
|---|---|
| `hdfs dfsadmin -report` | capacité, espace utilisé, liste des datanodes vivants |
| `hdfs fsck /` | santé de HDFS (blocs manquants, corrompus…) |
| `hdfs fsck chemin -files -blocks -locations` | sur quels datanodes sont stockés les blocs d'un fichier |
| `hdfs dfsadmin -safemode get` | dit si HDFS est en lecture seule (*safe mode*) |
| `hdfs dfsadmin -safemode leave` | sort du safe mode (si HDFS refuse d'écrire juste après le démarrage) |
| `hdfs getconf -confKey dfs.replication` | lit une valeur de config |
| `hadoop version` | version de Hadoop |

---

## 9. Lancer des jobs MapReduce (YARN)

```bash
# Forme générale
hadoop jar mon-job.jar NomDeLaClasse entree sortie

# Exemple fourni avec Hadoop : compter les mots
hadoop jar /opt/hadoop-3.2.1/share/hadoop/mapreduce/hadoop-mapreduce-examples-3.2.1.jar \
    wordcount dossier-entree dossier-sortie

# Voir le résultat
hadoop fs -ls dossier-sortie            # contient _SUCCESS + part-r-00000…
hadoop fs -cat dossier-sortie/part-r-*
```

- **Dossier de sortie :** il ne doit **pas** exister avant de lancer le job. Sinon, supprime-le : `hadoop fs -rm -r dossier-sortie`.
- **Fichier `_SUCCESS` :** il est vide et signifie juste que le job a réussi.

| Commande | Ce que ça fait |
|---|---|
| `yarn application -list` | jobs en cours |
| `yarn application -list -appStates ALL` | tous les jobs, y compris terminés |
| `yarn application -kill application_XXX` | tue un job |
| `yarn logs -applicationId application_XXX` | logs d'un job (pour comprendre une erreur) |
| `yarn node -list` | nodemanagers disponibles |

---

## 10. Spark (bonus)

```bash
/opt/spark-3.1.2/bin/spark-shell     # console Scala   (quitter : :quit ou Ctrl+D)
/opt/spark-3.1.2/bin/pyspark         # console Python  (quitter : exit() ou Ctrl+D)
```

```scala
val lignes = sc.textFile("hdfs://namenode:9000/user/root/notes.txt")  // lire depuis HDFS
lignes.count()                                                        // nb de lignes
lignes.take(5).foreach(println)                                       // 5 premières lignes
val local = sc.textFile("file:///root/notes.txt")                    // lire le disque du conteneur
```

Dans le namenode, un chemin sans préfixe (`"notes.txt"`) pointe aussi vers HDFS, dans `/user/root`.

---

## 11. Erreurs fréquentes

| Message | Cause | Solution |
|---|---|---|
| `ls: '.': No such file or directory` | `/user/root` n'existe pas encore | `hadoop fs -mkdir -p /user/root` |
| `put: 'x': File exists` | le fichier est déjà dans HDFS | `hadoop fs -put -f …` |
| `rm: 'x': Is a directory` | c'est un dossier | `hadoop fs -rm -r …` |
| `mkdir: 'a/b': No such file or directory` | le parent n'existe pas | `hadoop fs -mkdir -p …` |
| `Output directory … already exists` | dossier de sortie d'un job déjà présent | `hadoop fs -rm -r dossier-sortie` |
| `Name node is in safe mode` | HDFS vient de démarrer | attendre un peu, ou `hdfs dfsadmin -safemode leave` |
| `must specify at least one container source` | mauvaise syntaxe de `docker cp` (`\`, ou ` .` en trop) | `docker cp source namenode:/destination` |
| `Permission denied` sur un script Spark | droits d'exécution perdus au dézippage | `chmod +x /opt/spark-3.1.2/bin/* /opt/spark-3.1.2/sbin/*` |
| `bash: hadoop: command not found` | tu es sur ta machine, pas dans le conteneur | `docker exec -it namenode bash` |
| Le fichier n'apparaît pas avec `ls` | `ls` ne voit pas HDFS | `hadoop fs -ls` |

---

## 12. Interface web

- **http://localhost:9870** : interface HDFS du namenode.
  - **Utilities → Browse the file system :** naviguer, envoyer, télécharger et supprimer des fichiers.
  - **Datanodes :** état des datanodes et espace utilisé.
- **Interface YARN (port 8088) :** elle n'est **pas** exposée dans ce `docker-compose.yml`. Elle n'est donc pas accessible depuis ton navigateur sans modifier le fichier.
