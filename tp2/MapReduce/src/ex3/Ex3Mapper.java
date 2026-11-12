package ex3;

import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class Ex3Mapper extends Mapper<LongWritable, Text, // Ne pas modifier !
                                     Text, Text>          // TODO : couple (key, value) en sortie du mapper
{
    // key est l'offset depuis le début du fichier, value est une ligne du fichier.
    @Override
    public void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        String line = value.toString();

        // TODO : extraire la clé et la valeur de la ligne, puis context.write(...)
    }
}
