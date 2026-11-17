package ex2;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class Ex2Reducer extends Reducer<Text, Text,  // Couple (key, value) en sortie du mapper
                                       Text, Text>  // Couple (key, value) en sortie du reducer
{
    // key est la clé extraite du mapper (les lettres du mot, triées).
    // values est la liste des mots qui ont ces lettres (avec doublons).
    // values ne se parcourt qu'une fois et n'est pas une Collection.
    @Override
    public void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        // remove duplicates and sort by alphabetical order (TreeSet does both).
        // value.toString() makes a copy: Hadoop reuses the same Text object at each iteration.
        Set<String> uniqueValues = new TreeSet<>();
        for (Text value : values) {
            uniqueValues.add(value.toString());
        }

        // a group with a single word is not a list of anagrams
        if (uniqueValues.size() < 2) {
            return;
        }

        // Ecriture du résultat : "aeimr    aimer, maire, marie, ramie"
        context.write(key, new Text(String.join(", ", uniqueValues)));
    }
}
