package ex4;

// Exercice 4 : créer l'index d'un mot passé en paramètre
// dans "The_adventures_of_Sherlock_Holmes" (lignes numérotées avec cat -n).
// Attendu : burglar 1, murderer 5, bachelor 8, gentleman 45, innocent 17 entrée(s).

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class Ex4Driver {

    public static void main(String[] args) throws Exception {

        // Vérification de la présence des 3 arguments de la fonction.
        if (args.length != 3) {
            System.out.printf("Format de la ligne de commande Ex4Driver <rep source> <rep cible> <mot_a_indexer>\n");
            System.exit(-1);
        }

        // Instanciation et initialisation du job Hadoop.
        Job job = Job.getInstance();
        job.setJarByClass(Ex4Driver.class); // Classe de lancement.
        job.setJobName("Index de " + args[2]); // Nom du job.
        FileInputFormat.setInputPaths(job, new Path(args[0])); // Chemin du répertoire d'entrée.
        FileOutputFormat.setOutputPath(job, new Path(args[1])); // Chemin du répertoire de sortie.

        // Ajout du mot à indexer au contexte des jobs, AVANT le lancement :
        // la configuration est envoyée aux mappers au moment de la soumission du job.
        job.getConfiguration().set("mot_a_indexer", args[2]);

        // Indication au job des classes à exécuter pour le mapper et le reducer.
        job.setMapperClass(Ex4Mapper.class);
        job.setReducerClass(Ex4Reducer.class);

        // Indication des types pour le couple (key, value) en sortie du mapper.
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(LongWritable.class);

        // Indication des types pour le couple (key, value) en sortie du reducer.
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        // Lancement du job.
        boolean ok = job.waitForCompletion(true);
        System.exit(ok ? 0 : 1);
    }
}
