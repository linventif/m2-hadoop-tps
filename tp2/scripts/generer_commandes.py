#!/usr/bin/env python3
"""Génère le jeu de données de l'exercice 5 (le sujet n'en fournit pas).

Format d'une ligne, comme dans le sujet :
    id_cde;id_client;id_prod_1;id_prod_2;...;id_prod_n

Usage : python3 generer_commandes.py <fichier_sortie> [nb_commandes]

Graine fixe : le fichier généré est toujours le même.
Quelques cas particuliers sont ajoutés exprès pour tester le programme :
  - un même client qui commande plusieurs fois le même couple (compté une seule fois) ;
  - un produit en double dans une commande (ne forme pas de couple avec lui-même) ;
  - des commandes d'un seul produit (aucun couple) ;
  - une ligne vide et une ligne mal formée (doivent être ignorées).
"""
import random
import sys

NB_CLIENTS = 150
NB_PRODUITS = 30

def main():
    sortie = sys.argv[1]
    nb = int(sys.argv[2]) if len(sys.argv) > 2 else 2000
    rnd = random.Random(42)
    produits = [f"P{i:02d}" for i in range(1, NB_PRODUITS + 1)]
    # quelques produits souvent achetés ensemble, pour avoir des couples fréquents
    paniers_types = [["P01", "P02"], ["P05", "P06", "P07"], ["P10", "P11"]]

    lignes = []
    for n in range(1, nb + 1):
        client = f"CL{rnd.randint(1, NB_CLIENTS):03d}"
        k = rnd.choice([1, 2, 2, 3, 3, 4, 5])
        panier = rnd.sample(produits, k)
        if rnd.random() < 0.3:
            panier += rnd.choice(paniers_types)
        rnd.shuffle(panier)
        lignes.append(";".join([f"C{n:05d}", client] + panier))

    # cas particuliers
    lignes += [
        "C90001;CL001;P29;P30",
        "C90002;CL001;P30;P29",        # même client, même couple, ordre inversé
        "C90003;CL002;P29;P29;P30",    # produit en double dans la commande
        "C90004;CL003;P15",            # un seul produit : aucun couple
        "",                            # ligne vide
        "ligne mal formee",            # pas de ';'
    ]
    with open(sortie, "w", encoding="utf-8") as f:
        f.write("\n".join(lignes) + "\n")

if __name__ == "__main__":
    main()
