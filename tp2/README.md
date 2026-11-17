# TP2 – Programmation MapReduce en Java

> Master MIAGE – Ingénierie des Processus Métier
> Sujet : [M2 IPM 2 - TP2 MapReduce.pdf](M2%20IPM%202%20-%20TP2%20MapReduce.pdf)
> Prérequis : le cluster du [TP1](../tp1/README.md) doit tourner (`docker compose up -d` dans `Docker-Hadoop/`).

---

## Mise en place de Java

### Pourquoi Java 8 ?

Le cluster Hadoop (conteneurs Docker) tourne en **Java 8**. Une JVM ne sait lire que les fichiers `.class` de sa version ou d'une version plus ancienne. Un `.class` compilé pour Java 17 ou 26 fait donc planter le job avec `UnsupportedClassVersionError`.

Il n'est pas nécessaire d'installer un JDK 8. Un JDK récent sait produire du Java 8 avec l'option `--release 8` :

```bash
javac --release 8 ...      # produit des .class "version 52.0" = Java 8
```

JDK utilisé ici : OpenJDK 26. Il affiche des avertissements *source/target value 8 is obsolete*, que l'option `-Xlint:-options` masque. Ce ne sont pas des erreurs.

> **Alternative sans JDK sur la machine :** le namenode contient un `javac` 1.8. On peut y copier les sources et compiler directement dans le conteneur.

### Le projet `MapReduce`

```
tp2/MapReduce/
├── lib/                                   ← librairies fournies avec le sujet
│   ├── hadoop-common-3.2.1.jar            (Path, Text, IntWritable…)
│   └── hadoop-mapreduce-client-core-3.2.1.jar  (Job, Mapper, Reducer…)
├── src/
│   ├── ex1/                               ← un package par exercice
│   │   ├── Ex1Driver.java
│   │   ├── Ex1Mapper.java
│   │   └── Ex1Reducer.java
│   ├── ex2/ … ex5/                        (Ex2Driver, Ex2Mapper, Ex2Reducer…)
├── Makefile                               ← compile, envoie les données, lance les jobs, rapatrie les résultats
└── build/                                 ← généré (ignoré par git)
    ├── classes/exN/*.class
    └── exN.jar
```

### Données et résultats : `tp2/data/exN` → `tp2/out/exN`

La même arborescence existe en local et dans HDFS (chemins HDFS relatifs à `/user/root`) :

```
tp2/data/                                  local                        HDFS
├── ex1/shakespeare.txt                    tp2/data/exN/  ── put ──►   tp2/data/exN/   (entrée du job)
├── ex2/liste_mots.txt                     tp2/out/exN/   ◄── get ──   tp2/out/exN/    (résultat du job)
├── ex3/cloudera_log.txt
├── ex4/sherlock_numerote.txt              ← généré par `cat -n` depuis le fichier ci-dessous
└── The_Adventures_of_Sherlock_Holmes.txt  ← hors de ex4/ : Hadoop lit TOUS les fichiers du dossier d'entrée
tp2/out/
├── ex1/  ex2/  ex3/                       part-r-00000 + _SUCCESS
└── ex4/<mot>/                             un index par mot : burglar, murderer, bachelor, gentleman, innocent
```

Depuis `tp2/MapReduce/`, **une commande fait tout** : compilation, envoi des données, job, puis récupération du résultat dans `tp2/out/` :

```bash
make tout        # ex1 à ex4
make ex2         # un seul exercice
```

Étapes séparées, pour un exercice :

| Commande | Effet |
|---|---|
| `make PKG=ex2` | compile `src/ex2` en Java 8 → `build/ex2.jar` |
| `make upload PKG=ex2` | `tp2/data/ex2` → `/root/tp2/data/ex2` (namenode) → `tp2/data/ex2` (HDFS) |
| `make run PKG=ex2` | copie le jar dans le namenode, supprime `tp2/out/ex2` dans HDFS, lance le job |
| `make fetch PKG=ex2` | `tp2/out/ex2` (HDFS) → namenode → `tp2/out/ex2` (local) |
| `make result PKG=ex2` | 20 premières lignes de `tp2/out/ex2/part-r-00000` |

- **Nom du Driver :** il est déduit du package (`ex2` → `Ex2Driver`).
- **Arguments :** `ARGS=` ajoute des arguments au job, comme le mot à indexer de l'exercice 4 :
  ```bash
  make run fetch PKG=ex4 OUT=tp2/out/ex4/gentleman ARGS=gentleman
  ```
