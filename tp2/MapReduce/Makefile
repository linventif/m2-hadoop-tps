# Compilation et lancement des jobs MapReduce du TP2.
#
# Même arborescence en local et dans HDFS (chemins HDFS relatifs à /user/root) :
#   local  tp2/data/exN/  ── docker cp + hadoop fs -put ──►  HDFS  tp2/data/exN/   (entrée)
#   local  tp2/out/exN/   ◄── hadoop fs -get + docker cp ──  HDFS  tp2/out/exN/    (résultat)
#
# Tout lancer (depuis tp2/MapReduce/) :
#   make tout            ex1 à ex5 : envoi des données, job, récupération des résultats
#   make ex1             un seul exercice (idem ex2, ex3, ex4, ex5)
#
# Étapes séparées, pour un exercice (PKG=ex1 par défaut) :
#   make                 compile src/$(PKG) en Java 8 → build/$(PKG).jar
#   make upload          envoie tp2/data/$(PKG) dans HDFS (remplace l'existant)
#   make run             copie le jar dans le namenode et lance le job (supprime d'abord $(OUT))
#   make fetch           rapatrie $(OUT) de HDFS vers tp2/out/$(PKG)
#   make result          affiche les 20 premières lignes du résultat local
#   make clean           supprime build/
#
# Exemples :
#   make upload run fetch PKG=ex2
#   make run fetch PKG=ex4 OUT=tp2/out/ex4/gentleman ARGS=gentleman

PKG    ?= ex1
DRIVER ?= $(shell echo $(PKG) | sed 's/^./\U&/')Driver
IN     ?= tp2/data/$(PKG)
OUT    ?= tp2/out/$(PKG)
ARGS   ?=

# Exercice 4 : mots à indexer (entrées attendues d'après le sujet : 1, 5, 8, 45, 17).
MOTS_EX4 := burglar murderer bachelor gentleman innocent
SHERLOCK := ../data/The_Adventures_of_Sherlock_Holmes.txt

CONTAINER := namenode
JAR       := build/$(PKG).jar
SRC       := $(wildcard src/$(PKG)/*.java)
LOCAL_OUT := ../$(patsubst tp2/%,%,$(OUT))
# hadoop fs sans les lignes INFO (SASL…) ; hadoop jar garde ses logs de progression.
HDFS      := docker exec -e HADOOP_ROOT_LOGGER=WARN,console $(CONTAINER) hadoop fs

.PHONY: all upload deploy run fetch result clean tout ex1 ex2 ex3 ex4 ex5

all: $(JAR)

# --release 8 : le cluster tourne en Java 8, les .class doivent être en version 52.
$(JAR): $(SRC)
	rm -rf build/classes/$(PKG)
	javac --release 8 -Xlint:-options -cp "lib/*" -d build/classes $(SRC)
	jar cf $(JAR) -C build/classes $(PKG)

# tp2/data/$(PKG) (local) → /root/tp2/data/$(PKG) (namenode) → tp2/data/$(PKG) (HDFS)
upload:
	docker exec $(CONTAINER) rm -rf /root/tp2/data/$(PKG)
	docker exec $(CONTAINER) mkdir -p /root/tp2/data
	docker cp ../data/$(PKG) $(CONTAINER):/root/tp2/data/$(PKG)
	$(HDFS) -mkdir -p tp2/data
	$(HDFS) -rm -r -f tp2/data/$(PKG)
	$(HDFS) -put /root/tp2/data/$(PKG) tp2/data/$(PKG)
	$(HDFS) -ls tp2/data/$(PKG)

deploy: $(JAR)
	docker cp $(JAR) $(CONTAINER):/root/

run: deploy
	$(HDFS) -rm -r -f $(OUT)
	docker exec $(CONTAINER) hadoop jar /root/$(PKG).jar $(PKG).$(DRIVER) $(IN) $(OUT) $(ARGS)

# $(OUT) (HDFS) → /root/$(OUT) (namenode) → tp2/out/… (local)
fetch:
	docker exec $(CONTAINER) rm -rf /root/$(OUT)
	docker exec $(CONTAINER) mkdir -p /root/$(dir $(OUT))
	$(HDFS) -get $(OUT) /root/$(OUT)
	rm -rf $(LOCAL_OUT)
	mkdir -p $(dir $(LOCAL_OUT))
	docker cp $(CONTAINER):/root/$(OUT) $(LOCAL_OUT)
	@echo "→ résultat dans $(LOCAL_OUT)/"

result:
	head -n 20 $(LOCAL_OUT)/part-r-00000

clean:
	rm -rf build

# ─── Tout lancer ───────────────────────────────────────────────────────────

tout: ex1 ex2 ex3 ex4 ex5

ex1 ex2 ex3:
	$(MAKE) upload run fetch PKG=$@

# Le sujet conseille de numéroter les lignes avec cat -n avant de les indexer.
../data/ex4/sherlock_numerote.txt: $(SHERLOCK)
	mkdir -p $(@D)
	cat -n $< > $@

ex4: ../data/ex4/sherlock_numerote.txt
	$(MAKE) upload PKG=ex4
	for mot in $(MOTS_EX4); do \
	  $(MAKE) run fetch PKG=ex4 OUT=tp2/out/ex4/$$mot ARGS=$$mot || exit 1; \
	done
	@echo "Entrées de l'index (attendu : burglar 1, murderer 5, bachelor 8, gentleman 45, innocent 17) :"
	@for mot in $(MOTS_EX4); do \
	  printf '  %-10s %s\n' $$mot $$(cut -f2 ../out/ex4/$$mot/part-r-00000 | tr ',' '\n' | grep -c .); \
	done

# Exercice 5 : le sujet ne fournit pas de fichier, on le génère (graine fixe, toujours identique).
../data/ex5/commandes.txt: ../scripts/generer_commandes.py
	mkdir -p $(@D)
	python3 -I $< $@ 2000

ex5: ../data/ex5/commandes.txt
	$(MAKE) upload run fetch PKG=ex5
