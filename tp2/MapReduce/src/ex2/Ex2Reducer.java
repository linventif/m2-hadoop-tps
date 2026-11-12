package ex2;

import java.io.IOException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class Ex2Reducer extends Reducer<Text, Text,  // Couple (key, value) en sortie du mapper
                                       Text, Text>  // Couple (key, value) en sortie du reducer
{
    // key est la clé extraite du mapper.
    // values est la liste des valeurs issues des mappers pour chaque occurrence de key.
    @Override
    public void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        // remove duplicates
        java.util.Set<String> uniqueValues = new java.util.HashSet<>();
        for (Text value : values) {
            uniqueValues.add(value.toString());
        }

        // sort by alphabetical order
        java.util.List<String> sortedValues = new java.util.ArrayList<>();
        for (String value : uniqueValues) {
            sortedValues.add(value.toString());
        }
        java.util.Collections.sort(sortedValues);

        // convert the set back to a string
        String sb = String.join(", ", sortedValues);

        // Ecriture du résultat
        context.write(key, new Text(sb));
    }
}
