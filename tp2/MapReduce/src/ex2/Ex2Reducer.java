package ex2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Set<String> uniqueValues = new HashSet<>();
        for (Text value : values) {
            uniqueValues.add(value.toString());
        }

        // only keep words with more than 1 occurrence
        uniqueValues.removeIf(word -> Collections.frequency((Collection<?>) values, new Text(word)) <= 1);

        // sort by alphabetical order
        List<String> sortedValues = new ArrayList<>();
        for (String value : uniqueValues) {
            sortedValues.add(value.toString());
        }
        Collections.sort(sortedValues);

        // convert the set back to a string
        String sb = String.join(", ", sortedValues);

        // Ecriture du résultat
        context.write(key, new Text(sb));
    }
}
