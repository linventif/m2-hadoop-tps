package ex5;

// Exercice 5 : fichier de logs au format id_cde;id_client;id_prod_1;id_prod_2;...;id_prod_n
// Pour tout couple de produits (x, y), combien de fois a-t-il été commandé
// par des clients différents ?

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class Ex5Driver {

    public static void main(String[] args) throws Exception {

        // Vérification du nombre d'arguments.
        if (args.length != 2) {
            System.out.printf("Format de la ligne de commande Ex5Driver <rep source> <rep cible>\n");
            System.exit(-1);
        }

        // Instanciation et initialisation du job Hadoop.
        Job job = Job.getInstance();
        job.setJarByClass(Ex5Driver.class);
        job.setJobName("Couples de produits");
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        job.setMapperClass(Ex5Mapper.class);
        job.setReducerClass(Ex5Reducer.class);
        // Pas de job.setCombinerClass(Ex5Reducer.class) : le reducer compte des clients
        // DISTINCTS. Additionner des comptes partiels compterait deux fois un client
        // vu par deux mappers, et les types de sortie (IntWritable) ne sont pas ceux
        // de l'entrée du reducer (Text).

        // Sortie du mapper : (couple "x,y", id_client).
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        // Sortie du reducer : (couple "x,y", nombre de clients différents).
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        boolean ok = job.waitForCompletion(true);
        System.exit(ok ? 0 : 1);
    }
}
