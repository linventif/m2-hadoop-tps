package ex5;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class Ex5Mapper extends Mapper<LongWritable, Text, // Ne pas modifier !
                                     Text, Text>          // Couple (key, value) en sortie du mapper : ("x,y", id_client)
{
    // value est une commande : "id_cde;id_client;id_prod_1;...;id_prod_n".
    // Pour chaque couple de produits de la commande, on émet (couple, client).
    @Override
    public void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        String[] fields = value.toString().split(";");

        // Au moins id_cde, id_client et 2 produits, sinon pas de couple possible.
        if (fields.length < 4) {
            context.getCounter("Ex5", "commandes sans couple ou mal formees").increment(1);
            return;
        }
        String client = fields[1].trim();
        if (client.isEmpty()) {
            context.getCounter("Ex5", "commandes sans couple ou mal formees").increment(1);
            return;
        }

        // Produits distincts et triés : (P02, P01) et (P01, P02) donnent le même couple "P01,P02",
        // et un produit en double dans la commande ne forme pas de couple avec lui-même.
        TreeSet<String> set = new TreeSet<>();
        for (int i = 2; i < fields.length; i++) {
            String p = fields[i].trim();
            if (!p.isEmpty()) {
                set.add(p);
            }
        }
        List<String> products = new ArrayList<>(set);

        // Tous les couples x < y de la commande.
        Text clientText = new Text(client);
        for (int i = 0; i < products.size(); i++) {
            for (int j = i + 1; j < products.size(); j++) {
                context.write(new Text(products.get(i) + "," + products.get(j)), clientText);
            }
        }
    }
}
