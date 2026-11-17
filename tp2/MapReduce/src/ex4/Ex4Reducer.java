package ex4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class Ex4Reducer extends Reducer<Text, LongWritable, // Couple (key, value) en sortie du mapper
        Text, Text> // Couple (key, value) en sortie du reducer : (mot, "12,87,154,...")
{
    // key est le mot indexé, values les numéros des lignes où il apparaît
    // (un même numéro plusieurs fois si le mot est plusieurs fois sur la ligne).
    @Override
    public void reduce(Text key, Iterable<LongWritable> values, Context context)
            throws IOException, InterruptedException {

        // remove duplicates and sort by line number (TreeSet does both).
        Set<Long> lines = new TreeSet<>();
        for (LongWritable value : values) {
            lines.add(value.get());
        }

        // convert the line numbers to a comma-separated string
        List<String> parts = new ArrayList<>();
        for (Long line : lines) {
            parts.add(String.valueOf(line));
        }

        // Ecriture du résultat
        context.write(key, new Text(String.join(",", parts)));
    }
}