- **Fichiers de données non versionnés :** `cloudera_log.txt` (38 Mo) et le texte de Sherlock Holmes ne sont pas dans git. Il faut les placer à la main dans `tp2/data/`.

**Dans VS Code** (extension *Extension Pack for Java*), le fichier [`.vscode/settings.json`](../.vscode/settings.json) à la racine du dépôt indique où sont les sources et les jars. L'autocomplétion et le soulignement des erreurs marchent donc sans Maven ni Gradle :

```json
{
    "java.project.sourcePaths": ["tp2/MapReduce/src"],
    "java.project.referencedLibraries": ["tp2/MapReduce/lib/**/*.jar"]
}
```

Si VS Code ne trouve pas les classes Hadoop : palette de commandes (Ctrl+Shift+P) → *Java: Clean Java Language Server Workspace*.

> **Le PDF contient des caractères invisibles** (espaces de largeur nulle) dans le code, par exemple après `{` et `throws`. Le code copié-collé depuis le PDF ne compile pas. Les sources de `src/ex1/` sont propres.

---

## Exercice 1 – WordCount

Le principe : compter le nombre d'occurrences de chaque mot dans l'œuvre complète de Shakespeare.

```
                 MAP                       SHUFFLE & SORT                REDUCE
"to be or not"  ───►  (to,1) (be,1)    ───►  be  → [1, 1, …]      ───►  (be, 1234)
"to be"               (or,1) (not,1)         not → [1, …]               (not, 567)
                      (to,1) (be,1)          to  → [1, 1, …]            (to, 17656)
```

| Classe | Rôle |
|---|---|
| `Ex1Mapper` | reçoit une ligne et émet `(mot, 1)` pour chaque mot (découpage sur `\W+`) |
| `Ex1Reducer` | reçoit `(mot, [1, 1, 1, …])` et émet `(mot, somme)` |
| `Ex1Driver` | configure le job (entrée, sortie, classes, types) et le lance |

Types Hadoop utilisés : `LongWritable` (≈ `long`), `Text` (≈ `String`), `IntWritable` (≈ `int`).

### 1. Mettre Shakespeare dans HDFS

Le sujet nomme le dossier HDFS `gutenberg`. Ici, on suit l'arborescence commune `tp2/data/ex1`. C'est ce que fait `make upload PKG=ex1`.

Sur la machine hôte :

```bash
docker exec namenode mkdir -p /root/tp2/data
docker cp tp2/data/ex1 namenode:/root/tp2/data/ex1
```

Dans le namenode :

```bash
hadoop fs -mkdir -p tp2/data
hadoop fs -put /root/tp2/data/ex1 tp2/data/ex1
hadoop fs -ls tp2/data/ex1
# -rw-r--r--   3 root supergroup    5856576 ... tp2/data/ex1/shakespeare.txt
```

### 2 à 4. Projet Java, package `ex1`, driver/mapper/reducer

> Le sujet nomme le package `wc` (classes `WcDriver`…). Ici, chaque exercice a son package `exN` (classes `ExNDriver`, `ExNMapper`, `ExNReducer`) pour séparer les exercices dans le même projet.

Voir [`MapReduce/src/ex1/`](MapReduce/src/ex1/).

### 5. Compiler en Java 8 et créer le jar

Depuis `tp2/MapReduce/` :

```bash
javac --release 8 -Xlint:-options -cp "lib/*" -d build/classes src/ex1/*.java
jar cvf build/ex1.jar -C build/classes ex1
```

- **`-cp "lib/*"` :** ajoute les deux jars Hadoop au classpath pour la compilation.
- **`-d build/classes` :** les `.class` sont rangés dans `build/classes/ex1/`, car le package est `ex1`.
- **`jar cvf build/ex1.jar -C build/classes ex1` :** équivaut au `jar cvf wc.jar wc` du sujet. `-C` dit à `jar` de se placer dans `build/classes` avant d'ajouter le dossier `ex1`.

Pour vérifier :

```bash
file build/classes/ex1/Ex1Driver.class   # → compiled Java class data, version 52.0 (Java 1.8)
```

### 6. Copier le jar sur le namenode

```bash
docker cp build/ex1.jar namenode:/root/
```

### 7. Lancer le job

Dans le namenode :

