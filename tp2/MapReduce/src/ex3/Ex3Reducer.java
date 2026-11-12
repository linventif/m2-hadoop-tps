package ex3;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class Ex3Reducer extends Reducer<Text, IntWritable, Text, IntWritable>
{
    // reduce() est appelé une fois par clé distincte, avec toutes ses valeurs.
    @Override
    public void reduce(Text key, Iterable<IntWritable> values, Context context)
            throws IOException, InterruptedException {

        // sum all the values for this key
        int sum = 0;
        for (IntWritable value : values) {
            sum += value.get();
        }

        // write the key and the sum to the context
        context.write(key, new IntWritable(sum));
    }
}
