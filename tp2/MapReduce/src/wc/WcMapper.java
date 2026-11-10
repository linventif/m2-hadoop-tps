package wc;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class WcMapper extends Mapper<LongWritable, Text, // Ne pas modifier !
                                     Text, IntWritable>  // Couple (key, value) en sortie du mapper
{
    // key est l'offset, adresse relative depuis le début du fichier (Ne pas modifier !).
    // value est le texte à mapper (Ne pas modifier !).
    @Override
    public void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        // Transforme l'objet 'Text' en chaîne de caractères.
        String line = value.toString();

        // Découpe la chaîne de caractères.
        for (String word : line.split("\\W+"))
            if (word.length() > 0)
                // Ecriture du couple clé/valeur en sortie du mapper.
                context.write(new Text(word), new IntWritable(1));
    }
}