```bash
hadoop jar /root/ex1.jar ex1.Ex1Driver tp2/data/ex1 tp2/out/ex1
```

- **`ex1.Ex1Driver` :** il faut le nom **complet** de la classe principale (package + classe), parce que le jar n'a pas de `Main-Class` dans son manifeste.
- **`tp2/data/ex1` / `tp2/out/ex1` :** dossiers HDFS d'entrée et de sortie, relatifs à `/user/root` (`gutenberg` et `wordcounts` dans le sujet).
- **Avertissement `Implement the Tool interface…` :** il est normal, le code du sujet n'utilise pas `ToolRunner`.
- **Dossier de sortie :** `tp2/out/ex1` ne doit **pas** exister. Avant de relancer : `hadoop fs -rm -r tp2/out/ex1` (`make run` le fait automatiquement).

Compteurs intéressants affichés à la fin du job :

| Compteur | Valeur | Signification |
|---|---|---|
| `Map input records` | 147 838 | lignes lues |
| `Map output records` | 990 512 | couples `(mot, 1)` émis, soit le nombre total de mots |
| `Reduce input groups` | 32 923 | mots distincts |
| `Reduce output records` | 32 923 | lignes du résultat |
| `Launched map tasks` | 1 | fichier de 5,6 Mo < 128 Mo, donc un seul bloc et un seul mapper |

### 8. Vérifier le résultat

```bash
hadoop fs -ls tp2/out/ex1
# _SUCCESS          ← fichier vide : le job a réussi
# part-r-00000      ← résultat du reducer n°0

hadoop fs -cat tp2/out/ex1/part-r-00000 | head -n 20                     # début (ordre alphabétique)
hadoop fs -cat tp2/out/ex1/part-r-00000 | grep -P "^love\t"              # un mot précis
hadoop fs -cat tp2/out/ex1/part-r-00000 | sort -t$'\t' -k2 -nr | head    # les 10 mots les plus fréquents
```

Résultats obtenus :

| Mot | Occurrences |
|---|---|
| the | 25 419 |
| I | 23 496 |
| and | 20 195 |
| to | 17 656 |
| of | 16 912 |
| love | 2 279 |
| death | 933 |
| Romeo | 148 |
| Hamlet | 110 |
| Juliet | 72 |

**Limites du découpage `split("\\W+")`**, utiles à commenter :
- **Casse :** `the` et `The` sont comptés séparément.
- **Mots coupés :** en Java, `\W` ne connaît que les lettres ASCII. Les apostrophes (`I'd`, `’s`) et les lettres accentuées coupent donc les mots. C'est pour ça que `d` arrive 10ᵉ du classement.
- **Tri :** la sortie est triée par clé (ordre alphabétique, chiffres et majuscules d'abord), pas par nombre d'occurrences.

### Avec le Makefile (raccourci)

Depuis `tp2/MapReduce/` :

```bash
make ex1                 # étapes 1 et 5 à 8 : upload + compile + job + résultat dans tp2/out/ex1/
make result PKG=ex1      # 20 premières lignes du résultat
```

---

## Résultats des exercices 2 à 4

Obtenus avec `make tout` (fichiers dans `tp2/out/`).

| Exercice | Sortie | Résultat |
|---|---|---|
| 2 – Anagrammes | `out/ex2/part-r-00000` | 17 groupes, par ex. `acenr  ancre, caner, carne, cerna, crane, ecran, encra, nacre, rance` |
| 3 – Hits par quart d'heure | `out/ex3/part-r-00000` | 52 quarts d'heure de `10:30` à `23:15` inclus, 172 757 hits (lignes hors plage et mal formées ignorées) |
| 4 – Index | `out/ex4/<mot>/part-r-00000` | burglar 1, murderer 5, bachelor 8, gentleman 45, innocent 17 entrées, **identique au sujet** |

Choix de conception :
- **Exercice 2 :** texte mis en minuscules et sans accents (`écran` → `ecran`). Les doublons sont supprimés et les groupes d'un seul mot sont ignorés.
- **Exercice 3 :** clé `jj/Mois/aaaa:HH:mm` au début du quart d'heure. Le quart qui commence à 23h15 est inclus. Un combiner fait des sommes partielles avant le shuffle.
- **Exercice 4 :** lignes numérotées avec `cat -n`. Le mot à indexer est transmis via `job.getConfiguration()` **avant** `waitForCompletion`. Une entrée correspond à une ligne contenant le mot.
