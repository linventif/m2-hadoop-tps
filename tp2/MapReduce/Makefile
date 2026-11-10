# Compilation et lancement des jobs MapReduce du TP2.
#
#   make                 compile src/$(PKG) en Java 8 et crée build/$(PKG).jar
#   make deploy          copie le jar dans le namenode (/root)
#   make run             deploy + lance le job (supprime d'abord $(OUT) dans HDFS)
#   make result          affiche les 20 premières lignes du résultat
#   make clean           supprime build/
#
# Par défaut : exercice 1 (WordCount). Pour un autre exercice, surcharger les variables :
#   make run PKG=index DRIVER=IndexDriver IN=sherlock OUT=index ARGS=gentleman

PKG    ?= wc
DRIVER ?= WcDriver
IN     ?= gutenberg
OUT    ?= wordcounts
ARGS   ?=

CONTAINER := namenode
JAR       := build/$(PKG).jar
SRC       := $(wildcard src/$(PKG)/*.java)

.PHONY: all deploy run result clean

all: $(JAR)

# --release 8 : le cluster tourne en Java 8, les .class doivent être en version 52.
$(JAR): $(SRC)
	rm -rf build/classes/$(PKG)
	javac --release 8 -Xlint:-options -cp "lib/*" -d build/classes $(SRC)
	jar cvf $(JAR) -C build/classes $(PKG)

deploy: $(JAR)
	docker cp $(JAR) $(CONTAINER):/root/

run: deploy
	docker exec $(CONTAINER) hadoop fs -rm -r -f $(OUT)
	docker exec $(CONTAINER) hadoop jar /root/$(PKG).jar $(PKG).$(DRIVER) $(IN) $(OUT) $(ARGS)

result:
	docker exec $(CONTAINER) hadoop fs -cat $(OUT)/part-r-00000 | head -n 20

clean:
	rm -rf build
