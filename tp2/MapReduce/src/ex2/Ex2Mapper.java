package ex2;

import java.io.IOException;
import java.text.Normalizer;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class Ex2Mapper extends Mapper<LongWritable, Text, // Ne pas modifier !
                                     Text, Text>  // Couple (key, value) en sortie du mapper
{
    // key est l'offset, adresse relative depuis le début du fichier (Ne pas modifier !).
    // value est le texte à mapper (Ne pas modifier !).
    @Override
    public void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        // Transforme l'objet 'Text' en chaîne de caractères.
        // String line = value.toString();
        // Transforme l'objet 'Text' en chaîne de caractères, en minuscules et sans accents.
        String line = value.toString().toLowerCase();
        line = Normalizer.normalize(line, Normalizer.Form.NFD)  // é → e + ´
                        .replaceAll("\\p{M}", "");             // supprime les accents ´


        // Découpe la chaîne de caractères.
        for (String word : line.split("\\W+"))
            if (word.length() > 0) {
                // mot en minuscule
                word = word.toLowerCase();
                String keyWord = word.toLowerCase();
                // supression des caractères non alphabétiques
                keyWord = keyWord.replaceAll("[^a-zA-Z]", "");
                // triage des lettres du mot ana -> aan
                char[] chars = keyWord.toCharArray();
                java.util.Arrays.sort(chars);
                keyWord = new String(chars);

                // Ecriture du couple clé/valeur en sortie du mapper.
                context.write(new Text(keyWord), new Text(word));
            }
    }
}
