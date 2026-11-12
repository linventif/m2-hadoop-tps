package ex1;

import java.io.IOException;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class Ex1Reducer extends Reducer<Text, IntWritable,  // Couple (key, value) en sortie du mapper
                                       Text, IntWritable>  // Couple (key, value) en sortie du reducer
{
    // key est la clé extraite du mapper.
    // values est la liste des valeurs issues des mappers pour chaque occurrence de key.
    @Override
    public void reduce(Text key, Iterable<IntWritable> values, Context context)
            throws IOException, InterruptedException {

        int wordcount = 0;

        // On compte le nombre de valeurs.
        for (IntWritable value : values)
            wordcount = wordcount + value.get();

        // Ecriture du résultat
        context.write(key, new IntWritable(wordcount));
    }
}
