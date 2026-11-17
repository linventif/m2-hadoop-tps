package ex4;

import java.io.IOException;
import java.text.Normalizer;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class Ex4Mapper extends Mapper<LongWritable, Text, // Ne pas modifier !
        Text, LongWritable> // Couple (key, value) en sortie du mapper : (mot, n° de ligne)
{
    // key est l'offset en octets (pas le n° de ligne), value est une ligne du fichier
    // numéroté avec `cat -n` : "     42\tTexte de la ligne".
    @Override
    public void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        // Lecture du mot à indexer (une fois par ligne, pas une fois par mot),
        // mis en minuscules et sans accents comme le texte.
        String mot = normalize(context.getConfiguration().get("mot_a_indexer", ""));
        if (mot.isEmpty()) {
            return;
        }

        String line = value.toString();

        // get line number added with `cat -n` command: "     42\t..." -> 42
        int tab = line.indexOf('\t');
        if (tab < 0) {
            return; // ligne non numérotée : ignorée
        }
        long lineNumber;
        try {
            lineNumber = Long.parseLong(line.substring(0, tab).trim());
        } catch (NumberFormatException e) {
            return;
        }

        // Découpe uniquement le texte de la ligne (sans le numéro).
        String text = normalize(line.substring(tab + 1));
        for (String word : text.split("\\W+")) {
            // Si le mot est celui à indexer, on émet (mot, n° de ligne).
            if (word.equals(mot)) {
                context.write(new Text(word), new LongWritable(lineNumber));
            }
        }
    }

    // minuscules + suppression des accents (é -> e)
    private static String normalize(String s) {
        return Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
