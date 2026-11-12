package ex3;

// Exercice 3 : calculer le nombre de hits (requêtes HTTP) par quart d'heure
// entre 10h30 et 23h15 à partir des logs d'un site.
// Entrée : cloudera_log.txt dans le répertoire HDFS "logs".

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class Ex3Driver {

    public static void main(String[] args) throws Exception {

        // Vérification du nombre d'arguments.
        if (args.length != 2) {
            System.out.printf("Format de la ligne de commande Ex3Driver <rep source> <rep cible>\n");
            System.exit(-1);
        }

        // Instanciation et initialisation du job Hadoop.
        Job job = Job.getInstance();
        job.setJarByClass(Ex3Driver.class);
        job.setJobName("Hits par quart d'heure");
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        job.setMapperClass(Ex3Mapper.class);
        job.setReducerClass(Ex3Reducer.class);

        // TODO : adapter les types à ce qu'émettent le mapper et le reducer.
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        boolean ok = job.waitForCompletion(true);
        System.exit(ok ? 0 : 1);
    }
}
