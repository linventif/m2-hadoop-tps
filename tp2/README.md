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
│   └── wc/                                ← package "wc"
│       ├── WcDriver.java
│       ├── WcMapper.java
│       └── WcReducer.java
├── Makefile                               ← compile, crée le jar, lance le job
└── build/                                 ← généré (ignoré par git)
    ├── classes/wc/*.class
    └── wc.jar
```

**Dans VS Code** (extension *Extension Pack for Java*), le fichier [`.vscode/settings.json`](../.vscode/settings.json) à la racine du dépôt indique où sont les sources et les jars. L'autocomplétion et le soulignement des erreurs marchent donc sans Maven ni Gradle :

```json
{
    "java.project.sourcePaths": ["tp2/MapReduce/src"],
    "java.project.referencedLibraries": ["tp2/MapReduce/lib/**/*.jar"]
}
```

Si VS Code ne trouve pas les classes Hadoop : palette de commandes (Ctrl+Shift+P) → *Java: Clean Java Language Server Workspace*.

> **Le PDF contient des caractères invisibles** (espaces de largeur nulle) dans le code, par exemple après `{` et `throws`. Le code copié-collé depuis le PDF ne compile pas. Les sources de `src/wc/` sont propres.

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
| `WcMapper` | reçoit une ligne et émet `(mot, 1)` pour chaque mot (découpage sur `\W+`) |
| `WcReducer` | reçoit `(mot, [1, 1, 1, …])` et émet `(mot, somme)` |
| `WcDriver` | configure le job (entrée, sortie, classes, types) et le lance |

Types Hadoop utilisés : `LongWritable` (≈ `long`), `Text` (≈ `String`), `IntWritable` (≈ `int`).

### 1. Mettre Shakespeare dans HDFS (`gutenberg`)

Sur la machine hôte :

```bash
docker cp tp2/shakespeare.txt namenode:/root/
```

Dans le namenode :

```bash
hadoop fs -mkdir -p gutenberg
hadoop fs -put /root/shakespeare.txt gutenberg/
hadoop fs -ls gutenberg
# -rw-r--r--   3 root supergroup    5856576 ... gutenberg/shakespeare.txt
```

### 2 à 4. Projet Java, package `wc`, driver/mapper/reducer

Voir [`MapReduce/src/wc/`](MapReduce/src/wc/).

### 5. Compiler en Java 8 et créer le jar

Depuis `tp2/MapReduce/` :

```bash
javac --release 8 -Xlint:-options -cp "lib/*" -d build/classes src/wc/*.java
jar cvf build/wc.jar -C build/classes wc
```

- **`-cp "lib/*"` :** ajoute les deux jars Hadoop au classpath pour la compilation.
- **`-d build/classes` :** les `.class` sont rangés dans `build/classes/wc/`, car le package est `wc`.
- **`jar cvf build/wc.jar -C build/classes wc` :** équivaut au `jar cvf wc.jar wc` du sujet. `-C` dit à `jar` de se placer dans `build/classes` avant d'ajouter le dossier `wc`.

Pour vérifier :

```bash
file build/classes/wc/WcDriver.class   # → compiled Java class data, version 52.0 (Java 1.8)
```

### 6. Copier le jar sur le namenode

```bash
docker cp build/wc.jar namenode:/root/
```

### 7. Lancer le job

Dans le namenode :

```bash
hadoop jar /root/wc.jar wc.WcDriver gutenberg wordcounts
```

- **`wc.WcDriver` :** il faut le nom **complet** de la classe principale (package + classe), parce que le jar n'a pas de `Main-Class` dans son manifeste.
- **`gutenberg` / `wordcounts` :** dossiers HDFS d'entrée et de sortie, relatifs à `/user/root`.
- **Avertissement `Implement the Tool interface…` :** il est normal, le code du sujet n'utilise pas `ToolRunner`.
- **Dossier de sortie :** `wordcounts` ne doit **pas** exister. Avant de relancer : `hadoop fs -rm -r wordcounts`.

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
hadoop fs -ls wordcounts
# _SUCCESS          ← fichier vide : le job a réussi
# part-r-00000      ← résultat du reducer n°0

hadoop fs -cat wordcounts/part-r-00000 | head -n 20                     # début (ordre alphabétique)
hadoop fs -cat wordcounts/part-r-00000 | grep -P "^love\t"              # un mot précis
hadoop fs -cat wordcounts/part-r-00000 | sort -t$'\t' -k2 -nr | head    # les 10 mots les plus fréquents
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
make            # étape 5 : compile + crée build/wc.jar
make run        # étapes 6 et 7 : docker cp + hadoop jar (supprime d'abord wordcounts dans HDFS)
make result     # étape 8 : 20 premières lignes de part-r-00000
```

Les variables permettent de réutiliser le Makefile pour les exercices suivants, par exemple :

```bash
make run PKG=index DRIVER=IndexDriver IN=sherlock OUT=index ARGS=gentleman
```
