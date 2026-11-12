package ex3;

import java.io.IOException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class Ex3Reducer extends Reducer<Text, Text,  // TODO : couple (key, value) en sortie du mapper
                                       Text, Text>  // TODO : couple (key, value) en sortie du reducer
{
    // reduce() est appelé une fois par clé distincte, avec toutes ses valeurs.
    @Override
    public void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        // TODO : calculer le résultat pour cette clé, puis context.write(...)
    }
}
