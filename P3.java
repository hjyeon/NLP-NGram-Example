import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class P3 {
	
	private static char[] alphabet = " abcdefghijklmnopqrstuvwxyz".toCharArray(); // space + alphabet
	private static int lengthScript = 0;                                          // this gets updated later
	private static final int SENTENCE_LENGTH = 1000;                              // characters
	
	private static Map<String, Integer> count_unigrams = new HashMap<String,Integer>(); // count of unigrams
	private static Map<String, Integer> count_bigrams = new HashMap<String,Integer>();  // count of bigrams
	private static Map<String, Integer> count_trigrams = new HashMap<String,Integer>(); // count of trigrams
	
	private static Map<String, Double> transitionProbability1 = new HashMap<String, Double>(); // P(x)
	private static Map<String, Double> transitionProbability2 = new TreeMap<String, Double>(); // P(y|x)
	private static Map<String, Double> transitionProbability3 = new HashMap<String, Double>(); // P(z|xy)
	private static String sentences ="";
	
	public static void main(String[] args) throws IOException {
		
		// Step 1 - read a script of the movie from txt file into String object
		String script = new String(Files.readAllBytes(Paths.get("KungFuPanda.txt")));
		System.out.println("Finished reading the input file with script. ");
		
		// Step 2 - processing the script
		// make everything lowercase, remove other chars excepts letter or space with a single space
		script = script.toLowerCase().replaceAll("[^a-z ]", " ").replaceAll(" +", " ");
		System.out.println("Finished processing the script. ");
		
		// store the value of total chars in the processed script, will need this later
		lengthScript = script.length(); 
		System.out.println("Length of the script: " + lengthScript);
		
		// Step 3 - this step takes some time to run
		countNGrams(script);
		System.out.println("Counted all unigrams, bigrams and trigrams for the script.");
		
		// Step 4 - estimate transition probabilities (MLE) without Laplace smoothing 
		estimateTransitionProbabilities(1, count_unigrams, transitionProbability1); 
		System.out.println("Unigram, Without Smoothing: ");
		for (double d: transitionProbability1.values()) {
			System.out.printf("%.4f", d);
			System.out.print(", ");		
		}
		System.out.println();
		estimateTransitionProbabilities(2, count_bigrams, transitionProbability2);
		System.out.println("Bigram transition probabilities without formatting");
		System.out.println(transitionProbability2.toString());
		System.out.println("Bigram, Without Smoothing, formatted: ");
	//	System.out.println(transitionProbability2.values().toString());
		double[] printing = bigramProbabilities4DecimalFormat(false);
		for (int i = 0 ; i < 27*27 ; i++) {
			System.out.printf("%.4f",printing[i]);
			if (i%27 == 26 && i != 0) System.out.println("");
			else System.out.print(", ");		
		}
		System.out.println();
		estimateTransitionProbabilitiesWithLS(2, count_bigrams, transitionProbability2);
		System.out.println("Transition Probabilities overridden by LS");
		System.out.println("Bigram, With Smoothing, formatted: ");
		printing = bigramProbabilities4DecimalFormat(true);
		for (int i = 0 ; i < 27*27 ; i++) {
			System.out.printf("%.4f",printing[i]);
			if (i%27 == 26 && i != 0) System.out.println("");
			else System.out.print(", ");		
		}
		System.out.println();
		estimateTransitionProbabilitiesWithLS(3, count_trigrams, transitionProbability3);
		
		// Step 5 - generate sentences using trigram model, in some cases bigram model is used as well
		System.out.println("================== Generating sentences for each letter ==================");
		generateSentences();
		System.out.println("==========================================================================");
		System.out.println(sentences);
		System.out.println("-------------------------------");
		
		// Step 6 - compute likelihood prob and posterior probabilities for the script given by Young
		String scriptYoung = new String(Files.readAllBytes(Paths.get("script.txt")));
		
		// process this script as well 
		scriptYoung = scriptYoung.toLowerCase().replaceAll("[^a-z ]", " ").replaceAll(" +", " ");

		Map<String, Integer> count_unigramsScript2 = new HashMap<String,Integer>(); 
		int count = 0;
		for (int i = 0; i < alphabet.length; i++){
			count = scriptYoung.length() - scriptYoung.replace(String.valueOf(alphabet[i]), "").length();
			count_unigramsScript2.put(String.valueOf(alphabet[i]), count);
		}
		
		Map<String, Double> transitionProbabilityScript2 = new HashMap<String, Double>(); // P(x)
		double probability;
		for (String key: count_unigramsScript2.keySet()){
			probability = (count_unigramsScript2.get(key))/(double)(scriptYoung.length());
			transitionProbabilityScript2.put(key, probability);
		}
		System.out.println("Likelihood probabilities for the script of Young:");
		System.out.println(transitionProbabilityScript2.values().toString());
		
		// computing posterior probabilities for the script of Young
		Map<String, Double> posteriorProb = new HashMap<String, Double>(); 
		double post_prob;
		for (String key: count_unigramsScript2.keySet()){
			post_prob = transitionProbabilityScript2.get(key) / (transitionProbabilityScript2.get(key) + 
					transitionProbability1.get(key));
			posteriorProb.put(key, post_prob);
		}
		System.out.println("Posterior probabilities for the script of Young:");
		System.out.println(posteriorProb.values());
		
		
		Map<String, Double> posteriorProb2 = new HashMap<String, Double>(); 
		double post_prob2;
		for (String key: count_unigrams.keySet()){
			post_prob2 = transitionProbability1.get(key) / (transitionProbabilityScript2.get(key) + 
					transitionProbability1.get(key));
			posteriorProb2.put(key, post_prob2);
		}
		System.out.println("Posterior probabilities for Kung Fu Panda script:");
		System.out.println(posteriorProb2);
		
		System.out.println("Naive Bayes Model Prediction");
		System.out.println(naiveBayesPrediction(sentences, posteriorProb2,posteriorProb).toString());

		
		
		
		
		// TODO: similarly compute posterior probabilities for our script
		// TODO: need to implement Naive Bayes prediction 
		
	}
	
	public static ArrayList<Integer> naiveBayesPrediction (String sentences, Map<String, Double> posteriorProb0, Map<String, Double> posteriorProb1) {
		ArrayList<Integer> prediction = new ArrayList<Integer>();
		double prob0 = 0.0;
		double prob1 = 0.0;
		String[] sentence = sentences.trim().split(System.lineSeparator());
		for (String s : sentence) {
			int length = s.length();
			for (int i = 0 ; i < length ; i++) {
				prob0 += Math.log(posteriorProb0.get(Character.toString(s.charAt(i))));
				prob1 += Math.log(posteriorProb1.get(Character.toString(s.charAt(i))));
			}
			if (prob0 > prob1) {
				//System.out.println(prob0+","+ prob1);
				prediction.add(0);
			}
			else prediction.add(1);
		}
		
		return prediction;
	}
	
	public static void countNGrams(String script){

		// count # of occurrences of each char in an alphabet + space character
		int count = 0;
		for (int i = 0; i < alphabet.length; i++){
			count = script.length() - script.replace(String.valueOf(alphabet[i]), "").length();
			count_unigrams.put(String.valueOf(alphabet[i]), count);
		}

		// count # of occurrences for each bigram
		for (int i=0; i < alphabet.length; i++){
			for (int j=0; j < alphabet.length; j++){
				count = (script.length() - script.replace(String.valueOf(alphabet[i])+
						String.valueOf(alphabet[j]), "").length())/2;
				count_bigrams.put(String.valueOf(alphabet[i])+String.valueOf(alphabet[j]), count);
			}
		}

		// count # of occurrences for each trigram
		for (int i=0; i < alphabet.length; i++){
			for (int j=0; j < alphabet.length; j++){
				for (int k=0; k<alphabet.length; k++){
					count = (script.length() - script.replace(String.valueOf(alphabet[i])+
							String.valueOf(alphabet[j])+String.valueOf(alphabet[k]), "").length())/3;
					count_trigrams.put(String.valueOf(alphabet[i])+String.valueOf(alphabet[j])+
							String.valueOf(alphabet[k]), count);
				}
			}
		}
	}
	
	/*
	 * No Laplace smoothing
	 */
	public static void estimateTransitionProbabilities(int n, Map<String, Integer> ngram_count, Map<String, Double> trans_prob){

		double probability;
		for (String key: ngram_count.keySet()){
			if (n == 1) {
				// compute P(x)
				probability = (ngram_count.get(key))/(double)(lengthScript);
			} else if (n==2) {
				// compute P (y | x)
				if ((double) (ngram_count.get(key)) == 0) {
					probability = 0.0; 
				}
				else probability = (double) (ngram_count.get(key))/(double)(count_unigrams.get(String.valueOf(key.charAt(0))));
			//	if (key.contentEquals("q ")) System.out.println(ngram_count.get(key)); 
				//if (probability == 1.0000) {
					//System.out.println(ngram_count.get(key));
					//System.out.println(key);
					//System.out.println((count_unigrams.get(String.valueOf(key.charAt(0)))));
				//}
			} else {
				// compute P(z | xy)
				probability = (double) (ngram_count.get(key))/(double)(count_bigrams.get(key.substring(0, 2)));
			}
			trans_prob.put(key, probability);
		}
	}
	
	/*
	 * Laplace smoothing
	 */
	public static void estimateTransitionProbabilitiesWithLS(int n, Map<String, Integer> ngram_count, Map<String, Double> trans_prob){

		double probability;
		for (String key: ngram_count.keySet()){
			if (n == 1) {
				// compute P(x)
				probability = (ngram_count.get(key) + 1)/(double)((lengthScript) + 27);
			} else if (n==2) {
				// compute P (y | x)
				probability = (ngram_count.get(key) + 1 )/(double)(count_unigrams.get(String.valueOf(key.charAt(0)))+27);
			} else {
				// compute P(z | xy)
				probability = (ngram_count.get(key) + 1 )/(double)(count_bigrams.get(key.substring(0, 2))+27);
			}
			trans_prob.put(key, probability);
		}
	}
	
	public static void generateSentences() {

		// go through each letter, ignore first char which is space
		for (int i=1; i<alphabet.length; i++){ 

			StringBuilder sb = new StringBuilder();
			sb.append(String.valueOf(alphabet[i])); // append the first letter

			// keep building a sentence until length is 1000 chars
			while(sb.length() < SENTENCE_LENGTH){ 

				double[] cdf;

				if (sb.length() == 1 || transitionProbability2.get(sb.toString().substring(sb.length()-2,sb.length())) == 0.0){ 
					// for 2nd letter compute CDF using bigram or when can't use trigram model in general
					cdf = computeCDF(String.valueOf(sb.toString().charAt(0)), transitionProbability2);
				} else {
					// for the rest of the letters compute CDF using trigram model
					cdf = computeCDF(sb.toString().substring(sb.length()-2,sb.length()), transitionProbability3);
				}
				
				// generate random uniform variable between 0 and max value of the cdf
				Random r =	new Random();
				double randomValue = cdf[cdf.length-1] * r.nextDouble(); 
				
				char letter = findNextLetter(randomValue,cdf); // found next letter/space

				sb.append(letter); // append next letter to the sentence
			}
			sentences += sb.toString();
			sentences += System.lineSeparator();
			// print out the generated sentence
			System.out.println(sb.toString());
		}
	}
	
	public static double[] computeCDF(String c, Map<String,Double> trans_prob){

		double[] cdf = new double[27];
		double sum = 0;
		for (int i=0; i<27; i++){
			sum = sum + trans_prob.get(c+String.valueOf(alphabet[i]));
			cdf[i] = sum;
		}
		return cdf;
	}

	public static char findNextLetter(double u, double[] cdf){
	
		for (int i=0; i<alphabet.length; i++){
			if (u <= cdf[i]){
				return alphabet[i];
			}
		}
		// if greater than sum of all probabilities then return last char, shouldn't happen though
		return 'z'; 
	}
	
	public static double[] bigramProbabilities4DecimalFormat(boolean smoothing) {
		Object[] values = transitionProbability2.values().toArray();
		double[] decimals = new double[27*27];

		Double rounding = 0.0;
		Double conversion = 0.0;
		double sum =0.0;
		for (int i = 0 ; i < 27*27 ; i++) {
			if (i%27 == 26) {
				for (int j = i - 26 ; j < i + 1 ; j++) {
					sum += decimals[j];
				}
				if (1-sum < 0) decimals[i] = 0.0000;
				else if (smoothing && 1-sum < 0) decimals[i] = 0.0001;
				else if (smoothing && 10000.00*(1 - sum) < 10.00) decimals[i] = 0.0001;
				else decimals[i] = 1 - sum;
				sum = 0.0;
			}
			else {
				rounding = (double) Math.round(((Double)values[i] )*10000);
				if (rounding == 0.0 && smoothing) decimals[i] = 0.0001;
				else {
					conversion = (double) Math.round((Double)values[i]*10000);
					decimals[i] = conversion/10000.00; 
				}
			}

		
		}
		return decimals;
	}

}
