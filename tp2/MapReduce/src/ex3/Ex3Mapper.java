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
        String dateTime = parts[3].substring(1); // remove the leading '['
        String date = dateTime.split(":")[0]; // extract the date part

        // round it to the nearest 15 minutes
        String hour = dateTime.split(":")[1]; // extract the hour part
        String minute = dateTime.split(":")[2]; // extract the minute part
        int roundedMinute = (Integer.parseInt(minute) / 15) * 15;
        String roundedTime = hour + ":" + String.format("%02d", roundedMinute);

        // write the key-value pair : dd/MMM/yyyy:HH:mm, 1
        context.write(new Text(date + ":" + roundedTime), new IntWritable(1));
    }
}
