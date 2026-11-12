package ex4;

// Exercice 4 : créer l'index d'un mot passé en paramètre
// dans "The_adventures_of_Sherlock_Holmes".
// Attendu : burglar 1, murderer 5, bachelor 8, gentleman 45, innocent 17 entrée(s).
// Astuce du sujet : cat -n "fichier_source" > "fichier_resultat" pour numéroter les lignes.

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class Ex4Driver {

    public static void main(String[] args) throws Exception {

        // Vérification du nombre d'arguments.
        if (args.length != 3) {
            System.out.printf("Format de la ligne de commande Ex4Driver <rep source> <rep cible> <mot_a_indexer>\n");
            System.exit(-1);
        }

        // Instanciation et initialisation du job Hadoop.
        Job job = Job.getInstance();
        job.setJarByClass(Ex4Driver.class);
        job.setJobName("Index d'un mot");
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // Ajout du mot à indexer au contexte des jobs (lu dans le Mapper avec
        // context.getConfiguration().get("mot_a_indexer")).
        job.getConfiguration().set("mot_a_indexer", args[2]);

        job.setMapperClass(Ex4Mapper.class);
        job.setReducerClass(Ex4Reducer.class);

        // TODO : adapter les types à ce qu'émettent le mapper et le reducer.
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        boolean ok = job.waitForCompletion(true);
        System.exit(ok ? 0 : 1);
    }
}
