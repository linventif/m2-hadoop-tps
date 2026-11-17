package ex5;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class Ex5Reducer extends Reducer<Text, Text,         // Couple (key, value) en sortie du mapper : ("x,y", id_client)
                                       Text, IntWritable>  // Couple (key, value) en sortie du reducer : ("x,y", nb clients)
{
    // reduce() est appelé une fois par couple, avec le client de chaque commande
    // qui contient ce couple (un même client peut apparaître plusieurs fois).
    @Override
    public void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        // Clients différents : un Set ignore les doublons.
        // value.toString() fait une copie : Hadoop réutilise le même objet Text à chaque tour.
        Set<String> clients = new HashSet<>();
        for (Text value : values) {
            clients.add(value.toString());
        }

        context.write(key, new IntWritable(clients.size()));
    }
}
