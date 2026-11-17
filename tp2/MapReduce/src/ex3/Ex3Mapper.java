package ex3;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class Ex3Mapper extends Mapper<LongWritable, Text, // Ne pas modifier !
        Text, IntWritable>
{
    // key est l'offset depuis le début du fichier, value est une ligne du fichier.
    @Override
    public void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        String line = value.toString();

        // 217.89.36.129 - - [14/Jun/2014:10:30:14 -0400] xxxx

        // extract date and hour from the log line
        String[] parts = line.split(" ");

        // skip malformed lines (empty, truncated…) instead of crashing the whole job:
        // parts[3] must exist and look like "[dd/MMM/yyyy:HH:mm:ss"
        if (parts.length < 4 || !parts[3].startsWith("[")) {
            context.getCounter("Ex3", "lignes ignorees").increment(1);
            return;
        }
        String dateTime = parts[3].substring(1); // remove the leading '['
        String[] dateParts = dateTime.split(":"); // [dd/MMM/yyyy, HH, mm, ss]
        if (dateParts.length < 3) {
            context.getCounter("Ex3", "lignes ignorees").increment(1);
            return;
        }
        String date = dateParts[0]; // extract the date part

        // start of the quarter hour (truncated, not rounded): 10:44 -> 10:30
        String hour = dateParts[1]; // extract the hour part
        String minute = dateParts[2]; // extract the minute part
        int roundedMinute;
        try {
            roundedMinute = (Integer.parseInt(minute) / 15) * 15;
        } catch (NumberFormatException e) {
            context.getCounter("Ex3", "lignes ignorees").increment(1);
            return;
        }
        String roundedTime = hour + ":" + String.format("%02d", roundedMinute);

        // skip if time is 10:30 or after 23:15
        if (hour.equals("10") && roundedMinute < 30) {
            return;
        }
        if (hour.equals("23") && roundedMinute > 15) {
            return;
        }

        // write the key-value pair : dd/MMM/yyyy:HH:mm, 1
        context.write(new Text(date + ":" + roundedTime), new IntWritable(1));
    }
}
